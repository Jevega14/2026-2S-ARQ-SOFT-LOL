# Protocolo de validación con usuarios reales — El Apuntado MVP

Sección 3 del Entregable 3 (RA2). Basado en ISO/IEC 25010 (adecuación funcional, usabilidad y
eficiencia de desempeño) y en el cuestionario SUS (System Usability Scale).

> **Regla de oro:** todos los datos de esta carpeta deben venir de sesiones reales con personas
> ajenas al equipo. No se completan tablas con valores estimados ni "de ejemplo".

## 1. Objetivo

Comprobar con jugadores reales que el MVP:

1. Permite configurar una partida y jugar turnos completos **sin asistencia técnica**.
2. Valida correctamente los cierres (4-3-3 en Clásica; 3-3-3 + sobrante < 5 en Apuntado) y que el
   jugador **entiende** por qué un cierre es aceptado o rechazado.
3. Muestra el **conteo de puntos** de forma clara, incluida la acumulación hasta 101.
4. Responde con una **latencia** que el jugador no percibe como lenta.

## 2. Preguntas de evaluación y métricas

| # | Pregunta (característica ISO 25010) | Métrica | Fuente | Meta |
| :-- | :-- | :-- | :-- | :-- |
| M1 | ¿Logran cerrar una ronda válida? (efectividad) | Tasa de éxito = participantes con ≥ 1 `CLOSE OK` / participantes | `audit.jsonl` | ≥ 80 % |
| M2 | ¿Qué reglas confunden? (efectividad) | Cierres rechazados por participante y su motivo; errores de flujo (fuera de turno, robar/descartar) | `audit.jsonl` + hoja de observación | Documentar todos |
| M3 | ¿Cuánto tarda un turno? (eficiencia) | Tiempo promedio por turno (s), desde que recibe el turno hasta terminarlo o cerrar | `audit.jsonl` | Reportar media y mediana |
| M4 | ¿Responde rápido el servidor? (desempeño) | Latencia del servidor p50 / p95 por comando (ms) | `audit.jsonl` (`durationMicros`) | p95 < 200 ms |
| M5 | ¿Se percibe lento? | Rapidez percibida (1–5) | Encuesta E2 | ≥ 4 |
| M6 | ¿Es claro el conteo? | Claridad del conteo (1–5) | Encuesta E1 | ≥ 4 |
| M7 | Usabilidad global | Puntaje SUS (0–100) | Encuesta S1–S10 | ≥ 68 (promedio de la industria) |
| M8 | Satisfacción | Satisfacción general (1–5) | Encuesta E3 | ≥ 4 |
| M9 | Tareas sin ayuda | Tareas completadas sin intervención / tareas intentadas | Hoja de observación | ≥ 80 % |

## 3. Participantes

- **De 3 a 5 personas ajenas al equipo** (amistades, familiares, compañeras o compañeros de otros cursos).
  Nadie que haya visto el código o el diseño.
- Registrar el perfil sin datos personales identificables: rango de edad, familiaridad con juegos de
  cartas (ninguna / conoce El Apuntado / juega seguido) y experiencia con apps web (baja / media / alta).
- Cada participante recibe un código `U01`, `U02`… Los nombres reales **no** se escriben en el repositorio.
- Ideal: al menos un participante que no conozca El Apuntado y uno que sí.

## 4. Preparación (release checklist de la sesión)

- [ ] `docker compose up --build` con **`APUNTADO_DECK=random`** (valor por defecto). Nunca `demo`.
- [ ] Borrar o archivar `logs/audit.jsonl` de pruebas anteriores para empezar limpio.
- [ ] Abrir <http://localhost:8080> y verificar que carguen ambas modalidades.
- [ ] Si juegan en varios dispositivos: obtener la IP del equipo servidor y probar un enlace de jugador
      desde un celular en la misma red.
- [ ] Grabador de pantalla listo (OBS, QuickTime o la grabación del sistema) y permiso para grabar.
- [ ] Impresas o abiertas: hoja de observación, guion de tareas y encuesta.
- [ ] Reloj visible para el moderador.

## 5. Roles

- **Moderador:** lee el guion, no explica la interfaz, solo responde "¿qué crees que deberías hacer?".
  Si el participante está bloqueado más de 2 minutos, puede dar **una** pista y se registra como ayuda.
- **Observador:** llena la hoja de observación (confusiones, citas textuales, tiempos) y toma capturas.

## 6. Formato de la sesión (≈ 25–30 min por grupo)

| Minuto | Actividad |
| :-- | :-- |
| 0–3 | Bienvenida, consentimiento ([CONSENTIMIENTO.md](CONSENTIMIENTO.md)), perfil del participante |
| 3–5 | Explicación mínima de las reglas (texto de §7, igual para todos); se pide pensar en voz alta |
| 5–22 | Tareas T1–T4 |
| 22–28 | Encuesta ([ENCUESTA.md](ENCUESTA.md)) y preguntas abiertas |
| 28–30 | Cierre; el equipo anota en `usuarios.csv` qué partida y asiento jugó cada participante |

Se pueden evaluar 2–3 participantes en la misma partida (cada uno en un asiento). Cada participante
juega **al menos una partida de cada modalidad**, o se reparte: unos Clásica, otros Apuntado, uno ambas.

## 7. Texto de reglas que lee el moderador (idéntico en todas las sesiones)

> "Es un juego de cartas con baraja francesa. Cada jugador tiene 10 cartas. En tu turno robas una carta
> del mazo o del pozo y luego descartas una. Ganas cuando, después de descartar, tus cartas forman grupos:
> tríos o cuartetos del mismo número con palos distintos, o escaleras del mismo palo. En la modalidad
> Clásica necesitas un grupo de 4 y dos de 3. En Apuntado necesitas tres grupos de 3 y la carta que sobra
> debe valer menos de 5 puntos; los demás suman los puntos de sus cartas y quien llega a 101 queda fuera."

## 8. Guion de tareas (sin asistencia técnica)

| Tarea | Instrucción al participante | Éxito cuando… |
| :-- | :-- | :-- |
| **T1** | "Inicia una partida nueva en la modalidad que te indico, con los jugadores de esta mesa." | La partida aparece con 10 cartas por jugador |
| **T2** | "Juega al menos 3 turnos completos: roba (del mazo o del pozo), organiza tu mano y descarta." | 3 `END_TURN` sin ayuda |
| **T3a** | "Cuando creas que puedes, declara el cierre de la ronda." | `CLOSE OK` |
| **T3b** | "Ahora intenta cerrar a propósito con una mano que NO cumple, para ver qué responde el sistema." | `CLOSE REJECTED` y el participante explica el motivo con sus palabras |
| **T4** | (Apuntado) "Sigan jugando rondas hasta que alguien llegue a 101 y termine la partida." | Se observa la acumulación y el anuncio de ganador |

Notas:

- T3a puede no ocurrir de forma natural en pocos turnos. Si tras ~12 turnos nadie cierra, se registra
  como "no alcanzó cierre" (es un resultado válido) y se continúa con T3b.
- Para T4, si el tiempo no alcanza para llegar a 101 con barajas aleatorias, se registra hasta dónde
  llegaron. No se usa el modo demo con participantes.

## 9. Evidencias que se deben guardar

En `docs/validacion/evidencias/`:

- `logs/audit-AAAAMMDD-sesionN.jsonl`: copia de `logs/audit.jsonl` al final de cada sesión.
- `capturas/`: capturas de pantalla de partidas en curso, cierre válido, cierre rechazado y ganador.
- `videos/`: grabaciones cortas, o un `enlaces.md` con links (Drive/YouTube no listado) si pesan mucho.
- `encuestas/`: respuestas exportadas (CSV del formulario o fotos de las hojas).
- Fotografías de la sesión **sin rostros** o con consentimiento explícito.

Después de cada sesión, completar:

- [`usuarios.csv`](usuarios.csv): participante → partida(s) → asiento.
- [`encuesta.csv`](encuesta.csv): respuestas numéricas de la encuesta.
- [`observaciones.csv`](observaciones.csv): confusiones, ayudas y citas.

## 10. Análisis

```bash
python3 scripts/validacion_metricas.py \
  --audit docs/validacion/evidencias/logs \
  --usuarios docs/validacion/usuarios.csv \
  --encuesta docs/validacion/encuesta.csv \
  --observaciones docs/validacion/observaciones.csv \
  --salida docs/validacion/resultados.md
```

El script genera la **tabla consolidada** de la sección 3.3 (ID, modalidad, ¿cerró ronda válida?, errores,
tiempo promedio por turno, satisfacción), el puntaje SUS por participante y global, la latencia del
servidor y el detalle de cierres rechazados. Las conclusiones y el plan de mejoras se escriben en
[MEJORAS.md](MEJORAS.md).

## 11. Amenazas a la validez (para declarar en el informe)

- Muestra pequeña (3–5): resultados indicativos, no estadísticamente generalizables.
- Participantes cercanos al equipo pueden ser más benévolos en la encuesta (sesgo de cortesía).
- En modo relevo (un dispositivo) el tiempo por turno incluye pasar el dispositivo.
- Barajas aleatorias: no todos tienen la misma probabilidad de lograr un cierre en la sesión.
- La latencia medida es la del servidor; la percibida incluye red y navegador (por eso se pregunta E2).
