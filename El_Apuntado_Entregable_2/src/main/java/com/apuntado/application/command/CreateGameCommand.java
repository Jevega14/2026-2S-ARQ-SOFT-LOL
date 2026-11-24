package com.apuntado.application.command;

import java.util.List;

public record CreateGameCommand(String variantId, List<PlayerInput> players) {
 public CreateGameCommand {
  players = players == null ? List.of() : List.copyOf(players);
 }
}
