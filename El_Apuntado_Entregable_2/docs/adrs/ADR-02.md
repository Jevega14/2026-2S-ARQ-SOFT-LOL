# ADR-02 — Desacoplar las variantes de reglas mediante Strategy

**Estado:** Accepted

## Contexto

El sistema debe soportar como mínimo dos modalidades de juego: modalidad Clásica y modalidad de Apuntado. Las condiciones de cierre y liquidación de las modalidades presentan diferencias, por lo que una implementación que concentre todas las reglas dentro del flujo principal aumentaría el acoplamiento y dificultaría la incorporación de nuevas variantes.

Se consideran dos alternativas principales:

1. Implementar las variantes mediante un patrón Strategy dentro de un monolito modular.
2. Implementar las variantes como módulos o plugins independientes mediante una arquitectura Microkernel / Plug-in.

## Alternativas consideradas

### Alternativa A — Strategy dentro de un Monolito Modular

Definir una interfaz común para las reglas e implementar cada modalidad como una estrategia concreta.

### Alternativa B — Microkernel / Plug-in

Construir un núcleo reducido capaz de cargar diferentes módulos de reglas como plugins independientes.

## Decisión

Se adopta el **patrón Strategy dentro de una arquitectura de Monolito Modular**.

La abstracción `IReglaJuego` definirá el contrato común de las reglas. Las modalidades se implementarán mediante estrategias concretas:

- `ReglaClasicaCuartaTerna`
- `ReglaApuntado101`

El componente encargado de evaluar la mano dependerá de la abstracción `IReglaJuego` y no de una implementación concreta.

En el soporte Java actual, las clases de las estrategias se llaman `ReglaClasica` y `ReglaApuntado`, respectivamente.

## Justificación

La cantidad de variantes requerida por el MVP no justifica la complejidad operacional de una arquitectura Microkernel / Plug-in. El patrón Strategy proporciona el nivel de variabilidad requerido manteniendo la solución dentro de un monolito modular.

Esta decisión permite aislar los algoritmos correspondientes a cada modalidad y evita que el flujo principal del juego tenga que conocer los detalles de cada conjunto de reglas.

La incorporación de una nueva modalidad puede realizarse mediante una nueva implementación del contrato `IReglaJuego`, reduciendo la necesidad de modificar los componentes que consumen la abstracción.

## Consecuencias

**Positivas:**

- Las reglas de cada modalidad permanecen encapsuladas.
- El flujo principal del juego no depende de implementaciones concretas.
- Las estrategias pueden probarse de manera independiente.
- La incorporación de nuevas variantes puede realizarse mediante nuevas implementaciones del contrato.
- Se favorece el principio Open/Closed.

**Negativas:**

- Se introduce una abstracción adicional.
- El sistema requiere administrar la selección de la estrategia correspondiente.
- Una gran cantidad de variantes podría aumentar el número de implementaciones concretas.

## Compliance

Se utilizarán pruebas JUnit 5 y **ArchUnit 1.3.0**, ejecutadas con `mvn clean verify`.

1. `tests/domain/RuleVariantsTest.java` debe comprobar que:
   - La estrategia Clásica acepta una combinación válida de una cuarta y dos ternas.
   - La estrategia Clásica rechaza una mano que no cumple esa condición.
   - Apuntado acepta sobrantes con valor total menor que cinco y rechaza un total de cinco o más.
   - El registro carga las estrategias Clásica y Apuntado, y rechaza una modalidad desconocida.

2. `tests/architecture/ArchitectureFitnessTest.java`, mediante `orchestratorDoesNotKnowStrategies()`, debe fallar si `TurnOrchestratorComponent` depende directamente de paquetes de estrategias concretas (`..rules.clasica..`, `..rules.apuntado..` o `..rules.common..`).

3. El evaluador de manos debe recibir y utilizar `IReglaJuego`; no debe depender de `ReglaClasicaCuartaTerna`, `ReglaApuntado101` ni de sus clases concretas actuales. Esta restricción se verificará con una regla ArchUnit en `ArchitectureFitnessTest.java`.

Al incorporar una nueva variante compatible, las pruebas y reglas arquitectónicas deben pasar sin modificar el orquestador ni los componentes que consumen `IReglaJuego`.