package com.apuntado.application.port;

/** Puerto de auditoría. Un fallo al auditar nunca debe revertir ni hacer fallar un comando. */
public interface IAuditTrail {
 IAuditTrail NONE = entry -> { };

 void record(AuditEntry entry);
}
