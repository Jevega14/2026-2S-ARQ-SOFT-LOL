package com.apuntado.domain.rules.apuntado;
import com.apuntado.domain.rules.api.IReglaJuego;
import com.apuntado.domain.rules.common.Groups;
import com.apuntado.domain.model.Hand;
public final class ReglaApuntado implements IReglaJuego {
 public String id() {return "APUNTADO101";}
 public boolean permiteCerrar(Hand hand) {return Groups.matches(hand.cards(),new int[]{3,3,3},4);}
}
