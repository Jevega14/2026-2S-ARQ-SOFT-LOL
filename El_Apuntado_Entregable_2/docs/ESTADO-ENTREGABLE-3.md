# Estado del Entregable 3 — resumen de trabajo

Última actualización: 2026-11-25. Rama de trabajo: `entregable3/fase1-mvp` (Fase 1 en `2135917`, ya en GitHub; CI en verde: https://github.com/Jevega14/2026-2S-ARQ-SOFT-LOL/actions/runs/45623300026).

## Contexto

- Guía: "Guía de Especificación del Proyecto Entregable 3" (Fundamentos de Arquitectura, Richards & Ford).
  PDF máx. 30 páginas con 5 secciones: (1) UML despliegue/secuencia/clases, (2) MVP y checklists,
  (3) validación con 3–5 usuarios reales, (4) CI/CD + métricas, (5) retrospectiva y leyes de la arquitectura.
- Rúbrica: MVP 30 %, usuarios 20 %, UML 15 %, CI/calidad 15 %, retrospectiva 15 %, checklists/presentación 5 %.
- Nota Entrega 2: 4,67/5. Lo más bajo: Risk Storming (90) → faltan responsables, seguimiento y R1–R8 en la matriz 3x3.
- El PDF "Entrega 1 – AWS" es de otra materia (NexaCloud); no aplica.

## Fase 1 — HECHA (código, commit 2135917)

Hallazgos corregidos frente a la Entrega 2:

1. CI no corría en GitHub (estaba en subcarpeta) → movido a `/.github/workflows/quality.yml`
   con jobs `verify`, `integration` (Mongo real) y `release` (docker compose + smoke test).
2. Clásica jugaba varias rondas → ahora termina en el primer cierre 4-3-3 (métodos `default`
   en `IReglaJuego`: `terminaPartidaAlCerrar`, `descripcionCierre`).
3. Mazo agotado bloqueaba el juego → se rebaraja el pozo conservando la última carta.
4. `commandId` reutilizado no se detectaba → recibos con autor + hash + versión; 409 si se reutiliza.
5. Sin logs de auditoría → `logs/audit.jsonl` (una línea JSON por comando, con latencia y motivo de rechazo).
6. Sin despliegue en un comando → `Dockerfile` + `docker compose up --build` (app + Mongo).

UI: pantalla de relevo entre turnos, enlaces por jugador (multi-dispositivo), ordenar mano,
carta robada resaltada, regla de cierre visible, recuperación al recargar, etiquetas accesibles.
Modo demo para la sustentación: `APUNTADO_DECK=demo docker compose up --build`.

Verificado:
- `./mvnw clean verify`: 118/118 pruebas, 0 violaciones Checkstyle, cobertura 87,4 % líneas / 69,9 % ramas.
- Integración con MongoDB real: 6/6 (cierres simultáneos desde 2 instancias, reintentos, recuperación, partida a 101).
- Benchmark evaluador: p99 < 0,13 ms. ArchUnit: 16 reglas.
- Navegador + Docker: Clásica y Apuntado completas, cierre inválido rechazado, recuperación tras reinicio.
- Evidencia: `docs/evidence/entregable3/`.

Pendiente técnico conocido (para la retrospectiva): el documento Mongo no guarda `rulesProfile`/`schemaVersion` del esquema v1.

## Push y CI — HECHO (historial)

El push falló (403): Git en este Mac usa la cuenta corporativa `jpvega_cib`, sin permiso sobre
`Jevega14/2026-2S-ARQ-SOFT-LOL`. Opciones:
- Hacer push desde un equipo/terminal con la cuenta `Jevega14`, o
- Configurar en este repo credenciales de `Jevega14` (token personal) y ejecutar:
  `git push -u origin entregable3/fase1-mvp`
Luego revisar la pestaña Actions en GitHub y **tomar captura del pipeline en verde** (sección 4.1).
Después, fusionar la rama en `main` (o abrir PR).

## Fases

Pendiente: tomar captura del pipeline en verde (sección 4.1), fusionar a `main`, revocar el primer token expuesto en el chat.

**Fase 2 – UML — HECHA:** `docs/diagrams/entregable3/` (11-despliegue, 12-secuencia-cierre, 13a/13b clases; .puml + PNG + SVG, explicados en su README). Contenido:
- Despliegue: navegador → contenedor Spring Boot `.jar` (HTTP/REST 8080, WebSocket `/ws/games`) → MongoDB 27017.
- Secuencia del cierre: Jugador(UI) → GameController → GameApplicationService → TurnOrchestrator →
  RoundClosingService/IReglaJuego → RoundScoringComponent → IGameRepository/Mongo, con `alt` Clásica
  (4-3-3 → gana) / Apuntado (3-3-3 + sobrante < 5 → penalizaciones → ¿≥101? → fin o nueva ronda).
- Clases de implementación fieles al código (visibilidad, tipos, Strategy), marcando implementado vs pendiente.

**Fase 3 – Validación con usuarios — KIT HECHO (`docs/validacion/`, `scripts/validacion_metricas.py`); FALTA que el EQUIPO ejecute las sesiones:**
- Claude: guion de tareas, cuestionario SUS, plantilla de tabla de métricas, script que calcula métricas desde `audit.jsonl`, carpeta `docs/validacion/`.
- Equipo: sesiones con 3–5 personas ajenas al equipo en modo `random`; fotos/capturas/video, encuestas, guardar `logs/audit.jsonl`. **No inventar datos.**
- Al menos 2 mejoras post-validación (implementarlas y documentarlas).

**Fase 4 – Documento (Claude):** DOCX + PDF ≤ 30 páginas siguiendo el índice de la guía; checklists
(code-completion, testing, release), métricas reales, Risk Storming actualizado (responsables y seguimiento),
retrospectiva con las 3 leyes y escenario de 100.000 jugadores. Sección 3 con espacios para los resultados reales.

**Pendiente de decidir:** ¿despliegue en vivo (Render/Railway con cuenta del equipo) o basta `docker compose up`?

## Notas del entorno (este Mac)

- No hay Java instalado; los builds se corren en Docker con `~/.cache/e3/scripts/verify.sh clean verify`.
- Hay proxy corporativo con inspección TLS: los contenedores necesitan la CA en `~/.cache/e3/certs`.
  El `Dockerfile` del repo es estándar; en GitHub y en otros equipos no hace falta nada de esto.
