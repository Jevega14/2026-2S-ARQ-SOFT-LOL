package com.apuntado.tests.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.apuntado.application.DuplicateGameException;
import com.apuntado.application.OptimisticLockException;
import com.apuntado.domain.model.GameSession;
import com.apuntado.domain.model.PlayerSeat;
import com.apuntado.persistence.InMemoryGameRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

class InMemoryGameRepositoryTest {
 private List<PlayerSeat> players() {
  return List.of(new PlayerSeat("p1", 0, "Ana"), new PlayerSeat("p2", 1, "Luis"));
 }

 @Test
 void rejectsDuplicateCreate() {
  InMemoryGameRepository repository = new InMemoryGameRepository();
  GameSession session = GameSession.start("g", "CLASICA", players());
  repository.create(session);
  assertThrows(DuplicateGameException.class, () -> repository.create(session));
 }

 @Test
 void rejectsStaleWritesAndKeepsTheReceipt() {
  InMemoryGameRepository repository = new InMemoryGameRepository();
  GameSession previous = GameSession.start("g", "CLASICA", players());
  repository.create(previous);
  GameSession updated = previous.drawFromDeck("p1");
  repository.save(previous, updated, "cmd-1");
  assertThrows(OptimisticLockException.class, () -> repository.save(previous, updated, "cmd-2"));
  assertTrue(repository.receipt("g", "cmd-1").isPresent());
  assertEquals(updated.version(), repository.receipt("g", "cmd-1").orElseThrow().version());
 }

 @Test
 void storesTheLatestProjectionAfterEachCommand() {
  InMemoryGameRepository repository = new InMemoryGameRepository();
  GameSession previous = GameSession.start("g", "CLASICA", players());
  repository.create(previous);
  GameSession drawn = previous.drawFromDeck("p1");
  repository.save(previous, drawn, "cmd-1");
  String cardId = drawn.round().handOf("p1").cards().get(0).id();
  GameSession discarded = drawn.discard("p1", cardId);
  repository.save(drawn, discarded, "cmd-2");
  assertEquals(discarded.version(), repository.findById("g").orElseThrow().version());
 }
}
