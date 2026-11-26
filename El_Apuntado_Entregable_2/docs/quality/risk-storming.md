# Registro de Risk Storming

Matriz 3x3 de probabilidad (1 poco plausible, 2 plausible, 3 alta) por impacto (1 presentación,
2 operación o mantenimiento, 3 resultado, recuperación o privacidad). La valoración inicial viene del
Entregable 2; el riesgo residual refleja las mitigaciones implementadas y verificadas en el
Entregable 3 (sección 5.1 del informe).

Participantes: Jessica Paola Vega Alvarado, Andrés Felipe Villa Arias y Oscar Daniel Ruiz Mejía.

## Identificación y valoración

| Riesgo | Descripción | P × I (E2) | Valor E2 | P × I residual | Valor residual |
|---|---|---|---|---|---|
| R1 | Cierre aceptado o rechazado de forma incorrecta por el motor de reglas | 3 × 3 | 9 | 1 × 3 | 3 |
| R2 | Cierre doble: dos jugadores cierran la misma ronda a la vez | 2 × 3 | 6 | 1 × 3 | 3 |
| R3 | Pérdida del estado de la partida ante un reinicio o fallo | 2 × 3 | 6 | 1 × 3 | 3 |
| R4 | Fuga de cartas privadas (manos rivales u orden del mazo) | 2 × 3 | 6 | 1 × 3 | 3 |
| R5 | Orquestador de turnos que concentra lógica de las variantes | 2 × 2 | 4 | 1 × 2 | 2 |
| R6 | Crecimiento del documento agregado por rondas y recibos | 2 × 2 | 4 | 1 × 2 | 2 |
| R7 | Divergencia entre las reglas documentadas y el código | 3 × 2 | 6 | 1 × 2 | 2 |
| R8 | Evaluación lenta de la mano por la búsqueda combinatoria | 2 × 2 | 4 | 1 × 2 | 2 |
| R9 | Suplantación de jugador: sin autenticación, el enlace `?player=` basta (identificado en el E3) | 2 × 3 | 6 | 2 × 3 | 6 (aceptado) |

## Mitigación y seguimiento

| Riesgo | Acción | Responsable | Evidencia | Seguimiento |
|---|---|---|---|---|
| R1 | Estrategias separadas, grupos disjuntos y casos borde parametrizados | Oscar | `CombinationEdgeCasesTest`, `FullGameTest` | Cada cambio de regla agrega casos |
| R2 | Versión esperada y recibo de comando en la misma escritura | Jessica | `GameFlowMongoIT.concurrentClosesFromTwoServerInstances` | Job `integration` en cada push |
| R3 | Commit antes de responder y recuperación del snapshot | Jessica | `GameFlowMongoIT.recoversAfterARestartAndFinishesTheGameAt101`; `docker compose restart app` | Job `integration` |
| R4 | `GameView` por jugador en REST y WebSocket | Andrés | `theViewerOnlyReceivesItsOwnHand`, `sendsAnAuthorizedProjectionToEachViewer` | Revisar en cada cambio de vista |
| R5 | ArchUnit y complejidad ciclomática ≤ 10 | Oscar | `orchestratorDoesNotKnowStrategies`, Checkstyle | Ce del orquestador en `architecture-metrics.md` |
| R6 | Límite preventivo de 12 MiB por documento | Jessica | `MongoGameRepository.ensureSize` | Medir el tamaño en las sesiones con usuarios |
| R7 | Perfil de reglas MVP1 en README y pruebas; deriva corregida | Andrés | Tabla de deriva de la sección 5.1 del informe | Contrastar ADR y código en cada entrega |
| R8 | Benchmark del evaluador en el CI | Oscar | `EvaluatorBenchmarkTest`: p99 < 0,11 ms | Umbral p99 < 2 ms |
| R9 | Aceptado en el MVP (juego en mesa presencial) | Andrés | — | Autenticación OIDC antes de publicar en Internet (sección 5.3) |
