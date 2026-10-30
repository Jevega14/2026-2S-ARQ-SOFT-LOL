# El Apuntado — Soporte del Entregable 2

Base nueva de arquitectura; no es un MVP completo. Java 17 mínimo, Maven 3.9.9 usado en verificación. Las dependencias están fijadas en pom.xml. Ejecutar `mvn clean verify` desde esta carpeta. Las pruebas están en tests/architecture y tests/domain (configurado testSourceDirectory).

Implementado: evaluación de diez cartas después del descarte, Clásica 4-3-3, Apuntado 3-3-3 con sobrante menor que cinco, identidades únicas de cartas, acumulación y umbral, descubrimiento de estrategias con ServiceLoader, pruebas y control de CC. Presentación y orquestador son esqueletos, no endpoints.

Perfil adoptado MVP1: una baraja de 52 cartas, 2 a 4 participantes, sin comodines, as bajo=1, números nominales y figuras=10. Cierre Clásica 4-3-3; Apuntado 3-3-3 y sobrante <5. Quien cierra suma cero y cada rival suma todas sus cartas. Eliminación desde 101; gana el último no eliminado. Desconexión pausa; reconexión recupera; cancelación sin ganador. Apuestas fuera del MVP. El flujo de eliminación, mazo y desconexión está especificado, todavía no implementado. No se ha implementado MongoDB, comunicación de red ni cierre transaccional. Card deriva points de rank; el servidor debe validar que cada id y palo correspondan a una carta real de su baraja.

Una nueva estrategia implementa IReglaJuego y declara su clase en META-INF/services/com.apuntado.domain.rules.api.IReglaJuego dentro de su JAR. Se incorpora al classpath y se reinicia el proceso. No hay modificación ni recompilación del orquestador; sí se compila el proveedor nuevo. No hay recarga en caliente ni aislamiento de código no confiable.

schemas/ usa JSON Schema 2020-12 para validar en aplicación; no copiar directamente al validador $jsonSchema de MongoDB. Las referencias entre IDs, cartas únicas en zonas, acumulados y estados se verifican adicionalmente en dominio. Publicar estos archivos en el repositorio del equipo e insertar su URL en el documento. Recuperar los ADR-01 y ADR-02 auténticos del Entregable 1, no sobrescribirlos con títulos del boceto.
