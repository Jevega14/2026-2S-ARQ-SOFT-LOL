package com.apuntado.audit;

import com.apuntado.application.port.AuditEntry;
import com.apuntado.application.port.IAuditTrail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adaptador de auditoría: escribe una línea JSON por comando en el logger {@code AUDIT}
 * (archivo {@code logs/audit.jsonl}, ver {@code logback-spring.xml}). Es la evidencia de las
 * sesiones con usuarios y la fuente de las métricas de cierre, errores y latencia.
 */
public final class JsonLinesAuditTrail implements IAuditTrail {
 private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");

 @Override
 public void record(AuditEntry entry) {
  AUDIT.info(toJson(entry));
 }

 static String toJson(AuditEntry entry) {
  return new StringBuilder(256).append('{')
   .append(field("at", String.valueOf(entry.at()))).append(',')
   .append(field("gameId", entry.gameId())).append(',')
   .append(field("variantId", entry.variantId())).append(',')
   .append(field("playerId", entry.playerId())).append(',')
   .append(field("action", entry.action())).append(',')
   .append(field("commandId", entry.commandId())).append(',')
   .append(field("outcome", entry.outcome())).append(',')
   .append("\"version\":").append(entry.version()).append(',')
   .append("\"round\":").append(entry.round()).append(',')
   .append("\"turn\":").append(entry.turn()).append(',')
   .append("\"durationMicros\":").append(entry.durationMicros()).append(',')
   .append(field("detail", entry.detail()))
   .append('}').toString();
 }

 private static String field(String name, String value) {
  return '"' + name + "\":" + (value == null ? "null" : '"' + escape(value) + '"');
 }

 private static String escape(String value) {
  StringBuilder escaped = new StringBuilder(value.length());
  for (char character : value.toCharArray()) {
   if (character == '"' || character == '\\') escaped.append('\\').append(character);
   else if (character < 0x20) escaped.append(String.format("\\u%04x", (int) character));
   else escaped.append(character);
  }
  return escaped.toString();
 }
}
