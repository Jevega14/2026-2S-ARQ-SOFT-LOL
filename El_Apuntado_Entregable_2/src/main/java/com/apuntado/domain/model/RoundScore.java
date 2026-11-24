package com.apuntado.domain.model;

import java.util.Objects;

public record RoundScore(String playerId, int penalty, int totalAfter) {
 public RoundScore {
  Objects.requireNonNull(playerId);
  if (playerId.isBlank() || penalty < 0 || totalAfter < 0) throw new IllegalArgumentException("Resultado inválido");
 }
}
