package com.apuntado.presentation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateGameRequest(
 @NotBlank String variantId,
 @NotEmpty @Size(min = 2, max = 4) List<@Valid CreatePlayerRequest> players
) {
}
