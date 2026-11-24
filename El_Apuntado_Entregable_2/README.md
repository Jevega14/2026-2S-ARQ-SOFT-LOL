# El Apuntado — MVP del Entregable 3

MVP web jugable de El Apuntado sobre un monolito modular. Implementa las dos
modalidades (Clásica 4-3-3 y Apuntado a 101), el flujo completo de turnos y la
liquidación de rondas. La persistencia se elige por perfil: repositorio en
memoria por defecto y adaptador MongoDB con control de concurrencia optimista e
idempotencia por comando (ADR-04).

Java 17 mínimo. Las dependencias están fijadas en `pom.xml` y el repositorio
incluye Maven Wrapper con Maven 3.9.16.

## Ejecución local

```bash
./mvnw clean verify
./mvnw spring-boot:run
```

Luego abrir <http://localhost:8080>. También se puede empaquetar y ejecutar:

```bash
./mvnw clean package
java -jar target/arquitectura-entregable2-1.0.0.jar
```

En este entorno de desarrollo el JDK, Maven y el repositorio local se guardan en
`.tooling/` (ignorado por Git). Para compilar sin red:

```bash
JAVA_HOME="$PWD/.tooling/jdk-17.0.21.1+2" \
./mvnw --offline --batch-mode -Dmaven.repo.local=.tooling/m2 clean verify
```

## Funcionalidad del MVP

- Creación de partidas con 2 a 4 jugadores y selección de modalidad.
- Reparto de 10 cartas, pozo inicial y mazo estándar de 52 cartas.
- Robo desde mazo o pozo, descarte y rotación de turnos.
- Cierre Clásica 4-3-3 y cierre Apuntado 3-3-3 con sobrante menor que 5.
- Liquidación: quien cierra suma 0 y cada rival suma todas sus cartas.
- Acumulación, eliminación desde 101 puntos y ganador único.
- Interfaz web "hot-seat" que muestra únicamente la mano del jugador activo.
- Descubrimiento de variantes con `ServiceLoader` (`IReglaJuego` + `RuleRegistry`).

Perfil adoptado: baraja francesa de 52 cartas sin comodines, as = 1, figuras = 10,
de 2 a 4 participantes. Apuestas fuera del MVP.

## API REST

| Método | Ruta | Descripción |
| :-- | :-- | :-- |
| GET | `/api/variants` | Modalidades registradas por `ServiceLoader`. |
| POST | `/api/games` | Crea una partida (`variantId`, `players`). |
| GET | `/api/games/{gameId}?viewerId=` | Proyección autorizada para un jugador. |
| POST | `/api/games/{gameId}/draw-deck` | Roba del mazo (`playerId`, `commandId`). |
| POST | `/api/games/{gameId}/draw-discard` | Roba del pozo. |
| POST | `/api/games/{gameId}/discard` | Descarta (`playerId`, `cardId`, `commandId`). |
| POST | `/api/games/{gameId}/end-turn` | Termina el turno. |
| POST | `/api/games/{gameId}/close` | Intenta cerrar la ronda. |

Cada mutación exige un `commandId` único: repetir el mismo comando devuelve el
estado ya aplicado sin duplicar efectos (ADR-04). La versión de la partida
(`version`) permite rechazar escrituras obsoletas con HTTP 409.

## Tiempo real (WebSocket)

Tras persistir cada comando, `GameApplicationService` publica una proyección
autorizada por jugador mediante `IRealtimePublisher` (ADR-05). El adaptador
WebSocket escucha en:

```text
ws://localhost:8080/ws/games?gameId={gameId}&playerId={playerId}
```

Cada conexión recibe solo el estado de su jugador, con su propia mano y sin el
mazo completo ni las manos rivales:

```json
{"type":"CARD_DRAWN","state":{"gameId":"...","version":1,"viewerId":"p1",...}}
```

La notificación ocurre **después** del commit. Si el publicador falla, el
comando ya quedó confirmado y la respuesta HTTP sigue siendo válida; el cliente
puede recuperar el estado con `GET /api/games/{gameId}`. La UI usa el evento como
señal y solicita el snapshot REST, reconectando si cambia el jugador en turno.

## Persistencia

Por defecto se usa `InMemoryGameRepository`, que no requiere servicios externos.
Para usar MongoDB:

```bash
docker compose up -d mongo
SPRING_PROFILES_ACTIVE=mongo ./mvnw spring-boot:run
```

El perfil `mongo` activa `MongoGameRepository`, que guarda cada partida como un
documento con rondas, manos y puntajes embebidos, aplica escrituras atómicas
filtradas por versión, registra los `commandId` dentro del agregado y rechaza
documentos mayores a 12 MiB. La configuración de conexión está en
`src/main/resources/application-mongo.properties`.

Pruebas de integración reales (requieren MongoDB y Docker habilitado en WSL):

```bash
docker compose up -d mongo
APUNTADO_MONGO_IT=true ./mvnw --batch-mode test
```

Estas pruebas cubren recuperación desde otra instancia, recibos de comando y dos
cierres concurrentes desde la misma versión (exactamente uno debe ganar).

## Arquitectura

- `domain.model`: entidades y agregado `GameSession`.
- `domain.gameplay`: evaluación de manos y cierre de ronda.
- `domain.rules.api` / `rules.clasica` / `rules.apuntado`: Strategy aislado.
- `domain.scoring`: penalizaciones, acumulación y umbral de 101.
- `application`: casos de uso (`GameApplicationService`), comandos, vistas y el
  puerto `IGameRepository`. No depende de Spring ni de infraestructura.
- `persistence`: `InMemoryGameRepository` (perfil por defecto) y
  `persistence.mongo.MongoGameRepository` con su mapeador BSON (perfil `mongo`).
- `presentation`: controladores REST, DTOs y manejo de errores.
- `realtime`: WebSocket de notificación (`/ws/games`), registro de sesiones y
  publicación de proyecciones por jugador.
- `config`: composition root de Spring; única capa que conoce las implementaciones.
- `bootstrap`: registro de variantes por `ServiceLoader`.

Las restricciones se verifican con ArchUnit en
`tests/architecture/ArchitectureFitnessTest.java`: pureza del dominio,
aislamiento de variantes, presentación sin saltarse `application`, ausencia de
ciclos y orquestador sin estrategias concretas. No se deben debilitar estas
pruebas para permitir una dependencia indebida.

## Pruebas

`./mvnw clean verify` ejecuta 67 pruebas (dominio, aplicación, persistencia,
controladores, mapeo BSON, WebSocket, arquitectura y métricas) y Checkstyle con
complejidad ciclomática máxima de 10. ArchUnit también valida que `application`
no dependa de Spring ni de la infraestructura y que `realtime` solo use vistas de
aplicación. JaCoCo exige al menos 80 % de cobertura de líneas y 60 % de ramas.
Las pruebas `MongoGameRepositoryIT` quedan deshabilitadas salvo que se solicite
la integración real.

`ArchitectureMetricsTest` calcula acoplamiento aferente/efrente (Ca/Ce),
inestabilidad (I), abstracción (A) y distancia a la secuencia principal (D) por
módulo y por paquete, y escribe `target/architecture-metrics.md`. El test falla
si el dominio deja de ser independiente, si presentación pasa a ser dependida o
si el contrato de variantes se aleja de la secuencia principal.

## CI/CD y métricas

GitHub Actions (`.github/workflows/architecture.yml`) se ejecuta en cada `push` y
`pull_request`:

1. `verify`: compila, ejecuta las 67 pruebas, las Fitness Functions de ArchUnit,
   Checkstyle y JaCoCo; publica `quality-reports` y escribe el reporte de
   acoplamiento en el resumen del job.
2. `integration`: levanta `mongo:7` como service container y ejecuta
   `MongoGameRepositoryIT` con `APUNTADO_MONGO_IT=true`.

Artefactos generados:

- `target/surefire-reports/`: resultados JUnit.
- `target/checkstyle-result.xml`: complejidad ciclomática y estilo.
- `target/site/jacoco/`: cobertura HTML y XML.
- `target/architecture-metrics.md`: Ca/Ce, I, A y D por módulo y paquete.

Para reproducir la verificación local sin red:

```bash
JAVA_HOME="$PWD/.tooling/jdk-17.0.21.1+2" \
./mvnw --offline --batch-mode -Dmaven.repo.local=.tooling/m2 clean verify
```

## Pendiente para completar el Entregable 3

- Ejecutar el workflow en GitHub y capturar la evidencia del pipeline.
- Habilitar Docker en WSL para la integración local (el CI ya la ejecuta).
- Diagramas UML de despliegue, secuencia y clases del código final.
- Validación con 3 a 5 usuarios externos, checklists y retrospectiva de trade-offs.

## Esquemas y extensibilidad

`schemas/` usa JSON Schema 2020-12 para validación de aplicación; no copiarlo
directamente al validador `$jsonSchema` de MongoDB.

Una nueva variante implementa `IReglaJuego`, declara un identificador estable y
su clase en
`src/main/resources/META-INF/services/com.apuntado.domain.rules.api.IReglaJuego`
dentro de su JAR. No requiere cambios en `TurnOrchestratorComponent` y debe
conservar el aislamiento entre variantes.
