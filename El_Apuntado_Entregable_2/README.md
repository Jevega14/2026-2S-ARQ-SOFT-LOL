# El Apuntado — MVP del Entregable 3

MVP web jugable de El Apuntado sobre un monolito modular (Spring Boot + MongoDB).
Implementa las dos modalidades —**Clásica 4-3-3** (una sola ronda) y **Apuntado a
101** (rondas con acumulación y eliminación)—, el flujo completo de turnos, la
liquidación de rondas, idempotencia por comando, control de concurrencia
optimista (ADR-04) y un registro de auditoría por comando.

Java 17. Las dependencias están fijadas en `pom.xml` y el repositorio incluye
Maven Wrapper (Maven 3.9.16).

## Ejecución en un comando (recomendado)

Requiere Docker con Docker Compose.

```bash
docker compose up --build
```

Abre <http://localhost:8080>. Se levantan dos contenedores:

| Servicio | Imagen | Puerto | Rol |
| :-- | :-- | :-- | :-- |
| `app` | `el-apuntado:latest` (JRE 17 + `.jar`) | 8080 (HTTP + WebSocket) | API REST, interfaz web y `/ws/games` |
| `mongo` | `mongo:7` | 27017 | Fuente de verdad, volumen `mongo-data` |

El registro de auditoría queda en `./logs/audit.jsonl`. Para detener: `docker compose down`
(agrega `-v` para borrar también las partidas).

### Modo demostración (sustentación)

```bash
APUNTADO_DECK=demo docker compose up --build
```

Con `apuntado.deck=demo` el primer asiento recibe A♥ A♦ A♠ A♣ 9♥ 9♦ 9♠ J♥ J♦ J♠ en
cada ronda: es 4-3-3 en Clásica y 3-3-3 + A (1 punto) en Apuntado. Basta robar,
descartar la carta robada e «Intentar cierre». En Apuntado, dos o tres rondas
bastan para que el rival supere 101. **Las pruebas con usuarios se hacen en modo
`random` (valor por defecto).**

## Ejecución sin Docker

```bash
./mvnw clean verify          # pruebas, fitness functions, Checkstyle y JaCoCo
./mvnw spring-boot:run       # perfil por defecto: repositorio en memoria
```

Con MongoDB local:

```bash
docker compose up -d mongo
SPRING_PROFILES_ACTIVE=mongo ./mvnw spring-boot:run
```

## Cómo se juega

1. Elige modalidad y de 2 a 4 jugadores. La pantalla muestra la condición de cierre.
2. **Un dispositivo (modo relevo):** cuando cambia el turno aparece «Pasa el
   dispositivo a X»; la mano solo se muestra al pulsar «Ver mi mano».
3. **Varios dispositivos:** en «Jugar desde varios dispositivos» hay un enlace por
   jugador (`/?game={id}&player={playerId}`). En red local reemplaza `localhost`
   por la IP del equipo servidor. Cada navegador ve solo su mano y se actualiza
   por WebSocket.
4. En tu turno: roba del mazo o del pozo, ordena la mano (por palo o número; la
   carta recién robada se resalta), descarta y luego «Terminar turno» o «Intentar
   cierre». Un cierre inválido se rechaza explicando la regla.
5. Recargar la página (`/?game={id}`) recupera la partida desde el servidor.

## Reglas implementadas (perfil MVP1)

- Baraja francesa de 52 cartas sin comodines; As = 1, 2–10 nominal, J/Q/K = 10.
- 10 cartas por jugador; una carta inicia el pozo.
- Grupos: mismo número con palos distintos, o escalera del mismo palo (As solo
  bajo, sin escaleras circulares).
- **Clásica:** tras descartar, la mano debe formar 4-3-3 sin sobrantes. El primer
  cierre válido termina la partida.
- **Apuntado:** tras descartar, tres ternas y una carta sobrante de **menos de 5**
  puntos. Quien cierra suma 0; los demás suman todas sus cartas. Con **101 o más**
  un jugador queda eliminado; gana el último en pie.
- Si el mazo se agota, el pozo se baraja de nuevo conservando la última carta.

## API REST

| Método | Ruta | Descripción |
| :-- | :-- | :-- |
| GET | `/api/variants` | Modalidades registradas (`id`, `closeRequirement`, `singleRound`). |
| POST | `/api/games` | Crea una partida (`variantId`, `players`). |
| GET | `/api/games/{gameId}?viewerId=` | Proyección autorizada para un jugador. |
| POST | `/api/games/{gameId}/draw-deck` | Roba del mazo (`playerId`, `commandId`). |
| POST | `/api/games/{gameId}/draw-discard` | Roba del pozo. |
| POST | `/api/games/{gameId}/discard` | Descarta (`playerId`, `cardId`, `commandId`). |
| POST | `/api/games/{gameId}/end-turn` | Termina el turno. |
| POST | `/api/games/{gameId}/close` | Intenta cerrar la ronda. |

Códigos: `400` regla o datos inválidos (incluye cierre inválido), `404` partida
inexistente, `409` movimiento fuera de turno, versión obsoleta o `commandId`
reutilizado con otro contenido.

Cada mutación exige un `commandId`. Se guarda un recibo con autor y hash del
contenido en la misma escritura que el cambio: repetir el mismo comando devuelve
el estado sin duplicar efectos; reutilizar el identificador para otra acción o
autor se rechaza (ADR-04).

## Tiempo real (WebSocket)

```text
ws://localhost:8080/ws/games?gameId={gameId}&playerId={playerId}
```

Tras confirmar cada comando, `GameApplicationService` publica una proyección por
jugador (ADR-05): cada conexión recibe solo su mano, sin manos rivales ni el orden
del mazo. Si la publicación falla, el comando ya quedó confirmado y el cliente
recupera el estado con `GET /api/games/{gameId}`.

## Auditoría

Cada comando (aceptado, repetido o rechazado) se registra como una línea JSON en
`logs/audit.jsonl` (`APUNTADO_AUDIT_DIR` cambia la carpeta):

```json
{"at":"2026-11-25T03:20:11Z","gameId":"…","variantId":"APUNTADO101","playerId":"p2","action":"CLOSE","commandId":"…","outcome":"REJECTED","version":5,"round":2,"turn":1,"durationMicros":6721,"detail":"IllegalArgumentException: Cierre inválido: se requiere tres ternas y una carta sobrante de menos de 5 puntos"}
```

Es la evidencia de las sesiones con usuarios: permite calcular la tasa de cierres
válidos, los errores de regla y la latencia del servidor por acción.

## Arquitectura

- `domain.model`: entidades y agregado `GameSession` (invariantes de cartas, turnos y puntajes).
- `domain.gameplay`: `HandEvaluatorComponent` y `RoundClosingService` (cierre y liquidación).
- `domain.rules.api` / `rules.clasica` / `rules.apuntado` / `rules.common`: Strategy aislado.
- `domain.scoring`: penalizaciones, acumulación y umbral de 101.
- `application`: casos de uso, comandos, vistas y puertos (`IGameRepository`,
  `IRealtimePublisher`, `IAuditTrail`). No depende de Spring ni de infraestructura.
- `persistence`: `InMemoryGameRepository` (por defecto) y `persistence.mongo` (perfil `mongo`).
- `presentation`: controladores REST, DTOs y manejo de errores.
- `realtime`: WebSocket de notificación por jugador.
- `audit`: adaptador JSON Lines del puerto de auditoría.
- `config`: composition root; única capa que conoce las implementaciones.
- `bootstrap`: registro de variantes por `ServiceLoader`.

Una nueva variante implementa `IReglaJuego` (los métodos `terminaPartidaAlCerrar`
y `descripcionCierre` tienen implementación por defecto) y se declara en
`META-INF/services/com.apuntado.domain.rules.api.IReglaJuego` dentro de su JAR.
No requiere cambios en `TurnOrchestratorComponent`.

## Pruebas y fitness functions

`./mvnw clean verify` ejecuta **118 pruebas** y falla si se rompe cualquiera de estas reglas:

- **ArchUnit (16 reglas):** aislamiento entre variantes, UI aislada del
  almacenamiento, ausencia de ciclos, dominio puro, orquestador sin estrategias
  concretas, aplicación sin frameworks ni adaptadores, presentación que solo habla
  con la fachada de aplicación (Ley de Deméter entre capas), entre otras.
- **Checkstyle:** complejidad ciclomática máxima de 10 por método.
- **JaCoCo:** al menos 80 % de líneas y 60 % de ramas.
- **Métricas:** `ArchitectureMetricsTest` escribe Ca, Ce, I, A y D por módulo y
  paquete en `target/architecture-metrics.md`.
- **Benchmark (R8):** `EvaluatorBenchmarkTest` mide p50/p95/p99 del evaluador en
  `target/evaluator-benchmark.md`.
- **Casos borde:** sobrante de exactamente 5, ternas incompletas, escaleras con
  As bajo, detección de 100 frente a 101 exactos, partidas completas,
  rebarajado del pozo, reintentos, cierres concurrentes y fallo antes del commit.

Pruebas de integración con MongoDB real (6 pruebas `*IT`: cierres simultáneos
desde dos instancias, reintentos, recuperación tras reinicio y partida hasta 101):

```bash
docker compose up -d mongo
APUNTADO_MONGO_IT=true ./mvnw --batch-mode "-Dtest=*IT" -Dsurefire.failIfNoSpecifiedTests=false test
```

## CI/CD

El workflow está en la raíz del repositorio:
[`.github/workflows/quality.yml`](../.github/workflows/quality.yml) (GitHub solo
ejecuta workflows ubicados ahí). Corre en cada `push` y `pull_request`:

1. `verify`: build, 118 pruebas, ArchUnit, Checkstyle y JaCoCo; publica
   `quality-reports` y escribe métricas y benchmark en el resumen del job.
2. `integration`: `mongo:7` como service container y las pruebas `*IT`.
3. `release`: construye la imagen con `docker compose`, levanta el stack y hace
   un smoke test de la API.

## Informe del Entregable 3

[`docs/informe/`](docs/informe/) contiene el documento consolidado (DOCX editable y PDF).

## Validación con usuarios

El kit está en [`docs/validacion/`](docs/validacion/README.md): protocolo, consentimiento, hoja de
observación, encuesta SUS, plantillas CSV y carpeta de evidencias. `scripts/validacion_metricas.py`
convierte `logs/audit.jsonl` + encuestas en la tabla consolidada de la sección 3.3.

## Esquemas

`schemas/` usa JSON Schema 2020-12 para validación de aplicación; no copiarlo
directamente al validador `$jsonSchema` de MongoDB.
