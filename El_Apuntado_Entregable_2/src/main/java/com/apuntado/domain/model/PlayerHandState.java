package com.apuntado.domain.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record PlayerHandState(String playerId, List<Card> cards) {
 public PlayerHandState {
  Objects.requireNonNull(playerId);
  cards = List.copyOf(cards);
  if (playerId.isBlank() || cards.stream().map(Card::id).distinct().count() != cards.size()) {
   throw new IllegalArgumentException("Mano inválida");
  }
 }

 public PlayerHandState add(Card card) {
  List<Card> updated = new ArrayList<>(cards);
  updated.add(Objects.requireNonNull(card));
  return new PlayerHandState(playerId, updated);
 }

 public PlayerHandState remove(String cardId) {
  int index = indexOf(cardId);
  List<Card> updated = new ArrayList<>(cards);
  updated.remove(index);
  return new PlayerHandState(playerId, updated);
 }

 public Card card(String cardId) {
  return cards.get(indexOf(cardId));
 }

 private int indexOf(String cardId) {
  Objects.requireNonNull(cardId);
  for (int index = 0; index < cards.size(); index++) {
   if (cards.get(index).id().equals(cardId)) return index;
  }
  throw new IllegalArgumentException("La carta no pertenece a la mano");
 }
}
