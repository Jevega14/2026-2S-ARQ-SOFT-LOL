package com.apuntado.domain.model;

import java.util.Objects;

public record TurnState(String playerId, int number, TurnPhase phase) {
 public TurnState {
  Objects.requireNonNull(playerId);
  Objects.requireNonNull(phase);
  if (playerId.isBlank() || number < 1) throw new IllegalArgumentException("Turno inválido");
 }
}
