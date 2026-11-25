package com.apuntado.domain.rules.api;

import com.apuntado.domain.model.Hand;

/**
 * Contrato Strategy de una modalidad de juego (ADR-02 / ADR-03).
 * Los métodos {@code default} amplían el contrato sin romper proveedores existentes.
 */
public interface IReglaJuego {
 String id();

 boolean permiteCerrar(Hand hand);

 /** {@code true} si el primer cierre válido termina la partida (Clásica); {@code false} si se juegan rondas. */
 default boolean terminaPartidaAlCerrar() {
  return false;
 }

 /** Condición de cierre en lenguaje del jugador; se usa para explicar un cierre rechazado. */
 default String descripcionCierre() {
  return "una combinación válida para la modalidad " + id();
 }
}
