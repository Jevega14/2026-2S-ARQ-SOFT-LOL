package com.apuntado.domain.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record RoundState(
 int number,
 List<PlayerHandState> hands,
 List<Card> deck,
 List<Card> discard,
 RoundStatus status,
 String closedBy,
 List<RoundScore> scores
) {
 public RoundState {
  Objects.requireNonNull(hands);
  Objects.requireNonNull(deck);
  Objects.requireNonNull(discard);
  Objects.requireNonNull(status);
  Objects.requireNonNull(scores);
  hands = List.copyOf(hands);
  deck = List.copyOf(deck);
  discard = List.copyOf(discard);
  scores = List.copyOf(scores);
  if (number < 1 || hands.size() < 2) throw new IllegalArgumentException("Ronda inválida");
  validateCards(hands, deck, discard);
 }

 public static RoundState open(int number, List<PlayerSeat> players, List<Card> cards) {
  Objects.requireNonNull(players);
  List<Card> source = List.copyOf(cards);
  if (source.size() != 52 || source.stream().map(Card::id).distinct().count() != 52) {
   throw new IllegalArgumentException("La ronda requiere una baraja de 52 cartas únicas");
  }
  List<PlayerHandState> hands = new ArrayList<>();
  int nextCard = 0;
  for (PlayerSeat player : players) {
   hands.add(new PlayerHandState(player.playerId(), source.subList(nextCard, nextCard + 10)));
   nextCard += 10;
  }
  return new RoundState(number, hands, source.subList(nextCard + 1, source.size()),
   List.of(source.get(nextCard)), RoundStatus.OPEN, null, List.of());
 }

 public PlayerHandState handOf(String playerId) {
  Objects.requireNonNull(playerId);
  return hands.stream().filter(hand -> hand.playerId().equals(playerId)).findFirst()
   .orElseThrow(() -> new IllegalArgumentException("Jugador fuera de la ronda"));
 }

 public RoundState drawFromDeck(String playerId) {
  if (deck.isEmpty()) throw new IllegalStateException("No quedan cartas en el mazo");
  Card drawn = deck.get(0);
  List<Card> remainingDeck = new ArrayList<>(deck);
  remainingDeck.remove(0);
  return withHand(handOf(playerId).add(drawn), remainingDeck, discard);
 }

 public RoundState drawFromDiscard(String playerId) {
  Card drawn = discard.get(discard.size() - 1);
  List<Card> remainingDiscard = new ArrayList<>(discard);
  remainingDiscard.remove(remainingDiscard.size() - 1);
  return withHand(handOf(playerId).add(drawn), deck, remainingDiscard);
 }

 public RoundState discard(String playerId, String cardId) {
  PlayerHandState hand = handOf(playerId);
  Card card = hand.card(cardId);
  List<Card> updatedDiscard = new ArrayList<>(discard);
  updatedDiscard.add(card);
  return withHand(hand.remove(cardId), deck, updatedDiscard);
 }

 private RoundState withHand(PlayerHandState updatedHand, List<Card> updatedDeck, List<Card> updatedDiscard) {
  List<PlayerHandState> updatedHands = new ArrayList<>(hands);
  for (int index = 0; index < updatedHands.size(); index++) {
   if (updatedHands.get(index).playerId().equals(updatedHand.playerId())) updatedHands.set(index, updatedHand);
  }
  return new RoundState(number, updatedHands, updatedDeck, updatedDiscard, status, closedBy, scores);
 }

 public RoundState close(String playerId, List<RoundScore> results) {
  if (status != RoundStatus.OPEN || !handOf(playerId).playerId().equals(playerId)) {
   throw new IllegalStateException("La ronda no puede cerrarse");
  }
  return new RoundState(number, hands, deck, discard, RoundStatus.CLOSED, playerId, results);
 }

 private static void validateCards(List<PlayerHandState> hands, List<Card> deck, List<Card> discard) {
  List<Card> allCards = new ArrayList<>(deck);
  allCards.addAll(discard);
  hands.forEach(hand -> allCards.addAll(hand.cards()));
  if (allCards.size() != 52 || allCards.stream().map(Card::id).distinct().count() != allCards.size()) {
   throw new IllegalArgumentException("Las cartas deben pertenecer a una única baraja");
  }
 }
}
