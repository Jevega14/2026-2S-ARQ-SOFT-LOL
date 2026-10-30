package com.apuntado.presentation;
import com.apuntado.application.TurnOrchestratorComponent;
/** Adaptador mínimo para que la regla de aislamiento evalúe una clase real. */
public final class CloseController {
 private final TurnOrchestratorComponent application;
 public CloseController(TurnOrchestratorComponent application) {this.application=application;}
}
