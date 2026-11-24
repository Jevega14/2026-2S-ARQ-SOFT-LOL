package com.apuntado.domain.model;

import java.util.Objects;

public record PlayerScore(String playerId, int total) {
 public PlayerScore {
  Objects.requireNonNull(playerId);
  if (playerId.isBlank() || total < 0) throw new IllegalArgumentException("Puntaje inválido");
 }

 public boolean eliminated() {
  return total >= 101;
 }
}
