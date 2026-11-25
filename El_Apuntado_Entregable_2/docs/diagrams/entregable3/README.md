# Diagramas UML — Entregable 3 (Sección 1 del documento)

Fuentes PlantUML (`.puml`) versionadas junto a su render en PNG (para el documento) y SVG (editable/escalable).
Todos reflejan el código de la rama `entregable3/fase1-mvp`; no hay componentes "propuestos" en ellos.

Regenerar (requiere Docker):

```bash
cd docs/diagrams/entregable3
docker run --rm -v "$PWD":/data -w /data plantuml/plantuml -tpng *.puml
docker run --rm -v "$PWD":/data -w /data plantuml/plantuml -tsvg *.puml
```

| Archivo | Sección de la guía | Qué muestra |
| :-- | :-- | :-- |
| `11-despliegue` | 1.1 Despliegue | Nodos físicos, contenedores, artefactos y protocolos |
| `12-secuencia-cierre` | 1.2 Secuencia | Intento de cierre y liquidación, con `alt` Clásica / Apuntado |
| `13a-clases-dominio` | 1.3 Clases (1/2) | Dominio, motor de reglas Strategy + ServiceLoader, evaluación combinatoria |
| `13b-clases-aplicacion` | 1.3 Clases (2/2) | Casos de uso, puertos y adaptadores (persistencia, tiempo real, auditoría, config) |

## 1.1 Diagrama de despliegue

- **Nodo dispositivo del jugador:** navegador con el bundle estático `index.html` + `app.js` + `styles.css`
  (servido por la misma app; no hay build de frontend).
- **Nodo equipo servidor (host Docker):** red bridge de Docker Compose con dos contenedores:
  - `apuntado-app` (`el-apuntado:latest`, JRE 17): JVM con Tomcat embebido en **:8080** que ejecuta
    `arquitectura-entregable2-1.0.0.jar` (Spring Boot 3.3.5). Dentro, los módulos del monolito modular se
    comunican por **IPC en memoria** (llamadas síncronas a interfaces).
  - `apuntado-mongo` (`mongo:7`) en **:27017**, colección `game_sessions`, volumen `mongo-data`.
  - Archivo `./logs/audit.jsonl` montado desde el host (evidencia de las pruebas con usuarios).
- **Protocolos:** HTTP/1.1 REST + JSON y **WebSocket** (`/ws/games`) entre navegador y app; **MongoDB Wire
  Protocol sobre TCP** entre app y base de datos.
- **GitHub Actions** construye la misma imagen y ejecuta el smoke test (job `release`).
- Nota: perfil por defecto sin MongoDB (`InMemoryGameRepository`) y modos de baraja `random` / `demo`.

## 1.2 Diagrama de secuencia — cierre de ronda y liquidación

Correspondencia con los participantes que exige la guía:

| Guía | Implementación |
| :-- | :-- |
| Jugador (UI) | `Jugador` + `UI (app.js)` |
| GameOrchestrator | `GameController` → `GameApplicationService` → `TurnOrchestratorComponent` |
| HandEvaluator (plug-in de regla) | `IReglaJuego` (`ReglaClasica` / `ReglaApuntado`, cargadas por `RuleRegistry`) invocado desde `RoundClosingService`; `Groups.matches` hace la búsqueda combinatoria |
| ScoringService | `RoundScoringComponent` (`penalty`, `accumulate`) y `PlayerScore.eliminated()` (≥ 101) |
| Repository / DB | `IGameRepository` (`MongoGameRepository`) → MongoDB |

Fragmentos combinados:

- `alt` **Escenario 1 — Clásica:** `Groups.matches(cartas, {4,3,3}, 0)` → liquidación → `terminaPartidaAlCerrar() = true`
  → `FINISHED`, gana quien cierra (ronda ganada = partida ganada).
- `alt` **Escenario 2 — Apuntado:** `Groups.matches(cartas, {3,3,3}, 4)` (sobrante < 5) → `loop` de penalizaciones
  a los demás → evaluación de eliminados (≥ 101) → `alt` fin de partida / nueva ronda.
- `alt` **Mano inválida:** se rechaza con mensaje explicativo (HTTP 400) y se audita como `REJECTED`.
- `alt` **Concurrencia:** `updateFirst` filtrado por versión; si otro cierre ganó, `OptimisticLockException`
  → reintento idempotente (`REPLAY`) o 409, sin liquidación doble (riesgo R2).
- La notificación WebSocket y la auditoría ocurren **después** del commit (ADR-05).

## 1.3 Diagramas de clases de implementación

- Visibilidad UML explícita (`+` público, `-` privado, `~` paquete; `#` no aparece porque no hay herencia de
  implementación), tipos exactos de Java y estereotipos (`record`, `final`, `utility`, `port`, anotaciones Spring).
- **Patrón para desacoplar reglas:** Strategy (`IReglaJuego`) + descubrimiento tipo microkernel con
  `ServiceLoader` (`RuleRegistry`). Las estrategias solo comparten `Groups` (algoritmo puro).
- **Métodos de evaluación combinatoria:** `Groups.matches/assign/combinations/collect/valid/sameRank/run`.
- **Hexagonal dentro del monolito:** `application.port` define `IGameRepository`, `IRealtimePublisher` e
  `IAuditTrail`; los adaptadores viven en `persistence`, `realtime` y `audit`; solo `config` conoce las
  implementaciones concretas.

### Implementado vs. pendiente (retroalimentación E2, punto 2.4)

Todo lo dibujado está implementado y probado. Elementos del diseño objetivo de la Entrega 2 que **no** están
en el código (se discuten en la retrospectiva como desviación consciente):

- `rulesProfile` y `schemaVersion` dentro del documento de partida.
- Colecciones `jugadores` y `salas` (room), y el historial `moves` por ronda (la auditoría cumple ese rol).
- `deadline` del turno y pausa por desconexión (hoy se recupera recargando la página).
- Las entidades de persistencia propuestas en E2 (`GameSessionEntity`, `RoundEntity`…) se reemplazaron por
  `GameSessionMapper` sobre `org.bson.Document`, sin anotaciones en el dominio.
