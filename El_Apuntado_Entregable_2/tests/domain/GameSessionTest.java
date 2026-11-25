package com.apuntado.tests;

import com.apuntado.domain.model.GameSession;
import com.apuntado.domain.model.GameStatus;
import com.apuntado.domain.model.PlayerSeat;
import com.apuntado.domain.model.PlayerScore;
import com.apuntado.domain.model.StandardDeck;
import com.apuntado.domain.model.TurnPhase;
import com.apuntado.domain.gameplay.RoundClosingService;
import com.apuntado.domain.rules.apuntado.ReglaApuntado;
import com.apuntado.domain.rules.clasica.ReglaClasica;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GameSessionTest {
 private final RoundClosingService closing = new RoundClosingService(new com.apuntado.domain.scoring.RoundScoringComponent());
 private List<PlayerSeat> players() {
  return List.of(new PlayerSeat("p1", 0, "Ana"), new PlayerSeat("p2", 1, "Luis"));
 }
 private GameSession readyForClassicClose() {
  List<String> groupIds = com.apuntado.tests.support.Decks.CLOSING_HAND;
  List<com.apuntado.domain.model.Card> deck = new ArrayList<>(StandardDeck.shuffled());
  List<com.apuntado.domain.model.Card> winningHand = deck.stream().filter(card -> groupIds.contains(card.id())).toList();
  deck.removeAll(winningHand);
  deck.addAll(0, winningHand);
  GameSession session = GameSession.start("game-1", "CLASICA", players(), deck);
  String drawnCardId = session.round().deck().get(0).id();
  return session.drawFromDeck("p1").discard("p1", drawnCardId);
 }
 @Test void startsTheFirstRoundWithTheFirstPlayerDrawing() {
  GameSession session = GameSession.start("game-1", "CLASICA", players());
  assertEquals(GameStatus.ACTIVE, session.status());
  assertEquals(1, session.currentRound());
  assertEquals(10, session.round().handOf("p1").cards().size());
  assertEquals(10, session.round().handOf("p2").cards().size());
  assertEquals(31, session.round().deck().size());
  assertEquals(1, session.round().discard().size());
  assertEquals("p1", session.turn().playerId());
  assertEquals(1, session.turn().number());
  assertEquals(TurnPhase.DRAW, session.turn().phase());
 }
 @Test void drawsDiscardsAndPassesTheTurn() {
  GameSession session = GameSession.start("game-1", "CLASICA", players());
  GameSession afterDraw = session.drawFromDeck("p1");
  assertEquals(11, afterDraw.round().handOf("p1").cards().size());
  assertEquals(30, afterDraw.round().deck().size());
  assertEquals(TurnPhase.DISCARD, afterDraw.turn().phase());
  String cardId = afterDraw.round().handOf("p1").cards().get(0).id();
  GameSession afterDiscard = afterDraw.discard("p1", cardId);
  assertEquals(10, afterDiscard.round().handOf("p1").cards().size());
  assertEquals(2, afterDiscard.round().discard().size());
  GameSession nextTurn = afterDiscard.endTurn("p1");
  assertEquals("p2", nextTurn.turn().playerId());
  assertEquals(2, nextTurn.turn().number());
  assertEquals(TurnPhase.DRAW, nextTurn.turn().phase());
 }
 @Test void rejectsMovementsOutsideTheCurrentTurn() {
  GameSession session = GameSession.start("game-1", "CLASICA", players());
  assertThrows(IllegalStateException.class, () -> session.drawFromDeck("p2"));
  assertThrows(IllegalStateException.class, () -> session.discard("p1", "1H"));
 }
 @Test void rejectsSessionsWithLessThanTwoPlayers() {
  List<PlayerSeat> player = List.of(new PlayerSeat("p1", 0, "Ana"));
  assertThrows(IllegalArgumentException.class, () -> GameSession.start("game-1", "CLASICA", player));
 }
 @Test void canDrawTheOnlyCardFromTheDiscardPile() {
  GameSession session = GameSession.start("game-1", "CLASICA", players());
  GameSession afterDraw = session.drawFromDiscard("p1");
  assertEquals(11, afterDraw.round().handOf("p1").cards().size());
  assertTrue(afterDraw.round().discard().isEmpty());
 }
 @Test void drawingFromAnEmptyDiscardPileIsAConflictNotACrash() {
  com.apuntado.domain.model.RoundState empty = GameSession.start("game-1", "CLASICA", players())
   .drawFromDiscard("p1").round();
  assertThrows(IllegalStateException.class, () -> empty.drawFromDiscard("p1"));
 }
 @Test void aValidClassicCloseWinsTheGameInASingleRound() {
  GameSession closed = closing.attemptClose(readyForClassicClose(), "p1", new ReglaClasica());
  assertEquals(GameStatus.FINISHED, closed.status());
  assertEquals("p1", closed.winnerId());
  assertEquals(1, closed.currentRound());
  assertEquals(1, closed.completedRounds().size());
  assertEquals(0, closed.scoreOf("p1").total());
  assertFalse(closed.scoreOf("p2").eliminated());
  assertEquals(TurnPhase.CLOSED, closed.turn().phase());
 }
 @Test void aValidApuntadoCloseStartsTheNextRoundWithTheNextPlayer() {
  GameSession closed = closing.attemptClose(asVariant(readyForClassicClose(), "APUNTADO101"), "p1", new ReglaApuntado());
  assertEquals(GameStatus.ACTIVE, closed.status());
  assertEquals(2, closed.currentRound());
  assertEquals(1, closed.completedRounds().size());
  assertEquals(0, closed.scoreOf("p1").total());
  assertEquals("p2", closed.turn().playerId());
 }
 private static GameSession asVariant(GameSession session, String variantId) {
  return new GameSession(session.id(), variantId, session.players(), session.currentRound(), session.round(),
   session.turn(), session.status(), session.version(), session.totals(), session.completedRounds(), null);
 }
 @Test void rejectsAnInvalidCloseOrARuleForAnotherVariant() {
  GameSession invalid = GameSession.start("game-1", "CLASICA", players()).drawFromDeck("p1");
  String drawnCardId = invalid.round().handOf("p1").cards().get(10).id();
  invalid = invalid.discard("p1", drawnCardId);
  GameSession invalidSession = invalid;
  GameSession valid = readyForClassicClose();
  assertThrows(IllegalArgumentException.class, () -> closing.attemptClose(invalidSession, "p1", new ReglaClasica()));
  assertThrows(IllegalArgumentException.class, () -> closing.attemptClose(valid, "p1", new ReglaApuntado()));
 }
 @Test void finishesWhenTheOnlyRivalReachesTheLimit() {
  GameSession ready = readyForClassicClose();
  GameSession withRivalAtOneHundred = new GameSession(ready.id(), "APUNTADO101", ready.players(),
   ready.currentRound(), ready.round(), ready.turn(), ready.status(), ready.version(),
   List.of(new PlayerScore("p1", 0), new PlayerScore("p2", 100)), ready.completedRounds(), null);
  GameSession finished = closing.attemptClose(withRivalAtOneHundred, "p1", new ReglaApuntado());
  assertEquals(GameStatus.FINISHED, finished.status());
  assertEquals("p1", finished.winnerId());
  assertTrue(finished.scoreOf("p2").eliminated());
  assertEquals(TurnPhase.CLOSED, finished.turn().phase());
 }
 @Test void keepsPlayingAfterEliminatingAPlayerAndCanCloseTheNextRound() {
  GameSession closed = closing.attemptClose(readyForClassicCloseWithThreePlayers(), "p1", new ReglaApuntado());
  assertTrue(closed.scoreOf("p3").eliminated());
  assertEquals(GameStatus.ACTIVE, closed.status());
  assertEquals(2, closed.currentRound());
  assertEquals(2, closed.round().hands().size());
  assertEquals(3, closed.completedRounds().get(0).scores().size());
 }

 private GameSession readyForClassicCloseWithThreePlayers() {
  List<String> groupIds = com.apuntado.tests.support.Decks.CLOSING_HAND;
  List<com.apuntado.domain.model.Card> deck = new ArrayList<>(StandardDeck.shuffled());
  List<com.apuntado.domain.model.Card> winningHand = deck.stream().filter(card -> groupIds.contains(card.id())).toList();
  deck.removeAll(winningHand);
  deck.addAll(0, winningHand);
  List<PlayerSeat> players = List.of(new PlayerSeat("p1", 0, "Ana"), new PlayerSeat("p2", 1, "Luis"),
   new PlayerSeat("p3", 2, "Eva"));
  GameSession session = GameSession.start("game-3", "APUNTADO101", players, deck);
  GameSession withScores = new GameSession(session.id(), session.variantId(), session.players(), session.currentRound(),
   session.round(), session.turn(), session.status(), session.version(),
   List.of(new PlayerScore("p1", 0), new PlayerScore("p2", 0), new PlayerScore("p3", 95)),
   session.completedRounds(), null);
  String drawnCardId = withScores.round().deck().get(0).id();
  return withScores.drawFromDeck("p1").discard("p1", drawnCardId);
 }

 @Test void rejectsRepeatedPlayers() {
  List<PlayerSeat> players = List.of(new PlayerSeat("p1", 0, "Ana"), new PlayerSeat("p1", 1, "Luis"));
  assertThrows(IllegalArgumentException.class, () -> GameSession.start("game-1", "CLASICA", players));
 }
 @Test void rejectsNonConsecutiveSeats() {
  List<PlayerSeat> players = List.of(new PlayerSeat("p1", 0, "Ana"), new PlayerSeat("p2", 2, "Luis"));
  assertThrows(IllegalArgumentException.class, () -> GameSession.start("game-1", "CLASICA", players));
 }
}
