package com.apuntado.tests.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.apuntado.application.CommandConflictException;
import com.apuntado.application.GameApplicationService;
import com.apuntado.application.TurnOrchestratorComponent;
import com.apuntado.application.command.CreateGameCommand;
import com.apuntado.application.command.PlayerInput;
import com.apuntado.application.view.GameView;
import com.apuntado.bootstrap.RuleRegistry;
import com.apuntado.domain.gameplay.HandEvaluatorComponent;
import com.apuntado.domain.model.Card;
import com.apuntado.domain.model.GameSession;
import com.apuntado.domain.scoring.RoundScoringComponent;
import com.apuntado.persistence.mongo.MongoGameRepository;
import com.apuntado.tests.support.Decks;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

/**
 * Flujo completo contra MongoDB real (prioridad alta de la retroalimentación del Entregable 2):
 * cierres simultáneos, reintentos con el mismo commandId, recuperación tras reinicio y partida a 101.
 */
@EnabledIfEnvironmentVariable(named = "APUNTADO_MONGO_IT", matches = "true")
class GameFlowMongoIT {
 private static final String DATABASE = "apuntado_flow_it";
 private static final List<String> RIVAL_73 = Decks.ids("13H", "13D", "13S", "13C", "12H", "12D", "2C", "3C", "4C", "4D");
 private static final List<String> RIVAL_28 = Decks.ids("2H", "2D", "2S", "2C", "3H", "3D", "3S", "3C", "4H", "4D");
 private final String uri = System.getenv().getOrDefault("APUNTADO_MONGO_URI", "mongodb://localhost:27017");
 private MongoClient client;

 @BeforeEach
 void setUp() {
  client = MongoClients.create(uri);
  client.getDatabase(DATABASE).drop();
 }

 @AfterEach
 void tearDown() {
  client.getDatabase(DATABASE).drop();
  client.close();
 }

 /** Cada llamada simula una instancia nueva del servidor: cliente, repositorio y servicio propios. */
 private GameApplicationService newServerInstance(MongoClient mongo, Supplier<List<Card>> decks) {
  RuleRegistry rules = new RuleRegistry();
  return new GameApplicationService(new MongoGameRepository(new MongoTemplate(mongo, DATABASE)),
   new TurnOrchestratorComponent(new HandEvaluatorComponent(), rules, new RoundScoringComponent(), decks),
   rules, (gameId, type, views) -> { });
 }

 private static GameView readyToClose(GameApplicationService service) {
  GameView created = service.createGame(new CreateGameCommand("APUNTADO101",
   List.of(new PlayerInput("p1", 0, "Ana"), new PlayerInput("p2", 1, "Luis"))));
  GameView drawn = service.drawFromDeck(created.gameId(), "p1", "draw-1");
  return service.discard(created.gameId(), "p1", drawn.hand().get(10).id(), "discard-1");
 }

 private GameSession stored(String gameId) {
  return new MongoGameRepository(new MongoTemplate(client, DATABASE)).findById(gameId).orElseThrow();
 }

 @Test
 @DisplayName("Dos servidores cierran la misma ronda a la vez: un cierre, una liquidación")
 void concurrentClosesFromTwoServerInstances() throws Exception {
  GameApplicationService setup = newServerInstance(client, Decks.sequence(Decks.dealing(Decks.CLOSING_HAND, RIVAL_73)));
  GameView ready = readyToClose(setup);
  try (MongoClient secondClient = MongoClients.create(uri)) {
   List<GameApplicationService> servers = List.of(newServerInstance(client, Decks.sequence()),
    newServerInstance(secondClient, Decks.sequence()));
   CountDownLatch start = new CountDownLatch(1);
   ExecutorService pool = Executors.newFixedThreadPool(2);
   try {
    List<Future<Boolean>> attempts = List.of(0, 1).stream().map(index -> pool.submit(() -> {
     start.await();
     try {
      servers.get(index).closeRound(ready.gameId(), "p1", "close-" + index);
      return true;
     } catch (RuntimeException rejected) {
      return false;
     }
    })).toList();
    start.countDown();
    long accepted = 0;
    for (Future<Boolean> attempt : attempts) if (attempt.get(20, TimeUnit.SECONDS)) accepted++;
    assertEquals(1, accepted);
   } finally {
    pool.shutdownNow();
   }
  }
  GameSession game = stored(ready.gameId());
  assertEquals(1, game.completedRounds().size());
  assertEquals(73, game.scoreOf("p2").total());
  assertEquals(ready.version() + 1, game.version());
 }

 @Test
 @DisplayName("Reintento con el mismo commandId: devuelve el resultado y no duplica el recibo ni los puntos")
 void retriesAreIdempotentAndReusedIdsAreRejected() {
  GameApplicationService service = newServerInstance(client, Decks.sequence(Decks.dealing(Decks.CLOSING_HAND, RIVAL_73)));
  GameView ready = readyToClose(service);
  GameView first = service.closeRound(ready.gameId(), "p1", "close-1");
  GameView retry = service.closeRound(ready.gameId(), "p1", "close-1");

  assertEquals(first.version(), retry.version());
  assertEquals(73, stored(ready.gameId()).scoreOf("p2").total());
  Document raw = new MongoTemplate(client, DATABASE).findOne(
   Query.query(Criteria.where("_id").is(ready.gameId())), Document.class, "game_sessions");
  long receipts = raw.getList("receipts", Document.class).stream()
   .filter(receipt -> "close-1".equals(receipt.getString("commandId"))).count();
  assertEquals(1, receipts);
  assertThrows(CommandConflictException.class, () -> service.endTurn(ready.gameId(), "p2", "close-1"));
 }

 @Test
 @DisplayName("Recuperación: tras reiniciar el servidor la partida continúa desde el último commit")
 void recoversAfterARestartAndFinishesTheGameAt101() {
  Supplier<List<Card>> decks = Decks.sequence(Decks.dealing(Decks.CLOSING_HAND, RIVAL_73),
   Decks.dealing(Decks.CLOSING_HAND, RIVAL_28));
  GameApplicationService before = newServerInstance(client, decks);
  GameView ready = readyToClose(before);
  GameView afterRound1 = before.closeRound(ready.gameId(), "p1", "close-1");
  GameView p2Drawn = before.drawFromDeck(ready.gameId(), "p2", "r2-draw-p2");

  GameView recovered;
  try (MongoClient restarted = MongoClients.create(uri)) {
   GameApplicationService after = newServerInstance(restarted, decks);
   recovered = after.getGame(ready.gameId(), "p2");
   assertEquals(p2Drawn.version(), recovered.version());
   assertEquals(p2Drawn.hand(), recovered.hand());
   assertEquals("DISCARD", recovered.turn().phase());

   String id = ready.gameId();
   after.discard(id, "p2", recovered.hand().get(10).id(), "r2-discard-p2");
   after.endTurn(id, "p2", "r2-end-p2");
   GameView p1Drawn = after.drawFromDeck(id, "p1", "r2-draw-p1");
   after.discard(id, "p1", p1Drawn.hand().get(10).id(), "r2-discard-p1");
   GameView end = after.closeRound(id, "p1", "r2-close");
   assertEquals("FINISHED", end.status());
   assertEquals("p1", end.winnerId());
  }
  GameSession game = stored(ready.gameId());
  assertEquals(2, afterRound1.currentRound());
  assertEquals(101, game.scoreOf("p2").total());
  assertTrue(game.scoreOf("p2").eliminated());
  assertEquals(2, game.completedRounds().size());
 }
}
