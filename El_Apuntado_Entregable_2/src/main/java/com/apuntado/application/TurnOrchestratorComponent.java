package com.apuntado.application;
import com.apuntado.domain.gameplay.HandEvaluatorComponent;
import com.apuntado.domain.model.Hand;
import com.apuntado.domain.rules.api.IReglaJuego;
/** Esqueleto de delegación: aún no ejecuta turnos, persistencia ni notificaciones. */
public final class TurnOrchestratorComponent {
 private final HandEvaluatorComponent evaluator;
 public TurnOrchestratorComponent(HandEvaluatorComponent evaluator) {this.evaluator=evaluator;}
 public boolean validateClose(Hand hand,IReglaJuego rule) {return evaluator.canClose(hand,rule);}
}
