# Sesión piloto 1 (prueba interna del protocolo)

**Fecha:** 2026-11-25, 22:40–23:20 UTC · **Entorno:** `docker compose` local (perfil `mongo`, baraja `random`)
**Modo:** dos pestañas del mismo equipo, una por jugador (enlaces `/?game=&player=`).
**Participantes:** asiento `p1` «Jessi», integrante del equipo, sin experiencia previa en juegos de cartas.
Asiento `p2` «Russ»: _[completar quién lo jugó]_.

> Es una **prueba piloto interna**, no una sesión con usuarios externos. Sirve para validar el
> protocolo, el registro de auditoría y el script de métricas, y aporta hallazgos de usabilidad reales.
> No reemplaza las 3–5 sesiones del protocolo ni se mezcla con `usuarios.csv`, `encuesta.csv` u
> `observaciones.csv`: la encuesta de P01 está en `encuesta-piloto1.csv` y `usuarios-piloto1.csv`.

## Evidencia

| Evidencia | Archivo |
| :-- | :-- |
| Registro de auditoría (114 comandos, 2 partidas) | `evidencias/logs/audit-20261125-piloto1.jsonl` |
| Métricas calculadas por el script | `resultados-piloto1.md`, `resumen-piloto1.json` |
| Capturas | `evidencias/capturas/piloto1-0[1-5]-*.png` |

## Métricas obtenidas del registro (`scripts/validacion_metricas.py`)

| Métrica | Resultado |
| :-- | :-- |
| Partidas creadas / rondas cerradas | 2 / 1 (partida `8fe4089b`, cerró `p2` en la ronda 1; `p1` sumó 75) |
| Cierres rechazados por el motor de reglas | 4 en toda la sesión (3 de `p1`, 1 de `p2`, ambas partidas), todos con el mensaje «se requiere tres ternas y una carta sobrante de menos de 5 puntos». El script cuenta 2 para P01 en la partida `8fe4089b`. |
| Tiempo por turno (media · mediana) | P01: 91,2 s · 75,9 s (ambos asientos: 70,0 s · 50,8 s) |
| Latencia del servidor p50 · p95 | 6,6 ms · 11,2 ms (meta p95 < 200 ms ✅) |

La partida `800709e4` fue un primer intento de exploración (15 comandos) y se abandonó.

## Observaciones (vistas durante la sesión en las capturas)

| # | Momento | Tipo | Observación |
| :-- | :-- | :-- | :-- |
| O1 | Inicio | Regla | La participante no conocía el juego: necesitó explicación de qué es una terna, una escalera, la carta sobrante y el valor de las cartas antes de jugar. |
| O2 | Ronda 1, turnos 2–4 | Interacción | Descartó dos veces la carta que acababa de robar del pozo (A♣) en lugar de la que quería botar: un clic sobre una carta la descarta de inmediato, sin selección ni confirmación. Esperaba «seleccionar y luego descartar». |
| O3 | Ronda 1 | Interacción | En modo varios dispositivos vio «Mano de Jessi» durante el turno de Russ y preguntó si era una fuga de información. No quedó claro que cada enlace fija el jugador del navegador. |
| O4 | Ronda 1 | Regla | Al completar un trío descartó la carta baja que reservaba como sobrante (4♠); no tenía presente que la sobrante también debe quedarse en la mano. |
| O5 | Ronda 1 | Interacción | Tras robar preguntó «¿cuál robo?»: no notó que ya estaba en fase de descarte (el indicador «fase: descartar» pasa desapercibido). |
| O6 | Ronda 1 | Regla | Con 5♣ 6♣ 7♣ 8♣ pensó que podía cerrar; una escalera de 4 no equivale a dos ternas. |
| O7 | Fin de ronda 1 | Regla | Se sorprendió de recibir 75 puntos: no esperaba que las cartas ya agrupadas también sumen al perder la ronda. |

## Encuesta de P01 (respondida de forma verbal el 2026-11-25 y transcrita a la escala)

| Ítem | Resp. | Ítem | Resp. |
| :-- | :-: | :-- | :-: |
| S1 Usaría el juego con frecuencia | 3 (no le gustan los juegos de cartas) | E1 Conteo de puntos claro | 5 |
| S2 Innecesariamente complejo | 2 | E2 Respondió rápido | 5 |
| S3 Fácil de usar | 3 | E3 Satisfacción general | 4 |
| S4 Necesitaría ayuda técnica | 2 | E4 Entendí el cierre rechazado | 4 |
| S5 Funciones bien integradas | 4 | E5 Entendí cuándo robar del mazo o del pozo | 3 |
| S6 Demasiada inconsistencia | 2 | | |
| S7 La mayoría aprendería rápido | 5 | | |
| S8 Engorroso de usar | 2 | | |
| S9 Me sentí con confianza | 4 | | |
| S10 Necesité aprender muchas cosas | 4 | | |

**SUS = 67,5** (promedio de referencia: 68). **Respuestas abiertas:** A1, lo más confuso fue robar del mazo o del pozo y
que, en el mismo computador, una pestaña dijera «tu turno» mientras otra mostraba la mano del rival; A2, que el juego sea
«más diciente» para el usuario; A3, no dudó del conteo ni de la validación, y las mejoras le parecieron bien.

## Hallazgos que alimentan `MEJORAS.md`

- **MJ1** (O2): descarte en dos pasos (seleccionar → «Descartar X»). **Implementada** el 2026-11-25; ver `MEJORAS.md`.
- **MJ2** (O1, O4, O6, O7): ayuda de reglas en pantalla: grupos detectados en la mano, cuál sería la sobrante y aviso de que todas las cartas suman si otro cierra.
- **MJ3** (O3, O5): rotular el modo («Este navegador es de Jessi») y resaltar la fase actual con la acción esperada.
