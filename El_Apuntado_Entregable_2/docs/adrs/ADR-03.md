# ADR-03 — Estilo y extensibilidad de reglas

## Estado y alcance

Adoptado para esta especificación el 29 de octubre de 2026. Complementa ADR-01 de particionamiento y ADR-02 de Strategy del Entregable 1; no reemplaza sus versiones originales.

## Contexto

Clásica y Apuntado comparten el ciclo de juego, pero difieren en el cierre. El orquestador debe permanecer estable al incorporar una variante compatible. Un `switch` por modalidad trasladaría reglas al caso de uso y aumentaría su susceptibilidad a cambios.

## Decisión

Adoptar un monolito modular con Strategy. `IReglaJuego` define `id` y `permiteCerrar`. `HandEvaluator` delega en el proveedor activo. `RuleRegistry` usa `ServiceLoader` al iniciar y rechaza proveedores con ID duplicado.

Las clases concretas viven en paquetes separados y comparten solo el contrato, el modelo y algoritmos puros.

## Alternativas

Un microkernel completo facilita una oferta extensa de plug-ins, pero introduce un ciclo de vida y una gobernanza innecesarios para dos variantes. Un registro manual de estrategias es simple, aunque obliga a editar la composición para cada proveedor. Se elige descubrimiento estándar de Java para evitar esa edición.

## Consecuencias

La extensión compatible se compila por separado, se agrega al classpath y requiere reinicio. No se modifica ni recompila el orquestador. No hay recarga en caliente. Los proveedores son código confiable del equipo.

Cambiar la cantidad de cartas o el ciclo de turnos puede exigir una nueva versión del contrato; no se promete que cualquier juego se integre sin cambios.

## Cumplimiento verificable

ArchUnit prohíbe dependencias del orquestador hacia estrategias concretas y dependencias entre estrategias. La prueba de registro carga ambas variantes.

Una comprobación adicional carga un proveedor `DEMO` desde un JAR externo y compara el SHA-256 del orquestador antes y después; la evidencia está en [verification.json](../evidence/verification.json).

## Revisión

Revisar al introducir una variante incompatible con la mano de diez cartas, proveedores externos o necesidad real de carga sin reinicio.
