package com.apuntado.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record DiscardRequest(@NotBlank String playerId, @NotBlank String cardId, @NotBlank String commandId) {
}
