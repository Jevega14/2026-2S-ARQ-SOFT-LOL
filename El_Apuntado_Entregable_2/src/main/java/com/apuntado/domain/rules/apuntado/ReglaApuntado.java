package com.apuntado.domain.rules.apuntado;

import com.apuntado.domain.model.Hand;
import com.apuntado.domain.rules.api.IReglaJuego;
import com.apuntado.domain.rules.common.Groups;

/** Apuntado: tres ternas y sobrante estrictamente menor que 5 puntos; se juegan rondas hasta 101. */
public final class ReglaApuntado implements IReglaJuego {
 private static final int[] GRUPOS = {3, 3, 3};
 private static final int SOBRANTE_MAXIMO = 4;

 public String id() {
  return "APUNTADO101";
 }

 public boolean permiteCerrar(Hand hand) {
  return Groups.matches(hand.cards(), GRUPOS, SOBRANTE_MAXIMO);
 }

 @Override
 public String descripcionCierre() {
  return "tres ternas y una carta sobrante de menos de 5 puntos";
 }
}
