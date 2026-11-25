package com.apuntado.domain.gameplay;

import com.apuntado.domain.model.Card;
import com.apuntado.domain.model.GameSession;
import com.apuntado.domain.model.Hand;
import com.apuntado.domain.model.PlayerScore;
import com.apuntado.domain.model.PlayerSeat;
import com.apuntado.domain.model.RoundScore;
import com.apuntado.domain.model.StandardDeck;
import com.apuntado.domain.rules.api.IReglaJuego;
import com.apuntado.domain.scoring.RoundScoringComponent;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Intento de cierre y liquidación: valida la mano con la estrategia, calcula todas las penalizaciones
 * sin escribir y entrega al agregado un único cambio de estado (ADR-04).
 */
public final class RoundClosingService {
 private final RoundScoringComponent scoring;
 private final Supplier<List<Card>> decks;

 public RoundClosingService(RoundScoringComponent scoring) {
  this(scoring, StandardDeck::shuffled);
 }

 public RoundClosingService(RoundScoringComponent scoring, Supplier<List<Card>> decks) {
  this.scoring = Objects.requireNonNull(scoring);
  this.decks = Objects.requireNonNull(decks);
 }

 public GameSession attemptClose(GameSession session, String playerId, IReglaJuego rule) {
  Objects.requireNonNull(session);
  Objects.requireNonNull(rule);
  if (!session.variantId().equals(rule.id())) throw new IllegalArgumentException("Regla no corresponde a la partida");
  Hand closerHand = new Hand(session.round().handOf(playerId).cards());
  if (!rule.permiteCerrar(closerHand)) {
   throw new IllegalArgumentException("Cierre inválido: se requiere " + rule.descripcionCierre());
  }
  List<PlayerScore> totals = session.players().stream().map(player -> updatedScore(session, player, playerId)).toList();
  List<RoundScore> results = session.players().stream().map(player -> roundScore(session, player, playerId, totals)).toList();
  return session.closeRound(playerId, totals, results, rule.terminaPartidaAlCerrar(), decks.get());
 }

 private PlayerScore updatedScore(GameSession session, PlayerSeat player, String closerId) {
  int current = session.scoreOf(player.playerId()).total();
  if (!participates(session, player.playerId())) return new PlayerScore(player.playerId(), current);
  int penalty = scoring.penalty(handOf(session, player.playerId()), player.playerId().equals(closerId));
  return new PlayerScore(player.playerId(), scoring.accumulate(current, penalty));
 }

 private RoundScore roundScore(GameSession session, PlayerSeat player, String closerId, List<PlayerScore> totals) {
  int penalty = participates(session, player.playerId())
   ? scoring.penalty(handOf(session, player.playerId()), player.playerId().equals(closerId)) : 0;
  int total = totals.stream().filter(score -> score.playerId().equals(player.playerId())).findFirst().orElseThrow().total();
  return new RoundScore(player.playerId(), penalty, total);
 }

 private static boolean participates(GameSession session, String playerId) {
  return session.round().hands().stream().anyMatch(hand -> hand.playerId().equals(playerId));
 }

 private static Hand handOf(GameSession session, String playerId) {
  return new Hand(session.round().handOf(playerId).cards());
 }
}
