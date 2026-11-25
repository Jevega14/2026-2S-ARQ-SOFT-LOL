package com.apuntado.application.port;

import java.util.Objects;

/**
 * Recibo de un comando aceptado (ADR-04, esquema {@code CommandReceipt}).
 * {@code payloadHash} identifica acción, autor y argumentos: un reintento con el mismo {@code commandId}
 * solo es válido si coincide; cualquier otro contenido es una reutilización indebida del identificador.
 */
public record CommandReceipt(String commandId, String actorId, String payloadHash, int resultVersion) {
 public CommandReceipt {
  Objects.requireNonNull(commandId, "commandId");
  Objects.requireNonNull(actorId, "actorId");
  Objects.requireNonNull(payloadHash, "payloadHash");
  if (resultVersion < 0) throw new IllegalArgumentException("Versión de resultado inválida");
 }

 public boolean sameCommandAs(CommandReceipt other) {
  return commandId.equals(other.commandId) && actorId.equals(other.actorId) && payloadHash.equals(other.payloadHash);
 }
}
