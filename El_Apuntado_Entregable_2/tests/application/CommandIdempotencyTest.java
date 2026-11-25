package com.apuntado.tests.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.apuntado.application.CommandConflictException;
import com.apuntado.application.GameApplicationService;
import com.apuntado.application.TurnOrchestratorComponent;
import com.apuntado.application.command.CreateGameCommand;
import com.apuntado.application.command.PlayerInput;
import com.apuntado.application.port.AuditEntry;
import com.apuntado.application.port.CommandReceipt;
import com.apuntado.application.port.IGameRepository;
import com.apuntado.application.view.GameView;
import com.apuntado.application.view.VariantView;
import com.apuntado.bootstrap.RuleRegistry;
import com.apuntado.domain.gameplay.HandEvaluatorComponent;
import com.apuntado.domain.model.Card;
import com.apuntado.domain.model.GameSession;
import com.apuntado.domain.scoring.RoundScoringComponent;
import com.apuntado.persistence.InMemoryGameRepository;
import com.apuntado.tests.support.Decks;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Transaccionalidad de comandos (ADR-04, riesgos R2 y R3): reintentos, reutilización indebida de
 * commandId, cierres concurrentes, fallo antes del commit y registro de auditoría.
 */
class CommandIdempotencyTest {
 private static final List<String> RIVAL = Decks.ids("13H", "13D", "13S", "13C", "12H", "12D", "2C", "3C", "4C", "4D");
 private final List<AuditEntry> audit = new CopyOnWriteArrayList<>();

 private GameApplicationService service(IGameRepository repository, Supplier<List<Card>> decks) {
  RuleRegistry rules = new RuleRegistry();
  TurnOrchestratorComponent orchestrator = new TurnOrchestratorComponent(new HandEvaluatorComponent(), rules,
   new RoundScoringComponent(), decks);
  return new GameApplicationService(repository, orchestrator, rules, (gameId, type, views) -> { }, audit::add);
 }

 private static CreateGameCommand game(String variantId) {
  return new CreateGameCommand(variantId, List.of(new PlayerInput("p1", 0, "Ana"), new PlayerInput("p2", 1, "Luis")));
 }

 /** Partida en la que p1 ya robó y descartó la carta robada: queda listo para cerrar. */
 private static GameView readyToClose(GameApplicationService service, String variantId) {
  GameView created = service.createGame(game(variantId));
  GameView drawn = service.drawFromDeck(created.gameId(), "p1", "draw-1");
  String drawnCard = drawn.hand().get(drawn.hand().size() - 1).id();
  return service.discard(created.gameId(), "p1", drawnCard, "discard-1");
 }

 private long count(String action, String outcome) {
  return audit.stream().filter(entry -> entry.action().equals(action) && entry.outcome().equals(outcome)).count();
 }

 @Test
 @DisplayName("Reintentar un cierre con el mismo commandId no vuelve a sumar penalizaciones")
 void retryingACloseDoesNotDuplicateScores() {
  InMemoryGameRepository repository = new InMemoryGameRepository();
  GameApplicationService service = service(repository, Decks.sequence(Decks.dealing(Decks.CLOSING_HAND, RIVAL)));
  GameView ready = readyToClose(service, "APUNTADO101");

  GameView first = service.closeRound(ready.gameId(), "p1", "close-1");
  GameView retry = service.closeRound(ready.gameId(), "p1", "close-1");

  assertEquals(first.version(), retry.version());
  GameSession stored = repository.findById(ready.gameId()).orElseThrow();
  assertEquals(1, stored.completedRounds().size());
  assertEquals(73, stored.scoreOf("p2").total());
  assertEquals(1, count("CLOSE", "OK"));
  assertEquals(1, count("CLOSE", "REPLAY"));
 }

 @Test
 @DisplayName("Un commandId reutilizado con otra acción o autor se rechaza (409)")
 void reusingACommandIdForAnotherCommandIsRejected() {
  GameApplicationService service = service(new InMemoryGameRepository(), Decks.sequence());
  GameView created = service.createGame(game("CLASICA"));
  service.drawFromDeck(created.gameId(), "p1", "cmd-1");

  assertThrows(CommandConflictException.class, () -> service.endTurn(created.gameId(), "p1", "cmd-1"));
  assertThrows(CommandConflictException.class, () -> service.drawFromDeck(created.gameId(), "p2", "cmd-1"));
  assertEquals(2, count("END_TURN", "REJECTED") + count("DRAW_DECK", "REJECTED"));
 }

 @Test
 @DisplayName("Dos cierres simultáneos desde la misma versión: exactamente uno se confirma")
 void concurrentClosesLetExactlyOneWin() throws Exception {
  InMemoryGameRepository repository = new InMemoryGameRepository();
  GameApplicationService service = service(repository, Decks.sequence(Decks.dealing(Decks.CLOSING_HAND, RIVAL)));
  GameView ready = readyToClose(service, "APUNTADO101");
  CountDownLatch start = new CountDownLatch(1);
  ExecutorService pool = Executors.newFixedThreadPool(2);
  try {
   List<Future<Boolean>> attempts = List.of("close-a", "close-b").stream().map(commandId -> pool.submit(() -> {
    start.await();
    try {
     service.closeRound(ready.gameId(), "p1", commandId);
     return true;
    } catch (RuntimeException rejected) {
     return false;
    }
   })).toList();
   start.countDown();
   long accepted = 0;
   for (Future<Boolean> attempt : attempts) if (attempt.get(10, TimeUnit.SECONDS)) accepted++;
   assertEquals(1, accepted);
  } finally {
   pool.shutdownNow();
  }
  GameSession stored = repository.findById(ready.gameId()).orElseThrow();
  assertEquals(1, stored.completedRounds().size(), "no hay liquidación doble");
  assertEquals(73, stored.scoreOf("p2").total());
 }

 @Test
 @DisplayName("Un fallo antes del commit no deja cambios parciales ni recibo")
 void failureBeforeCommitLeavesNoPartialState() {
  InMemoryGameRepository delegate = new InMemoryGameRepository();
  IGameRepository failing = new IGameRepository() {
   public Optional<GameSession> findById(String gameId) { return delegate.findById(gameId); }
   public void create(GameSession session) { delegate.create(session); }
   public void save(GameSession previous, GameSession updated, CommandReceipt receipt) {
    throw new IllegalStateException("caída simulada del almacenamiento");
   }
   public Optional<CommandReceipt> receipt(String gameId, String commandId) { return delegate.receipt(gameId, commandId); }
  };
  GameApplicationService service = service(failing, Decks.sequence());
  GameView created = service.createGame(game("APUNTADO101"));

  assertThrows(IllegalStateException.class, () -> service.drawFromDeck(created.gameId(), "p1", "cmd-1"));

  GameSession stored = delegate.findById(created.gameId()).orElseThrow();
  assertEquals(0, stored.version());
  assertEquals(10, stored.round().handOf("p1").cards().size());
  assertTrue(delegate.receipt(created.gameId(), "cmd-1").isEmpty());
  assertEquals(1, count("DRAW_DECK", "REJECTED"));
 }

 @Test
 @DisplayName("Un cierre inválido explica la regla y queda auditado como error de regla")
 void invalidCloseIsExplainedAndAudited() {
  List<String> noGroups = Decks.ids("1H", "3D", "5S", "7C", "9H", "11D", "6S", "8D", "10C", "12S");
  GameApplicationService service = service(new InMemoryGameRepository(),
   Decks.sequence(Decks.dealing(noGroups, RIVAL)));
  GameView ready = readyToClose(service, "CLASICA");

  IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
   () -> service.closeRound(ready.gameId(), "p1", "close-1"));

  assertTrue(error.getMessage().contains("4-3-3"), error.getMessage());
  AuditEntry rejected = audit.stream().filter(entry -> entry.action().equals("CLOSE")).findFirst().orElseThrow();
  assertEquals("REJECTED", rejected.outcome());
  assertEquals("CLASICA", rejected.variantId());
  assertTrue(rejected.detail().contains("Cierre inválido"));
  assertTrue(rejected.durationMicros() >= 0);
 }

 @Test
 @DisplayName("Partidas completas por la capa de aplicación: Clásica en una ronda, Apuntado hasta 101")
 void playsCompleteGamesThroughTheApplicationLayer() {
  GameApplicationService classic = service(new InMemoryGameRepository(),
   Decks.sequence(Decks.dealing(Decks.CLOSING_HAND, RIVAL)));
  GameView classicReady = readyToClose(classic, "CLASICA");
  GameView classicEnd = classic.closeRound(classicReady.gameId(), "p1", "close-1");
  assertEquals("FINISHED", classicEnd.status());
  assertEquals("p1", classicEnd.winnerId());
  assertEquals("p1", classicEnd.lastClosedBy());

  List<String> rival28 = Decks.ids("2H", "2D", "2S", "2C", "3H", "3D", "3S", "3C", "4H", "4D");
  GameApplicationService apuntado = service(new InMemoryGameRepository(), Decks.sequence(
   Decks.dealing(Decks.CLOSING_HAND, RIVAL), Decks.dealing(Decks.CLOSING_HAND, rival28)));
  GameView ready = readyToClose(apuntado, "APUNTADO101");
  GameView round2 = apuntado.closeRound(ready.gameId(), "p1", "close-1");
  assertEquals("ACTIVE", round2.status());
  assertEquals(2, round2.currentRound());
  assertEquals("p2", round2.turn().playerId());
  String id = ready.gameId();
  GameView p2Drawn = apuntado.drawFromDeck(id, "p2", "r2-draw-p2");
  apuntado.discard(id, "p2", p2Drawn.hand().get(10).id(), "r2-discard-p2");
  apuntado.endTurn(id, "p2", "r2-end-p2");
  GameView p1Drawn = apuntado.drawFromDeck(id, "p1", "r2-draw-p1");
  apuntado.discard(id, "p1", p1Drawn.hand().get(10).id(), "r2-discard-p1");
  GameView end = apuntado.closeRound(id, "p1", "r2-close");

  assertEquals("FINISHED", end.status());
  assertEquals("p1", end.winnerId());
  assertEquals(101, end.players().stream().filter(player -> player.playerId().equals("p2")).findFirst()
   .orElseThrow().total());
  assertTrue(end.players().stream().filter(player -> player.playerId().equals("p2")).findFirst().orElseThrow().eliminated());
 }

 @Test
 @DisplayName("Las modalidades exponen su condición de cierre y si son de una sola ronda")
 void variantsDescribeTheirClosingRule() {
  List<VariantView> variants = service(new InMemoryGameRepository(), Decks.sequence()).variants();
  VariantView classic = variants.stream().filter(variant -> variant.id().equals("CLASICA")).findFirst().orElseThrow();
  VariantView apuntado = variants.stream().filter(variant -> variant.id().equals("APUNTADO101")).findFirst().orElseThrow();
  assertTrue(classic.singleRound());
  assertFalse(apuntado.singleRound());
  assertTrue(apuntado.closeRequirement().contains("menos de 5"));
 }
}
