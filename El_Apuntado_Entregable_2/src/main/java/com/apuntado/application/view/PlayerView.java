package com.apuntado.application.view;

public record PlayerView(
 String playerId,
 String displayName,
 int seat,
 int total,
 boolean eliminated,
 int cardCount,
 boolean currentTurn
) {
}
