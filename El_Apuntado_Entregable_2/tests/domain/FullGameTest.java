package com.apuntado.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.apuntado.domain.gameplay.RoundClosingService;
import com.apuntado.domain.model.Card;
import com.apuntado.domain.model.GameSession;
import com.apuntado.domain.model.GameStatus;
import com.apuntado.domain.model.PlayerScore;
import com.apuntado.domain.model.PlayerSeat;
import com.apuntado.domain.model.RoundScore;
import com.apuntado.domain.model.RoundState;
import com.apuntado.domain.rules.apuntado.ReglaApuntado;
import com.apuntado.domain.rules.clasica.ReglaClasica;
import com.apuntado.domain.scoring.RoundScoringComponent;
import com.apuntado.tests.support.Decks;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Partidas completas con barajas guionadas: verifica la acumulación entre rondas, la detección exacta
 * del umbral de 101 y las invariantes del modelo (retroalimentación del Entregable 2, punto 2.2).
 */
class FullGameTest {
 private static final List<String> RIVAL_73 = Decks.ids("13H", "13D", "13S", "13C", "12H", "12D", "2C", "3C", "4C", "4D");
 private static final List<String> RIVAL_72 = Decks.ids("13H", "13D", "13S", "13C", "12H", "12D", "2C", "3C", "4C", "3D");
 private static final List<String> RIVAL_28 = Decks.ids("2H", "2D", "2S", "2C", "3H", "3D", "3S", "3C", "4H", "4D");

 private static List<PlayerSeat> players() {
  return List.of(new PlayerSeat("p1", 0, "Ana"), new PlayerSeat("p2", 1, "Luis"));
 }

 /** El jugador en turno roba del mazo y descarta la misma carta: su mano no cambia. */
 private static GameSession drawAndDiscardSame(GameSession game, String playerId) {
  GameSession drawn = game.drawFromDeck(playerId);
  List<Card> hand = drawn.round().handOf(playerId).cards();
  return drawn.discard(playerId, hand.get(hand.size() - 1).id());
 }

 private static void assertTotalsMatchHistory(GameSession game) {
  for (PlayerSeat player : game.players()) {
   int sum = game.completedRounds().stream().flatMap(round -> round.scores().stream())
    .filter(score -> score.playerId().equals(player.playerId())).mapToInt(RoundScore::penalty).sum();
   assertEquals(sum, game.scoreOf(player.playerId()).total(), "el total debe igualar el historial de " + player.playerId());
  }
  for (RoundState round : game.completedRounds()) {
   assertEquals(round.closedBy(), round.scores().stream().filter(score -> score.penalty() == 0)
    .map(RoundScore::playerId).filter(round.closedBy()::equals).findFirst().orElseThrow());
  }
 }

 @Test
 @DisplayName("Apuntado: 73 + 28 = 101 exactos elimina al rival y termina la partida")
 void apuntadoDetectsExactly101() {
  RoundClosingService closing = new RoundClosingService(new RoundScoringComponent(),
   Decks.sequence(Decks.dealing(Decks.CLOSING_HAND, RIVAL_28)));
  ReglaApuntado rule = new ReglaApuntado();
  GameSession game = GameSession.start("full-101", "APUNTADO101", players(), Decks.dealing(Decks.CLOSING_HAND, RIVAL_73));
  assertEquals(73, Decks.points(RIVAL_73));
  assertEquals(72, Decks.points(RIVAL_72));
  assertEquals(28, Decks.points(RIVAL_28));

  game = closing.attemptClose(drawAndDiscardSame(game, "p1"), "p1", rule);
  assertEquals(GameStatus.ACTIVE, game.status());
  assertEquals(2, game.currentRound());
  assertEquals(73, game.scoreOf("p2").total());
  assertEquals("p2", game.turn().playerId(), "la siguiente ronda la inicia el jugador posterior a quien cerró");

  game = drawAndDiscardSame(game, "p2").endTurn("p2");
  game = closing.attemptClose(drawAndDiscardSame(game, "p1"), "p1", rule);

  assertEquals(101, game.scoreOf("p2").total());
  assertTrue(game.scoreOf("p2").eliminated());
  assertEquals(GameStatus.FINISHED, game.status());
  assertEquals("p1", game.winnerId());
  assertEquals(2, game.completedRounds().size());
  assertTotalsMatchHistory(game);
 }

 @Test
 @DisplayName("Apuntado: 72 + 28 = 100 no elimina y se reparte una tercera ronda")
 void apuntadoKeepsPlayingAt100() {
  RoundClosingService closing = new RoundClosingService(new RoundScoringComponent(),
   Decks.sequence(Decks.dealing(Decks.CLOSING_HAND, RIVAL_28)));
  ReglaApuntado rule = new ReglaApuntado();
  GameSession game = GameSession.start("full-100", "APUNTADO101", players(), Decks.dealing(Decks.CLOSING_HAND, RIVAL_72));
  game = closing.attemptClose(drawAndDiscardSame(game, "p1"), "p1", rule);
  game = drawAndDiscardSame(game, "p2").endTurn("p2");
  game = closing.attemptClose(drawAndDiscardSame(game, "p1"), "p1", rule);

  assertEquals(100, game.scoreOf("p2").total());
  assertFalse(game.scoreOf("p2").eliminated());
  assertEquals(GameStatus.ACTIVE, game.status());
  assertEquals(3, game.currentRound());
  assertNull(game.winnerId());
  assertTotalsMatchHistory(game);
 }

 @Test
 @DisplayName("Apuntado con tres jugadores: el eliminado ya no recibe mano y los turnos lo saltan")
 void eliminatedPlayersLeaveTheTable() {
  List<PlayerSeat> three = List.of(new PlayerSeat("p1", 0, "Ana"), new PlayerSeat("p2", 1, "Luis"),
   new PlayerSeat("p3", 2, "Eva"));
  List<String> rivalLow = Decks.ids("2H", "2D", "2S", "2C", "3H", "3D", "3S", "3C", "4H", "4D");
  List<String> rivalHigh = Decks.ids("13H", "13D", "13S", "13C", "12H", "12D", "12S", "12C", "10H", "10D");
  RoundClosingService closing = new RoundClosingService(new RoundScoringComponent());
  GameSession start = GameSession.start("full-3", "APUNTADO101", three, Decks.dealing(Decks.CLOSING_HAND, rivalLow, rivalHigh));
  GameSession withScores = new GameSession(start.id(), start.variantId(), start.players(), 1, start.round(), start.turn(),
   start.status(), 0, List.of(new PlayerScore("p1", 0), new PlayerScore("p2", 0), new PlayerScore("p3", 2)), List.of(), null);

  GameSession next = closing.attemptClose(drawAndDiscardSame(withScores, "p1"), "p1", new ReglaApuntado());

  assertEquals(102, next.scoreOf("p3").total());
  assertTrue(next.scoreOf("p3").eliminated());
  assertEquals(GameStatus.ACTIVE, next.status());
  assertEquals(2, next.round().hands().size());
  assertTrue(next.round().hands().stream().noneMatch(hand -> hand.playerId().equals("p3")));
  assertEquals("p2", next.turn().playerId());
  GameSession afterP2 = drawAndDiscardSame(next, "p2").endTurn("p2");
  assertEquals("p1", afterP2.turn().playerId(), "el turno salta al eliminado");
 }

 @Test
 @DisplayName("Clásica: el primer cierre 4-3-3 termina la partida aunque el rival tenga pocos puntos")
 void classicIsASingleRound() {
  GameSession game = GameSession.start("classic", "CLASICA", players(), Decks.dealing(Decks.CLOSING_HAND, RIVAL_28));
  GameSession finished = new RoundClosingService(new RoundScoringComponent())
   .attemptClose(drawAndDiscardSame(game, "p1"), "p1", new ReglaClasica());
  assertEquals(GameStatus.FINISHED, finished.status());
  assertEquals("p1", finished.winnerId());
  assertEquals(28, finished.scoreOf("p2").total());
  assertEquals(1, finished.completedRounds().size());
  assertThrows(IllegalStateException.class, () -> finished.drawFromDeck("p2"), "no se juega después del cierre");
 }

 @Test
 @DisplayName("Cuando el mazo se agota, el pozo se rebaraja conservando la última carta descartada")
 void reshufflesTheDiscardPileWhenTheDeckIsEmpty() {
  GameSession game = GameSession.start("reshuffle", "APUNTADO101", players());
  int guard = 0;
  while (!game.round().deck().isEmpty()) {
   String player = game.turn().playerId();
   game = drawAndDiscardSame(game, player).endTurn(player);
   assertTrue(++guard < 100);
  }
  Card top = game.round().discard().get(game.round().discard().size() - 1);
  int recyclable = game.round().discard().size() - 1;
  String player = game.turn().playerId();

  GameSession afterDraw = game.drawFromDeck(player);

  assertEquals(List.of(top), afterDraw.round().discard(), "la cima del pozo se conserva");
  assertEquals(recyclable - 1, afterDraw.round().deck().size());
  assertEquals(11, afterDraw.round().handOf(player).cards().size());
  List<String> all = new ArrayList<>();
  afterDraw.round().hands().forEach(hand -> hand.cards().forEach(card -> all.add(card.id())));
  afterDraw.round().deck().forEach(card -> all.add(card.id()));
  afterDraw.round().discard().forEach(card -> all.add(card.id()));
  assertEquals(52, all.stream().distinct().count(), "ninguna carta se duplica ni se pierde");
 }

 @Test
 @DisplayName("Sin cartas en mazo ni pozo reciclable, robar es un conflicto explícito")
 void cannotReshuffleASingleCard() {
  GameSession game = GameSession.start("empty", "APUNTADO101", players());
  RoundState round = game.round();
  RoundState noDeckOneDiscard = new RoundState(round.number(), List.of(
   round.hands().get(0).add(round.deck().get(0)), withAll(round.hands().get(1), round.deck().subList(1, 31))),
   List.of(), round.discard(), round.status(), null, List.of());
  assertThrows(IllegalStateException.class, () -> noDeckOneDiscard.drawFromDeck("p1"));
 }

 private static com.apuntado.domain.model.PlayerHandState withAll(com.apuntado.domain.model.PlayerHandState hand,
  List<Card> cards) {
  com.apuntado.domain.model.PlayerHandState result = hand;
  for (Card card : cards) result = result.add(card);
  return result;
 }
}
