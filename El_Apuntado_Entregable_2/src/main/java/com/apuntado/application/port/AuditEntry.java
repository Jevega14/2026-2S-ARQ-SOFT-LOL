package com.apuntado.application.port;

import java.time.Instant;

/**
 * Registro de auditoría de un comando: permite reconstruir la sesión de prueba con usuarios
 * (tasa de cierres, errores de regla y latencia del servidor por acción).
 *
 * @param outcome {@code OK}, {@code REPLAY} (reintento idempotente) o {@code REJECTED}
 */
public record AuditEntry(Instant at, String gameId, String variantId, String playerId, String action,
 String commandId, String outcome, int version, int round, int turn, long durationMicros, String detail) {
}
