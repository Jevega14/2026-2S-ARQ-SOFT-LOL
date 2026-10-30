package com.apuntado.domain.model;
import java.util.List;
public record Hand(List<Card> cards) {
 public Hand {
  cards=List.copyOf(cards);
  if (cards.size()!=10) throw new IllegalArgumentException("Se evalúan diez cartas tras descartar");
  if (cards.stream().map(Card::id).distinct().count()!=cards.size()) throw new IllegalArgumentException("Carta repetida");
 }
}
