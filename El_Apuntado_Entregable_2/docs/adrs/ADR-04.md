# ADR-04 — Motor y modelo de persistencia

## Estado y alcance

Adoptado para esta especificación el 29 de octubre de 2026. Selecciona un motor concreto: MongoDB. El adaptador de almacenamiento es trabajo del MVP; la evidencia adjunta no incluye una instancia de base de datos.

## Contexto

Apuntado requiere historial de rondas y acumulación hasta 101. Un cierre parcial o repetido alteraría el resultado de la partida. Guardar solo en memoria o al final de la ronda perdería movimientos cuando se interrumpa el proceso.

## Decisión

Usar partidas como agregado documental con rondas, manos, movimientos, puntajes y recibos embebidos. Referenciar perfiles de jugadores y sala; conservar snapshots descriptivos.

Persistir cada comando aceptado mediante versión esperada y escritura atómica del documento. La copia en memoria no es la fuente de verdad.

## Alternativas

SQL ofrece claves foráneas y transacciones sobre tablas normalizadas, pero obliga a reconstruir el agregado desde varias relaciones. Archivos JSON locales simplifican un prototipo, aunque requieren resolver bloqueo, escritura segura y recuperación. MongoDB aproxima la unidad almacenada a la unidad de consistencia del juego.

## Consecuencias

Se obtiene recuperación de una partida mediante un documento y se evita repartir el cierre entre colecciones. Aumentan la validación de referencias y el control de tamaño.

Una política de archivo que separe rondas cambiaría la frontera transaccional y necesita revisión. El perfil de reglas queda versionado para interpretar partidas antiguas.

## Cumplimiento verificable

- Validar cada candidato con `game-session.schema.json` y verificar invariantes antes de escribir.
- En integración, ejecutar dos cierres simultáneos desde la misma versión: exactamente uno debe confirmar.
- Repetir un comando no debe duplicar penalizaciones.
- La suma del historial debe coincidir con `totals`.
- Verificar recuperación tras reinicio y rechazo preventivo a 12 MiB.

Estos son criterios pendientes de ejecución en el MVP.

## Revisión

Revisar cuando el tamaño de una partida, los reportes entre partidas o las transacciones entre agregados hagan insuficiente esta unidad de almacenamiento.
