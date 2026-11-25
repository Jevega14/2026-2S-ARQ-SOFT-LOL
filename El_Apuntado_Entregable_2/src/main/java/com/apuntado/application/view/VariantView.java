package com.apuntado.application.view;

/** Modalidad registrada: identificador, condición de cierre y si se juega a una sola ronda. */
public record VariantView(String id, String closeRequirement, boolean singleRound) {
}
