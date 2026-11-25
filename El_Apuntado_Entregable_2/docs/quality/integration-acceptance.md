# Criterios de aceptación de integración (ADR-04 / ADR-05)

Estado al cierre de la Fase 1 del Entregable 3. Las pruebas `*IT` se ejecutan contra
`mongo:7` real (`APUNTADO_MONGO_IT=true`) y en el job `integration` del CI.

| Criterio | Estado | Evidencia |
| :-- | :-- | :-- |
| Dos cierres desde la misma versión: uno confirma, otro recibe conflicto; sin penalizaciones parciales | ✅ | `GameFlowMongoIT.concurrentClosesFromTwoServerInstances` (dos clientes Mongo), `CommandIdempotencyTest.concurrentClosesLetExactlyOneWin` |
| Reintento con igual commandId y hash: devuelve el resultado; acumulados sin cambio | ✅ | `GameFlowMongoIT.retriesAreIdempotentAndReusedIdsAreRejected`, `CommandIdempotencyTest.retryingACloseDoesNotDuplicateScores` |
| commandId reutilizado con otro hash o autor: rechazar | ✅ | mismas pruebas (`CommandConflictException`, HTTP 409) |
| Fallo antes del commit: no hay confirmación ni modificación parcial | ✅ | `CommandIdempotencyTest.failureBeforeCommitLeavesNoPartialState` |
| Fallo después del commit y antes de publicar: el snapshot recupera el estado | ✅ | `GameApplicationServiceTest.publisherFailureDoesNotRevertTheCommittedCommand` |
| Recuperación tras reinicio del servidor | ✅ | `GameFlowMongoIT.recoversAfterARestartAndFinishesTheGameAt101`; verificación manual con `docker compose restart app` |
| Proyección por jugador: no revelar mano rival ni orden del mazo | ✅ | `GameApplicationServiceTest.theViewerOnlyReceivesItsOwnHand`, `WebSocketRealtimePublisherTest` |
| Totales igualan historial; Clásica tiene una ronda; Apuntado elimina desde 101 | ✅ | `FullGameTest` (73 + 28 = 101 elimina; 72 + 28 = 100 continúa) |
| Límite preventivo de tamaño BSON (12 MiB) | ✅ | `MongoGameRepository.ensureSize` |
| Perfil MVP1 inmutable por partida | ⚠️ Parcial | El perfil está fijado en código; el documento no guarda aún `rulesProfile`/`schemaVersion` del esquema v1 |
