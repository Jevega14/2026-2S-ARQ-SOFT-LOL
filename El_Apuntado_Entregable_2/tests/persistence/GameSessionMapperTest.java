package com.apuntado.tests.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.apuntado.domain.gameplay.RoundClosingService;
import com.apuntado.domain.model.Card;
import com.apuntado.domain.model.GameSession;
import com.apuntado.domain.model.GameStatus;
import com.apuntado.domain.model.PlayerScore;
import com.apuntado.domain.model.PlayerSeat;
import com.apuntado.domain.model.StandardDeck;
import com.apuntado.domain.rules.clasica.ReglaClasica;
import com.apuntado.domain.scoring.RoundScoringComponent;
import com.apuntado.persistence.mongo.GameSessionMapper;
import java.util.ArrayList;
import java.util.List;
import org.bson.Document;
import org.junit.jupiter.api.Test;

class GameSessionMapperTest {
 private List<PlayerSeat> players() {
  return List.of(new PlayerSeat("p1", 0, "Ana"), new PlayerSeat("p2", 1, "Luis"));
 }

 @Test
 void roundTripsAFreshSession() {
  GameSession session = GameSession.start("g", "APUNTADO101", players());
  GameSession restored = GameSessionMapper.toDomain(GameSessionMapper.toDocument(session));
  assertEquals(session, restored);
 }

 @Test
 void roundTripsAnInProgressSession() {
  GameSession session = GameSession.start("g", "CLASICA", players());
  String drawn = session.round().deck().get(0).id();
  GameSession advanced = session.drawFromDeck("p1").discard("p1", drawn);
  Document document = GameSessionMapper.toDocument(advanced);
  GameSession restored = GameSessionMapper.toDomain(document);
  assertEquals(advanced, restored);
  assertEquals(2, restored.version());
  assertEquals("DISCARD", restored.turn().phase().name());
 }

 @Test
 void roundTripsCompletedRoundsTotalsAndWinner() {
  GameSession finished = finishedSession();
  GameSession restored = GameSessionMapper.toDomain(GameSessionMapper.toDocument(finished));
  assertEquals(finished, restored);
  assertEquals(GameStatus.FINISHED, restored.status());
  assertEquals("p1", restored.winnerId());
  assertEquals(1, restored.completedRounds().size());
  assertTrue(restored.scoreOf("p2").eliminated());
 }

 private GameSession finishedSession() {
  List<String> groupIds = List.of("7H", "7D", "7S", "7C", "9H", "9D", "9S", "11H", "11D", "11S");
  List<Card> deck = new ArrayList<>(StandardDeck.shuffled());
  List<Card> winningHand = deck.stream().filter(card -> groupIds.contains(card.id())).toList();
  deck.removeAll(winningHand);
  deck.addAll(0, winningHand);
  GameSession session = GameSession.start("g-finished", "CLASICA", players(), deck);
  String drawn = session.round().deck().get(0).id();
  GameSession ready = session.drawFromDeck("p1").discard("p1", drawn);
  GameSession nearLoss = new GameSession(ready.id(), ready.variantId(), ready.players(), ready.currentRound(),
   ready.round(), ready.turn(), ready.status(), ready.version(),
   List.of(new PlayerScore("p1", 0), new PlayerScore("p2", 100)), ready.completedRounds(), null);
  return new RoundClosingService(new RoundScoringComponent()).attemptClose(nearLoss, "p1", new ReglaClasica());
 }
}
