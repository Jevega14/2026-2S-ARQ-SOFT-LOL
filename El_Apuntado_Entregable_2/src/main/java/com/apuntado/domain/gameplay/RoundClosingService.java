package com.apuntado.domain.gameplay;

import com.apuntado.domain.model.GameSession;
import com.apuntado.domain.model.Hand;
import com.apuntado.domain.model.PlayerScore;
import com.apuntado.domain.model.PlayerSeat;
import com.apuntado.domain.model.RoundScore;
import com.apuntado.domain.rules.api.IReglaJuego;
import com.apuntado.domain.scoring.RoundScoringComponent;
import java.util.List;
import java.util.Objects;

public final class RoundClosingService {
 private final RoundScoringComponent scoring;
 public RoundClosingService(RoundScoringComponent scoring) {this.scoring = Objects.requireNonNull(scoring);}
 public GameSession attemptClose(GameSession session, String playerId, IReglaJuego rule) {
  Objects.requireNonNull(session);
  Objects.requireNonNull(rule);
  if (!session.variantId().equals(rule.id())) throw new IllegalArgumentException("Regla no corresponde a la partida");
  Hand closerHand = new Hand(session.round().handOf(playerId).cards());
  if (!rule.permiteCerrar(closerHand)) throw new IllegalArgumentException("Cierre inválido");
  List<PlayerScore> totals = session.players().stream().map(player -> updatedScore(session, player, playerId)).toList();
  List<RoundScore> results = session.players().stream().map(player -> roundScore(session, player, playerId, totals)).toList();
  return session.closeRound(playerId, totals, results);
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
