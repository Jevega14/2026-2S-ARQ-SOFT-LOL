# Resultados de la validación con usuarios

Generado por `scripts/validacion_metricas.py` el 2026-11-25 19:07 a partir de: docs/validacion/evidencias/logs/audit-20261125-piloto1.jsonl, docs/validacion/usuarios-piloto1.csv, docs/validacion/encuesta-piloto1.csv.

## 3.3 Tabla consolidada de métricas de usuario

| ID Usuario | Modalidad probada | ¿Logró cerrar ronda válida? | Errores / confusiones detectadas | Tiempo promedio por turno | Satisfacción (1-5) | SUS (0-100) |
| :-- | :-- | :-- | :-- | --: | --: | --: |
| P01 | Apuntado (101) | No | cierre inválido (regla) ×2 | 91,2 s | 4,0 / 5 | 67,5 |

## Indicadores frente a las metas del protocolo

| Métrica | Resultado | Meta | Estado |
| :-- | --: | :-- | :-- |
| M1 Tasa de éxito en cierre | 0 % | ≥ 80 % | ❌ no cumple |
| M2 Cierres rechazados (total) | 2 | documentar | — |
| M3 Tiempo por turno (media · mediana) | 91,2 s · 75,9 s | reportar | — |
| M4 Latencia del servidor p50 · p95 | 6,6 ms · 11,2 ms | p95 < 200 ms | ✅ cumple |
| M5 Rapidez percibida (E2) | 5,0 | ≥ 4 | ✅ cumple |
| M6 Claridad del conteo (E1) | 5,0 | ≥ 4 | ✅ cumple |
| M7 SUS promedio | 67,5 | ≥ 68 | ❌ no cumple |
| M8 Satisfacción general (E3) | 4,0 | ≥ 4 | ✅ cumple |
| M9 Tareas sin ayuda | — | ≥ 80 % | sin datos |

Participantes con partidas registradas: **1** · partidas: 2 (terminadas: 0).

## Detalle por partida

| Partida | Modalidad | Rondas cerradas | ¿Terminó? | Ganador | Totales finales |
| :-- | :-- | --: | :-- | :-- | :-- |
| `800709e4` | Apuntado (101) | 0 | No | — | — |
| `8fe4089b` | Apuntado (101) | 1 | No | — | p1: 75, p2: 0 |

## Cierres rechazados por el motor de reglas

- **P01:** Cierre inválido: se requiere tres ternas y una carta sobrante de menos de 5 puntos
- **P01:** Cierre inválido: se requiere tres ternas y una carta sobrante de menos de 5 puntos
