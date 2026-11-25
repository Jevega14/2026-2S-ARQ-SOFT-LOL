package com.apuntado.config;

import com.apuntado.domain.model.Card;
import com.apuntado.domain.model.StandardDeck;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Baraja guionada para la sustentación ({@code apuntado.deck=demo}). El primer asiento recibe
 * A♥ A♦ A♠ A♣ 9♥ 9♦ 9♠ J♥ J♦ J♠: ya es 4-3-3 en Clásica y 3-3-3 + A (1 punto) en Apuntado.
 * Basta robar y descartar la carta robada para mostrar un cierre válido; el resto se baraja al azar.
 * No se usa en las pruebas con usuarios, que juegan con {@code apuntado.deck=random}.
 */
public final class DemoDeckSupplier implements Supplier<List<Card>> {
 static final Set<String> FIRST_HAND = Set.of("1H", "1D", "1S", "1C", "9H", "9D", "9S", "11H", "11D", "11S");

 @Override
 public List<Card> get() {
  List<Card> deck = new ArrayList<>(StandardDeck.shuffled());
  List<Card> scripted = deck.stream().filter(card -> FIRST_HAND.contains(card.id())).toList();
  deck.removeAll(scripted);
  deck.addAll(0, scripted);
  return List.copyOf(deck);
 }
}
