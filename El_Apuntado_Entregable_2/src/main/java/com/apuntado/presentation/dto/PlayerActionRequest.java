package com.apuntado.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record PlayerActionRequest(@NotBlank String playerId, @NotBlank String commandId) {
}
