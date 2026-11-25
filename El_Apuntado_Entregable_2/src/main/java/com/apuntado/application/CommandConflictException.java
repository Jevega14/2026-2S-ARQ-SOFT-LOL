package com.apuntado.application;

/** Se reutilizó un {@code commandId} con otro autor o contenido (ADR-04). */
public class CommandConflictException extends RuntimeException {
 public CommandConflictException(String commandId) {
  super("El commandId ya se usó para otro comando: " + commandId);
 }
}
