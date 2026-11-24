package com.apuntado.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class StandardDeck {
 private static final List<String> SUITS = List.of("H", "D", "S", "C");
 private StandardDeck() {}
 public static List<Card> shuffled() {
  List<Card> cards = new ArrayList<>();
  for (String suit : SUITS) {
   for (int rank = 1; rank <= 13; rank++) cards.add(new Card(rank + suit, rank, suit, rank));
  }
  Collections.shuffle(cards);
  return List.copyOf(cards);
 }
}
