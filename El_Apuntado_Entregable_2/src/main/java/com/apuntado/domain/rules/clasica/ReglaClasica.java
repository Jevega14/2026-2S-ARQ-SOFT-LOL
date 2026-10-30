package com.apuntado.domain.rules.clasica;
import com.apuntado.domain.rules.api.IReglaJuego;
import com.apuntado.domain.rules.common.Groups;
import com.apuntado.domain.model.Hand;
public final class ReglaClasica implements IReglaJuego {
 public String id() {return "CLASICA";}
 public boolean permiteCerrar(Hand hand) {return Groups.matches(hand.cards(),new int[]{4,3,3},0);}
}
