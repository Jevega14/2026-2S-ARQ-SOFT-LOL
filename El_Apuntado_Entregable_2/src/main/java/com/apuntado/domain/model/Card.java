package com.apuntado.domain.model;
import java.util.Objects;
public record Card(String id, int rank, String suit, int points) {
 public Card {
  Objects.requireNonNull(id); Objects.requireNonNull(suit);
  if (id.isBlank() || suit.isBlank()) throw new IllegalArgumentException("Identidad vacía");
  if (rank < 1 || rank > 13 || points < 0) throw new IllegalArgumentException("Carta inválida");
  points = Math.min(rank, 10);
 }
}
