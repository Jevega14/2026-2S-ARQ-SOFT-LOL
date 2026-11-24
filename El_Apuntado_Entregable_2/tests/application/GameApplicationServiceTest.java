package com.apuntado.tests.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.apuntado.application.GameApplicationService;
import com.apuntado.application.GameNotFoundException;
import com.apuntado.application.TurnOrchestratorComponent;
import com.apuntado.application.command.CreateGameCommand;
import com.apuntado.application.command.PlayerInput;
import com.apuntado.application.port.IRealtimePublisher;
import com.apuntado.application.view.GameView;
import com.apuntado.bootstrap.RuleRegistry;
import com.apuntado.domain.gameplay.HandEvaluatorComponent;
import com.apuntado.domain.scoring.RoundScoringComponent;
import com.apuntado.persistence.InMemoryGameRepository;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GameApplicationServiceTest {
 private InMemoryGameRepository repository;
 private RecordingPublisher publisher;
 private GameApplicationService service;

 @BeforeEach
 void setUp() {
  RuleRegistry rules = new RuleRegistry();
  TurnOrchestratorComponent orchestrator = new TurnOrchestratorComponent(
   new HandEvaluatorComponent(), rules, new RoundScoringComponent());
  repository = new InMemoryGameRepository();
  publisher = new RecordingPublisher();
  service = new GameApplicationService(repository, orchestrator, rules, publisher);
 }

 private CreateGameCommand command(String variantId) {
  return new CreateGameCommand(variantId, List.of(
   new PlayerInput("p1", 0, "Ana"), new PlayerInput("p2", 1, "Luis")));
 }

 @Test
 void createsAnActiveGameWithTenCardsAndNoRivalHandExposed() {
  GameView view = service.createGame(command("CLASICA"));
  assertEquals("ACTIVE", view.status());
  assertEquals(1, view.currentRound());
  assertEquals("p1", view.viewerId());
  assertEquals(10, view.hand().size());
  assertEquals(2, view.players().size());
  assertTrue(view.players().stream().allMatch(player -> player.cardCount() == 10));
  assertEquals("p1", view.turn().playerId());
  assertEquals("DRAW", view.turn().phase());
 }

 @Test
 void listsBothRegisteredVariants() {
  List<String> ids = service.variants().stream().map(variant -> variant.id()).toList();
  assertEquals(List.of("APUNTADO101", "CLASICA"), ids);
 }

 @Test
 void drawingFromDeckAdvancesPhaseAndRepeatsIdempotently() {
  GameView created = service.createGame(command("CLASICA"));
  GameView afterDraw = service.drawFromDeck(created.gameId(), "p1", "cmd-1");
  assertEquals(11, afterDraw.hand().size());
  assertEquals("DISCARD", afterDraw.turn().phase());
  assertEquals(1, afterDraw.version());
  GameView repeated = service.drawFromDeck(created.gameId(), "p1", "cmd-1");
  assertEquals(afterDraw.version(), repeated.version());
  assertEquals(afterDraw.hand(), repeated.hand());
 }

 @Test
 void rejectsMovementsOutsideTheTurn() {
  GameView created = service.createGame(command("CLASICA"));
  assertThrows(IllegalStateException.class, () -> service.drawFromDeck(created.gameId(), "p2", "cmd-x"));
 }

 @Test
 void theViewerOnlyReceivesItsOwnHand() {
  GameView created = service.createGame(command("APUNTADO101"));
  GameView asLuis = service.getGame(created.gameId(), "p2");
  assertEquals("p2", asLuis.viewerId());
  assertEquals(10, asLuis.hand().size());
  assertNotEquals(created.hand(), asLuis.hand());
 }

 @Test
 void rejectsUnknownGameAndVariant() {
  assertThrows(GameNotFoundException.class, () -> service.getGame("missing", "p1"));
  assertThrows(IllegalArgumentException.class, () -> service.createGame(command("DESCONOCIDA")));
 }

 @Test
 void rejectsNonConsecutiveSeats() {
  CreateGameCommand invalid = new CreateGameCommand("CLASICA", List.of(
   new PlayerInput("p1", 0, "Ana"), new PlayerInput("p2", 2, "Luis")));
  assertThrows(IllegalArgumentException.class, () -> service.createGame(invalid));
 }

 @Test
 void rejectsAnInvalidClose() {
  GameView created = service.createGame(command("CLASICA"));
  GameView afterDraw = service.drawFromDeck(created.gameId(), "p1", "cmd-1");
  assertThrows(IllegalArgumentException.class,
   () -> service.closeRound(created.gameId(), "p1", "cmd-2"));
  assertEquals(11, afterDraw.hand().size());
 }

 @Test
 void publishesAfterCommitWithOneAuthorizedViewPerPlayer() {
  GameView created = service.createGame(command("CLASICA"));
  publisher.clear();
  service.drawFromDeck(created.gameId(), "p1", "cmd-1");
  assertEquals(1, publisher.calls.size());
  RecordingPublisher.Call call = publisher.calls.get(0);
  assertEquals("CARD_DRAWN", call.eventType());
  assertEquals(2, call.views().size());
  GameView ana = viewOf(call, "p1");
  GameView luis = viewOf(call, "p2");
  assertEquals(11, ana.hand().size());
  assertEquals(10, luis.hand().size());
  assertNotEquals(ana.hand(), luis.hand());
 }

 @Test
 void publisherFailureDoesNotRevertTheCommittedCommand() {
  GameView created = service.createGame(command("CLASICA"));
  publisher.fail = true;
  GameView afterDraw = service.drawFromDeck(created.gameId(), "p1", "cmd-1");
  assertEquals(1, afterDraw.version());
  assertEquals(1, repository.findById(created.gameId()).orElseThrow().version());
 }

 @Test
 void repeatedCommandDoesNotPublishAgain() {
  GameView created = service.createGame(command("CLASICA"));
  publisher.clear();
  service.drawFromDeck(created.gameId(), "p1", "cmd-1");
  int calls = publisher.calls.size();
  service.drawFromDeck(created.gameId(), "p1", "cmd-1");
  assertEquals(calls, publisher.calls.size());
 }

 private static GameView viewOf(RecordingPublisher.Call call, String viewerId) {
  return call.views().stream().filter(view -> view.viewerId().equals(viewerId)).findFirst().orElseThrow();
 }

 private static final class RecordingPublisher implements IRealtimePublisher {
  private final List<Call> calls = new ArrayList<>();
  private boolean fail;

  @Override
  public void publish(String gameId, String eventType, List<GameView> views) {
   if (fail) throw new IllegalStateException("fallo simulado del publicador");
   calls.add(new Call(gameId, eventType, views));
  }

  void clear() {
   calls.clear();
  }

  private record Call(String gameId, String eventType, List<GameView> views) {
  }
 }
}
