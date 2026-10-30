package com.apuntado.domain.gameplay;
import com.apuntado.domain.model.Hand;
import com.apuntado.domain.rules.api.IReglaJuego;
public final class HandEvaluatorComponent {
 public boolean canClose(Hand hand,IReglaJuego rule) {return rule.permiteCerrar(hand);}
}
