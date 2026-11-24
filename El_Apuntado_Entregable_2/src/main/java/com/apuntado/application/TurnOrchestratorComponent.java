package com.apuntado.application;
import com.apuntado.domain.gameplay.HandEvaluatorComponent;
import com.apuntado.domain.gameplay.RoundClosingService;
import com.apuntado.domain.model.GameSession;
import com.apuntado.domain.model.Hand;
import com.apuntado.domain.model.PlayerSeat;
import com.apuntado.bootstrap.RuleRegistry;
import com.apuntado.domain.rules.api.IReglaJuego;
import com.apuntado.domain.scoring.RoundScoringComponent;
import java.util.List;
/** Esqueleto de delegación: aún no ejecuta turnos, persistencia ni notificaciones. */
public final class TurnOrchestratorComponent {
 private final HandEvaluatorComponent evaluator;
 private final RoundClosingService closing;
 private final RuleRegistry rules;
 public TurnOrchestratorComponent(HandEvaluatorComponent evaluator) {
  this(evaluator, new RuleRegistry(), new RoundScoringComponent());
 }
 public TurnOrchestratorComponent(HandEvaluatorComponent evaluator, RuleRegistry rules, RoundScoringComponent scoring) {
  this.evaluator=evaluator;
  this.rules=rules;
  this.closing=new RoundClosingService(scoring);
 }
 public boolean validateClose(Hand hand,IReglaJuego rule) {return evaluator.canClose(hand,rule);}
 public GameSession startGame(String id, String variantId, List<PlayerSeat> players) {
  rules.get(variantId);
  return GameSession.start(id, variantId, players);
 }
 public GameSession drawFromDeck(GameSession game, String playerId) {return game.drawFromDeck(playerId);}
 public GameSession drawFromDiscard(GameSession game, String playerId) {return game.drawFromDiscard(playerId);}
 public GameSession discard(GameSession game, String playerId, String cardId) {return game.discard(playerId, cardId);}
 public GameSession endTurn(GameSession game, String playerId) {return game.endTurn(playerId);}
 public GameSession closeRound(GameSession game, String playerId) {
  return closing.attemptClose(game, playerId, rules.get(game.variantId()));
 }
}
