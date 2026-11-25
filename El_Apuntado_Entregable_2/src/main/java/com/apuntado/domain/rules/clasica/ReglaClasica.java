package com.apuntado.domain.rules.clasica;

import com.apuntado.domain.model.Hand;
import com.apuntado.domain.rules.api.IReglaJuego;
import com.apuntado.domain.rules.common.Groups;

/** Clásica: una cuarta y dos ternas (4-3-3) tras el descarte; el primer cierre válido gana la partida. */
public final class ReglaClasica implements IReglaJuego {
 private static final int[] GRUPOS = {4, 3, 3};

 public String id() {
  return "CLASICA";
 }

 public boolean permiteCerrar(Hand hand) {
  return Groups.matches(hand.cards(), GRUPOS, 0);
 }

 @Override
 public boolean terminaPartidaAlCerrar() {
  return true;
 }

 @Override
 public String descripcionCierre() {
  return "una cuarta y dos ternas (4-3-3) sin cartas sobrantes";
 }
}
