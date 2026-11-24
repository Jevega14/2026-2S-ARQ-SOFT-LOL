package com.apuntado.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record CreatePlayerRequest(
 @NotBlank String playerId,
 @PositiveOrZero int seat,
 @NotBlank String displayName
) {
}
