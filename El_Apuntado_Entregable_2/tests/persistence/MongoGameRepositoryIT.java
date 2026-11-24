package com.apuntado.tests.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.apuntado.application.OptimisticLockException;
import com.apuntado.domain.model.GameSession;
import com.apuntado.domain.model.PlayerSeat;
import com.apuntado.persistence.mongo.MongoGameRepository;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * Pruebas de integración contra MongoDB real (ADR-04).
 * No se ejecutan por defecto; requieren {@code APUNTADO_MONGO_IT=true}
 * y una instancia disponible en {@code APUNTADO_MONGO_URI}.
 */
@EnabledIfEnvironmentVariable(named = "APUNTADO_MONGO_IT", matches = "true")
class MongoGameRepositoryIT {
 private static final String COLLECTION = "game_sessions";
 private final String uri = System.getenv().getOrDefault("APUNTADO_MONGO_URI", "mongodb://localhost:27017");
 private MongoClient client;
 private MongoTemplate template;
 private MongoGameRepository repository;

 @BeforeEach
 void setUp() {
  client = MongoClients.create(uri);
  template = new MongoTemplate(client, "apuntado_it");
  template.dropCollection(COLLECTION);
  repository = new MongoGameRepository(template);
 }

 @AfterEach
 void tearDown() {
  template.dropCollection(COLLECTION);
  client.close();
 }

 private List<PlayerSeat> players() {
  return List.of(new PlayerSeat("p1", 0, "Ana"), new PlayerSeat("p2", 1, "Luis"));
 }

 @Test
 void persistsAndRecoversTheAggregate() {
  GameSession session = GameSession.start("g", "CLASICA", players());
  repository.create(session);
  MongoGameRepository otherInstance = new MongoGameRepository(new MongoTemplate(client, "apuntado_it"));
  assertEquals(session, otherInstance.findById("g").orElseThrow());
 }

 @Test
 void recordsReceiptsAndRejectsStaleVersions() {
  GameSession previous = GameSession.start("g", "CLASICA", players());
  repository.create(previous);
  GameSession updated = previous.drawFromDeck("p1");
  repository.save(previous, updated, "cmd-1");
  assertTrue(repository.receipt("g", "cmd-1").isPresent());
  assertThrows(OptimisticLockException.class, () -> repository.save(previous, updated, "cmd-2"));
 }

 @Test
 void concurrentSavesFromTheSameVersionLetExactlyOneWin() throws InterruptedException {
  GameSession previous = GameSession.start("g", "CLASICA", players());
  repository.create(previous);
  GameSession updated = previous.drawFromDeck("p1");
  CountDownLatch start = new CountDownLatch(1);
  AtomicInteger accepted = new AtomicInteger();
  AtomicInteger conflicts = new AtomicInteger();
  Runnable attempt = () -> {
   try {
    start.await();
    repository.save(previous, updated, "cmd-" + Thread.currentThread().getId());
    accepted.incrementAndGet();
   } catch (OptimisticLockException exception) {
    conflicts.incrementAndGet();
   } catch (InterruptedException exception) {
    Thread.currentThread().interrupt();
   }
  };
  Thread first = new Thread(attempt);
  Thread second = new Thread(attempt);
  first.start();
  second.start();
  start.countDown();
  first.join();
  second.join();
  assertEquals(1, accepted.get());
  assertEquals(1, conflicts.get());
 }
}
