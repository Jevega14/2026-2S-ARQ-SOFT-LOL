package com.apuntado.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.apuntado.application.port.AuditEntry;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class JsonLinesAuditTrailTest {
 @Test
 void writesOneEscapedJsonLinePerEntry() {
  AuditEntry entry = new AuditEntry(Instant.parse("2026-11-24T20:00:00Z"), "g-1", "CLASICA", "p1", "CLOSE", "c-1",
   "REJECTED", 4, 1, 3, 1250, "IllegalArgumentException: \"Cierre inválido\"\nlínea");
  String json = JsonLinesAuditTrail.toJson(entry);
  assertTrue(json.startsWith("{\"at\":\"2026-11-24T20:00:00Z\""));
  assertTrue(json.contains("\"outcome\":\"REJECTED\""));
  assertTrue(json.contains("\"durationMicros\":1250"));
  assertTrue(json.contains("\\\"Cierre inválido\\\"\\u000alínea"));
  assertEquals(-1, json.indexOf('\n'));
  new JsonLinesAuditTrail().record(entry);
 }

 @Test
 void writesNullForMissingContext() {
  AuditEntry entry = new AuditEntry(Instant.EPOCH, "g-1", null, "p1", "DRAW_DECK", "c-1", "REJECTED", -1, 0, 0, 5, null);
  assertTrue(JsonLinesAuditTrail.toJson(entry).contains("\"variantId\":null"));
 }
}
