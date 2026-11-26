# Guía de contribución — El Apuntado

## Propósito

Este repositorio contiene el MVP del juego El Apuntado (Entregable 3): interfaz
web, orquestación de turnos, persistencia en MongoDB, notificación por WebSocket,
cierre transaccional con idempotencia y auditoría por comando. Esta guía resume
las reglas que el equipo sigue al modificar el código.

## Tecnologías y comandos

- Java 17.
- Maven Wrapper incluido (Maven 3.9.16).
- Ejecutar comandos desde esta carpeta:

  ```bash
  ./mvnw clean verify
  ```

- Las pruebas viven en `tests/`, configurado como fuente de pruebas en
  `pom.xml`.
- No añadir dependencias sin una necesidad justificada.

## Arquitectura

Mantener los paquetes y sus responsabilidades:

- `domain`: modelo y lógica de negocio pura. No depende de application,
  bootstrap, presentation, persistencia, red, bases de datos ni frameworks.
- `domain.rules.api`: contrato estable de las variantes (`IReglaJuego`).
- `domain.rules.clasica` y `domain.rules.apuntado`: implementaciones
  independientes entre sí.
- `application`: coordinación de casos de uso; no debe conocer variantes
  concretas.
- `bootstrap`: composición y descubrimiento de proveedores mediante
  `ServiceLoader`.
- `presentation`: entrada/salida; solo se comunica con application.

Las restricciones se verifican con ArchUnit en
`tests/architecture/ArchitectureFitnessTest.java`. No se deben debilitar
estas pruebas para permitir una dependencia indebida.

## Reglas de negocio vigentes

- Baraja de 52 cartas, sin comodines; as vale 1, figuras valen 10.
- Participan de 2 a 4 jugadores.
- Clásica cierra con grupos 4-3-3 y el primer cierre válido termina la partida (una sola ronda).
- Apuntado cierra con grupos 3-3-3 y sobrante menor que 5.
- El cierre otorga 0 puntos a quien cierra; los demás suman todas sus cartas.
- Hay eliminación desde 101 puntos; gana el último jugador no eliminado.

Antes de cambiar una regla, contrastarla con el README y los ADR pertinentes.

## Variantes extensibles

Una nueva variante debe:

1. Implementar `IReglaJuego`.
2. Tener un identificador único y estable.
3. Declararse en
   `src/main/resources/META-INF/services/com.apuntado.domain.rules.api.IReglaJuego`
   o, si es externa, en el archivo equivalente dentro de su JAR.
4. No requerir cambios en `TurnOrchestratorComponent`.
5. Añadir pruebas de dominio y preservar el aislamiento entre variantes.

No se admite recarga en caliente ni ejecución aislada de proveedores no
confiables.

## Calidad y pruebas

- Ejecutar `./mvnw clean verify` tras cambios de código o configuración, y las
  pruebas `*IT` con `APUNTADO_MONGO_IT=true` si se toca persistencia.
- Mantener la complejidad ciclomática en un máximo de 10, según Checkstyle.
- Añadir o actualizar pruebas de dominio al cambiar una regla.
- No modificar evidencia histórica en `docs/evidence/` salvo que se solicite
  regenerarla explícitamente.
- Mantener `README.md`, ADR y diagramas alineados con los cambios
  arquitectónicos relevantes.

## Esquemas y datos

- Los archivos en `schemas/` usan JSON Schema 2020-12 para validación de
  aplicación; no copiarlos directamente a validadores `$jsonSchema` de MongoDB.
- Las referencias, la unicidad de cartas por zona, acumulados y transiciones
  de estado requieren validación adicional en dominio.
- El servidor debe validar que cada carta recibida pertenezca realmente a la
  baraja y coincida con su identificador y palo.

## Límites de modificación

- MongoDB, REST, WebSocket, recibos de comando y auditoría ya están implementados
  (Entregable 3); cambios en sus contratos requieren revisar ADR-04 y ADR-05.
- No sobrescribir ADR-01 ni ADR-02: deben conservar o recuperar el contenido
  auténtico del Entregable 1.
- Evitar cambios incompatibles en `IReglaJuego`, los identificadores de
  variantes y los contratos de dominio.
