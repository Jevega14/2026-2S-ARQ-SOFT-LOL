package com.apuntado.application;

import com.apuntado.bootstrap.RuleRegistry;
import com.apuntado.domain.gameplay.HandEvaluatorComponent;
import com.apuntado.domain.gameplay.RoundClosingService;
import com.apuntado.domain.model.Card;
import com.apuntado.domain.model.GameSession;
import com.apuntado.domain.model.Hand;
import com.apuntado.domain.model.PlayerSeat;
import com.apuntado.domain.model.StandardDeck;
import com.apuntado.domain.rules.api.IReglaJuego;
import com.apuntado.domain.scoring.RoundScoringComponent;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Mediador del turno: aplica cada movimiento sobre el agregado y resuelve la estrategia por
 * {@link RuleRegistry} sin conocer modalidades concretas (ADR-03). No persiste ni notifica.
 */
public final class TurnOrchestratorComponent {
 private final HandEvaluatorComponent evaluator;
 private final RoundClosingService closing;
 private final RuleRegistry rules;
 private final Supplier<List<Card>> decks;

 public TurnOrchestratorComponent(HandEvaluatorComponent evaluator) {
  this(evaluator, new RuleRegistry(), new RoundScoringComponent());
 }

 public TurnOrchestratorComponent(HandEvaluatorComponent evaluator, RuleRegistry rules, RoundScoringComponent scoring) {
  this(evaluator, rules, scoring, StandardDeck::shuffled);
 }

 /** {@code decks} entrega una baraja de 52 cartas por ronda; permite barajas guionadas para demo y pruebas. */
 public TurnOrchestratorComponent(HandEvaluatorComponent evaluator, RuleRegistry rules, RoundScoringComponent scoring,
  Supplier<List<Card>> decks) {
  this.evaluator = Objects.requireNonNull(evaluator);
  this.rules = Objects.requireNonNull(rules);
  this.decks = Objects.requireNonNull(decks);
  this.closing = new RoundClosingService(scoring, decks);
 }

 public boolean validateClose(Hand hand, IReglaJuego rule) {
  return evaluator.canClose(hand, rule);
 }

 public GameSession startGame(String id, String variantId, List<PlayerSeat> players) {
  rules.get(variantId);
  return GameSession.start(id, variantId, players, decks.get());
 }

 public GameSession drawFromDeck(GameSession game, String playerId) {
  return game.drawFromDeck(playerId);
 }

 public GameSession drawFromDiscard(GameSession game, String playerId) {
  return game.drawFromDiscard(playerId);
 }

 public GameSession discard(GameSession game, String playerId, String cardId) {
  return game.discard(playerId, cardId);
 }

 public GameSession endTurn(GameSession game, String playerId) {
  return game.endTurn(playerId);
 }

 public GameSession closeRound(GameSession game, String playerId) {
  return closing.attemptClose(game, playerId, rules.get(game.variantId()));
 }

 public String closeRequirement(String variantId) {
  return rules.get(variantId).descripcionCierre();
 }
}
