package com.apuntado.tests.support;

import com.apuntado.domain.model.Card;
import com.apuntado.domain.model.StandardDeck;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.function.Supplier;

/**
 * Barajas guionadas para pruebas. El reparto de {@code RoundState.open} entrega 10 cartas por asiento
 * activo en orden, luego una al pozo y el resto queda en el mazo.
 */
public final class Decks {
 /** A♥ A♦ A♠ A♣ 9♥ 9♦ 9♠ J♥ J♦ J♠: cierra 4-3-3 (Clásica) y 3-3-3 + A de 1 punto (Apuntado). */
 public static final List<String> CLOSING_HAND =
  List.of("1H", "1D", "1S", "1C", "9H", "9D", "9S", "11H", "11D", "11S");

 private Decks() {
 }

 /** Baraja de 52 cartas cuyas primeras manos son exactamente las indicadas (por asiento). */
 @SafeVarargs
 public static List<Card> dealing(List<String>... hands) {
  List<Card> deck = new ArrayList<>(StandardDeck.shuffled());
  List<Card> front = new ArrayList<>();
  for (List<String> hand : hands) {
   for (String id : hand) {
    Card card = deck.stream().filter(candidate -> candidate.id().equals(id)).findFirst()
     .orElseThrow(() -> new IllegalArgumentException("Carta repetida o inexistente: " + id));
    deck.remove(card);
    front.add(card);
   }
  }
  front.addAll(deck);
  return List.copyOf(front);
 }

 public static List<String> ids(String... ids) {
  return Arrays.asList(ids);
 }

 /** Entrega las barajas en orden y luego barajas aleatorias. */
 @SafeVarargs
 public static Supplier<List<Card>> sequence(List<Card>... decks) {
  Deque<List<Card>> pending = new ArrayDeque<>(Arrays.asList(decks));
  return () -> pending.isEmpty() ? StandardDeck.shuffled() : pending.poll();
 }

 public static int points(List<String> ids) {
  return ids.stream().mapToInt(id -> Math.min(Integer.parseInt(id.substring(0, id.length() - 1)), 10)).sum();
 }
}
