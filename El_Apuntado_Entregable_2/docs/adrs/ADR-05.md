# ADR-05 — Sincronización y comunicación

## Estado y alcance

Adoptado para esta especificación el 29 de octubre de 2026. Decide la comunicación interna. WebSocket queda como un adaptador posible para la comunicación con navegadores, no como el mecanismo de coordinación entre componentes de dominio.

## Contexto

El cliente necesita conocer el resultado de su acción y recibir cambios de turno. Si la evaluación, la puntuación y el almacenamiento se resuelven mediante eventos independientes, el cierre puede quedar repartido en pasos parciales o difíciles de ordenar.

## Decisión

Ejecutar los pasos críticos de forma síncrona mediante contratos. `TurnOrchestrator` coordina la respuesta y persiste antes de notificar. Después de confirmar, `IRealtimePublisher` distribuye una proyección autorizada del estado.

Los eventos de notificación no modifican puntajes ni determinan si una jugada era válida.

## Alternativas

Los eventos locales facilitan añadir observadores, pero introducen orden, manejo de errores y consistencia eventual si se usan para lógica crítica. Las llamadas directas a implementaciones concretas simplifican inicialmente el código, pero acoplan el orquestador. Se eligen llamadas síncronas por interfaces y notificación posterior al commit.

## Consecuencias

El resultado del comando es más fácil de probar y sigue una secuencia explícita. La escritura y la publicación no son atómicas entre sí: puede haber estado confirmado sin aviso recibido.

El cliente conserva la versión y solicita un snapshot al reconectar o detectar saltos; nunca se corrige el marcador aplicando un evento recibido dos veces.

## Cumplimiento verificable

- Una prueba de integración debe simular un fallo del publicador después del commit: el estado debe seguir confirmado y recuperarse.
- No debe publicarse éxito si falla la escritura.
- Los clientes no deben recibir mazos completos ni manos ajenas.
- ArchUnit impide que el dominio importe `java.net`, drivers o infraestructura.

Las pruebas de publicación y privacidad quedan pendientes del adaptador.

## Revisión

Si se exige entrega durable de notificaciones sin depender de reconexión, evaluar un outbox persistido en el mismo agregado. No introducir un broker hasta justificar esa necesidad.
