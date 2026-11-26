# Kit de validación con usuarios reales (Entregable 3, sección 3)

| Archivo | Para qué |
| :-- | :-- |
| [PROTOCOLO.md](PROTOCOLO.md) | Diseño metodológico (3.1): objetivo, métricas y metas, participantes, guion de tareas T1–T4, roles, amenazas a la validez |
| [CONSENTIMIENTO.md](CONSENTIMIENTO.md) | Texto para leer y registro de aceptación por código de participante |
| [HOJA-OBSERVACION.md](HOJA-OBSERVACION.md) | Hoja que llena el observador durante la sesión |
| [ENCUESTA.md](ENCUESTA.md) | SUS (S1–S10), preguntas del juego (E1–E5) y abiertas (A1–A3) |
| `usuarios.csv` · `encuesta.csv` · `observaciones.csv` | Plantillas (solo encabezados) que se llenan después de cada sesión |
| [evidencias/](evidencias/README.md) | Logs de auditoría, capturas, videos y encuestas (3.2) |
| [PILOTO-1.md](PILOTO-1.md) | Sesión piloto 1: evidencias, métricas, observaciones O1–O7 y encuesta de P01 |
| `usuarios-piloto1.csv` · `encuesta-piloto1.csv` · `resultados-piloto1.md` · `resumen-piloto1.json` | Datos y resultados de la sesión piloto 1 |
| [MEJORAS.md](MEJORAS.md) | Plan de ajustes post-validación (3.4): MJ1 y MJ3 implementadas, MJ2 planificada |
| `resultados.md` | Lo genera `scripts/validacion_metricas.py` (3.3) |

## El día de la sesión, en 6 pasos

1. `docker compose up --build` (modo `random`) y archivar cualquier `logs/audit.jsonl` previo.
2. Leer el consentimiento y anotar el perfil de cada participante (código U01…).
3. Leer el texto de reglas del protocolo (§7) y pedir que piensen en voz alta.
4. Ejecutar T1–T4 sin ayudar; el observador llena la hoja y toma capturas; grabar pantalla.
5. Encuesta al terminar.
6. Copiar `logs/audit.jsonl` a `evidencias/logs/audit-AAAAMMDD-sesionN.jsonl` y llenar los tres CSV
   (en `usuarios.csv`, el `game_id` aparece en la URL `/?game=…` y el `player_id` es p1, p2…).

Luego:

```bash
python3 scripts/validacion_metricas.py --audit docs/validacion/evidencias/logs \
  --usuarios docs/validacion/usuarios.csv --encuesta docs/validacion/encuesta.csv \
  --observaciones docs/validacion/observaciones.csv --salida docs/validacion/resultados.md
```

El script está probado con datos sintéticos (`scripts/test_validacion_metricas.py`, también en el CI)
y con el registro real de la sesión piloto 1. Las plantillas `usuarios.csv`, `encuesta.csv` y
`observaciones.csv` siguen vacías para las siguientes sesiones; los datos del piloto están en sus
archivos `*-piloto1`.
