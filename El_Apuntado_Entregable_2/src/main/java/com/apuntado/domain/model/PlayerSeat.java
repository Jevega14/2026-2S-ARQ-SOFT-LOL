package com.apuntado.domain.model;

import java.util.Objects;

public record PlayerSeat(String playerId, int seat, String displayName) {
 public PlayerSeat {
  Objects.requireNonNull(playerId);
  Objects.requireNonNull(displayName);
  if (playerId.isBlank() || displayName.isBlank() || seat < 0) {
   throw new IllegalArgumentException("Jugador inválido");
  }
 }
}
