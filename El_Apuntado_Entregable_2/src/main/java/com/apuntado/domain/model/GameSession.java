package com.apuntado.domain.model;

import java.util.List;
import java.util.Objects;

public record GameSession(
 String id,
 String variantId,
 List<PlayerSeat> players,
 int currentRound,
 RoundState round,
 TurnState turn,
 GameStatus status,
 int version,
 List<PlayerScore> totals,
 List<RoundState> completedRounds,
 String winnerId
) {
 public GameSession {
  Objects.requireNonNull(id);
  Objects.requireNonNull(variantId);
  Objects.requireNonNull(round);
  Objects.requireNonNull(turn);
  Objects.requireNonNull(status);
  Objects.requireNonNull(totals);
  Objects.requireNonNull(completedRounds);
  players = List.copyOf(players);
  totals = List.copyOf(totals);
  completedRounds = List.copyOf(completedRounds);
  if (id.isBlank() || variantId.isBlank() || currentRound < 1 || version < 0) {
   throw new IllegalArgumentException("Sesión inválida");
  }
  validatePlayers(players);
  if (round.number() != currentRound) throw new IllegalArgumentException("Ronda actual inválida");
  validateTotals(totals, players);
  if (players.stream().noneMatch(player -> player.playerId().equals(turn.playerId()))) {
   throw new IllegalArgumentException("El turno debe pertenecer a un jugador");
  }
  if (status == GameStatus.ACTIVE) validateActiveState(round, turn, winnerId, totals, players);
  if (status == GameStatus.FINISHED) validateFinishedState(round, turn, winnerId, totals);
  if (status == GameStatus.WAITING) throw new IllegalArgumentException("La sesión debe iniciar activa");
 }

 public static GameSession start(String id, String variantId, List<PlayerSeat> players) {
  return start(id, variantId, players, StandardDeck.shuffled());
 }

 public static GameSession start(String id, String variantId, List<PlayerSeat> players, List<Card> deck) {
  List<PlayerSeat> seats = List.copyOf(players);
  RoundState round = RoundState.open(1, seats, deck);
  return new GameSession(id, variantId, seats, 1, round,
   new TurnState(seats.get(0).playerId(), 1, TurnPhase.DRAW), GameStatus.ACTIVE, 0,
   seats.stream().map(player -> new PlayerScore(player.playerId(), 0)).toList(), List.of(), null);
 }

 public GameSession drawFromDeck(String playerId) {
  requireTurn(playerId, TurnPhase.DRAW);
  return update(round.drawFromDeck(playerId), new TurnState(playerId, turn.number(), TurnPhase.DISCARD));
 }

 public GameSession drawFromDiscard(String playerId) {
  requireTurn(playerId, TurnPhase.DRAW);
  return update(round.drawFromDiscard(playerId), new TurnState(playerId, turn.number(), TurnPhase.DISCARD));
 }

 public GameSession discard(String playerId, String cardId) {
  requireTurn(playerId, TurnPhase.DISCARD);
  if (round.handOf(playerId).cards().size() != 11) throw new IllegalStateException("Debe robar antes de descartar");
  return update(round.discard(playerId, cardId), turn);
 }

 public GameSession endTurn(String playerId) {
  requireTurn(playerId, TurnPhase.DISCARD);
  if (round.handOf(playerId).cards().size() != 10) throw new IllegalStateException("Debe descartar antes de terminar");
  PlayerSeat nextPlayer = nextActiveAfter(playerId, totals);
  return update(round, new TurnState(nextPlayer.playerId(), turn.number() + 1, TurnPhase.DRAW));
 }

 private GameSession update(RoundState updatedRound, TurnState updatedTurn) {
  return new GameSession(id, variantId, players, currentRound, updatedRound, updatedTurn, status, version + 1,
   totals, completedRounds, winnerId);
 }

 /**
  * Cierra la ronda del jugador en turno con los puntajes ya liquidados.
  * Termina la partida si la modalidad lo indica ({@code endsGame}) o si solo queda un jugador activo;
  * en otro caso abre la siguiente ronda con {@code nextDeck}.
  */
 public GameSession closeRound(String playerId, List<PlayerScore> updatedTotals, List<RoundScore> results,
  boolean endsGame, List<Card> nextDeck) {
  requireTurn(playerId, TurnPhase.DISCARD);
  PlayerHandState closerHand = round.handOf(playerId);
  if (closerHand.cards().size() != 10) throw new IllegalArgumentException("La mano debe tener diez cartas");
  validateTotals(updatedTotals, players);
  RoundState closedRound = round.close(playerId, results);
  List<RoundState> history = new java.util.ArrayList<>(completedRounds);
  history.add(closedRound);
  List<PlayerSeat> activePlayers = activePlayers(players, updatedTotals);
  if (endsGame || activePlayers.size() == 1) {
   return new GameSession(id, variantId, players, currentRound, closedRound,
    new TurnState(playerId, turn.number(), TurnPhase.CLOSED), GameStatus.FINISHED, version + 1,
    updatedTotals, history, playerId);
  }
  PlayerSeat nextStarter = nextActiveAfter(playerId, updatedTotals);
  RoundState nextRound = RoundState.open(currentRound + 1, activePlayers, nextDeck);
  return new GameSession(id, variantId, players, currentRound + 1, nextRound,
   new TurnState(nextStarter.playerId(), 1, TurnPhase.DRAW), GameStatus.ACTIVE, version + 1,
   updatedTotals, history, null);
 }

 public PlayerScore scoreOf(String playerId) {
  return totals.stream().filter(score -> score.playerId().equals(playerId)).findFirst()
   .orElseThrow(() -> new IllegalArgumentException("Jugador fuera de la partida"));
 }

 private static List<PlayerSeat> activePlayers(List<PlayerSeat> players, List<PlayerScore> scores) {
  return players.stream().filter(player -> scores.stream().anyMatch(score -> score.playerId().equals(player.playerId())
   && !score.eliminated())).toList();
 }

 private PlayerSeat nextActiveAfter(String playerId, List<PlayerScore> scores) {
  int currentSeat = seatOf(playerId);
  for (int offset = 1; offset <= players.size(); offset++) {
   PlayerSeat candidate = players.get((currentSeat + offset) % players.size());
   if (!scoreFor(scores, candidate.playerId()).eliminated()) return candidate;
  }
  throw new IllegalStateException("No hay jugadores activos");
 }

 private static PlayerScore scoreFor(List<PlayerScore> scores, String playerId) {
  return scores.stream().filter(score -> score.playerId().equals(playerId)).findFirst().orElseThrow();
 }

 private static void validateActiveState(RoundState round, TurnState turn, String winnerId, List<PlayerScore> totals,
  List<PlayerSeat> players) {
  if (round.status() != RoundStatus.OPEN || round.hands().size() != activePlayers(players, totals).size()
   || turn.phase() == TurnPhase.CLOSED || winnerId != null) throw new IllegalArgumentException("Estado activo inválido");
  if (scoreFor(totals, turn.playerId()).eliminated()) throw new IllegalArgumentException("Turno de jugador eliminado");
 }

 /** Quien cierra suma 0, así que nunca queda eliminado: el ganador siempre es quien cerró la última ronda. */
 private static void validateFinishedState(RoundState round, TurnState turn, String winnerId,
  List<PlayerScore> totals) {
  if (round.status() != RoundStatus.CLOSED || turn.phase() != TurnPhase.CLOSED || winnerId == null
   || !winnerId.equals(round.closedBy()) || scoreFor(totals, winnerId).eliminated()) {
   throw new IllegalArgumentException("Estado final inválido");
  }
 }

 private static void validateTotals(List<PlayerScore> totals, List<PlayerSeat> players) {
  if (totals.size() != players.size() || totals.stream().map(PlayerScore::playerId).distinct().count() != totals.size()) {
   throw new IllegalArgumentException("Puntajes incompletos");
  }
  for (PlayerSeat player : players) scoreFor(totals, player.playerId());
 }

 private void requireTurn(String playerId, TurnPhase expectedPhase) {
  if (!turn.playerId().equals(playerId) || turn.phase() != expectedPhase) {
   throw new IllegalStateException("Movimiento fuera de turno");
  }
 }

 private int seatOf(String playerId) {
  for (int index = 0; index < players.size(); index++) {
   if (players.get(index).playerId().equals(playerId)) return index;
  }
  throw new IllegalArgumentException("Jugador fuera de la partida");
 }

 private static void validatePlayers(List<PlayerSeat> players) {
  if (players.size() < 2 || players.size() > 4) throw new IllegalArgumentException("Participan de 2 a 4 jugadores");
  if (players.stream().map(PlayerSeat::playerId).distinct().count() != players.size()) {
   throw new IllegalArgumentException("Jugador repetido");
  }
  for (int index = 0; index < players.size(); index++) {
   if (players.get(index).seat() != index) throw new IllegalArgumentException("Sillas inválidas");
  }
 }
}
