package com.apuntado.application.view;

import java.util.List;

/**
 * Proyección autorizada del estado. Incluye únicamente la mano del jugador consultante;
 * de los rivales solo se expone el número de cartas (privacidad, ADR-05).
 */
public record GameView(
 String gameId,
 String variantId,
 String status,
 int currentRound,
 int version,
 String viewerId,
 TurnView turn,
 String winnerId,
 List<PlayerView> players,
 List<CardView> hand,
 int deckCount,
 int discardCount,
 CardView discardTop,
 List<RoundResultView> lastRoundResults,
 String lastClosedBy,
 String closeRequirement
) {
}
