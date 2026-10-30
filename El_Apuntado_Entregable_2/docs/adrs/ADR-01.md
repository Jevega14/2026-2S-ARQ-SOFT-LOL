# ADR-01 — Seleccionar particionamiento superior por dominio

**Estado:** Accepted

## Contexto

El sistema debe organizar diferentes responsabilidades relacionadas con la gestión de partidas, turnos, reglas, puntuación, cartas y persistencia. Se deben considerar dos alternativas principales para el particionamiento superior de la aplicación: particionamiento técnico y particionamiento por dominio.

El particionamiento técnico agrupa componentes según su naturaleza tecnológica, por ejemplo presentación, servicios, persistencia y utilidades. El particionamiento por dominio organiza los componentes de acuerdo con las capacidades y conceptos principales del negocio.

El dominio del proyecto presenta una fuerte concentración de variabilidad en las reglas del juego. Las modificaciones relacionadas con modalidades de juego deben permanecer aisladas del resto de la aplicación.

## Alternativas consideradas

### Alternativa A — Particionamiento técnico

Organizar el sistema principalmente mediante capas técnicas como presentación, servicios, persistencia y utilidades.

### Alternativa B — Particionamiento por dominio

Organizar el sistema alrededor de conceptos y capacidades del dominio del juego, manteniendo las responsabilidades relacionadas dentro de módulos cohesivos.

## Decisión

Se adopta el **particionamiento por dominio** como estrategia de organización superior del sistema.

Los módulos se estructurarán alrededor de responsabilidades del dominio tales como:

- Gestión de partidas y salas.
- Gestión de turnos.
- Motor de reglas.
- Puntuación y liquidación.
- Gestión de cartas.
- Persistencia del estado.

## Justificación

El particionamiento por dominio permite mantener juntas las responsabilidades que cambian por razones similares. En particular, las modificaciones de las reglas del juego permanecen principalmente dentro del módulo correspondiente al motor de reglas.

Esta decisión favorece la cohesión y reduce la dispersión de cambios relacionados con el dominio a través de diferentes capas técnicas.

## Consecuencias

**Positivas:**

- Mayor cohesión entre responsabilidades relacionadas.
- Las modificaciones de reglas quedan concentradas en el dominio.
- Facilita la identificación de límites de los módulos.
- Favorece el bajo acoplamiento entre las reglas y la infraestructura.

**Negativas:**

- Requiere definir cuidadosamente las interfaces entre módulos.
- Algunas operaciones pueden atravesar más de un módulo.
- La organización inicial requiere mayor análisis del dominio.

## Compliance

Se utilizará **ArchUnit 1.3.0**, ya declarado como dependencia de pruebas en `pom.xml`. Las reglas se ejecutarán con `mvn clean verify` mediante `tests/architecture/ArchitectureFitnessTest.java`.

La verificación deberá fallar si:

1. Una clase del paquete `..presentation..` depende directamente de `..persistence..`, `..domain..`, `com.mongodb..` o `java.sql..`. Esto se comprueba con `presentationCannotBypassApplication()`.
2. Una clase del paquete `..domain..` depende de paquetes de aplicación, presentación, persistencia o bootstrap, o de infraestructura como `java.net..`, `java.sql..`, `com.mongodb..`, `org.springframework..`, `jakarta..` o `io.netty..`. Esto se comprueba con `domainIsPure()`.
3. Los paquetes de producción bajo `com.apuntado` presentan ciclos de dependencias. Esto se comprueba con `noPackageCycles()`.

La compilación de verificación debe finalizar sin violaciones de estas reglas.