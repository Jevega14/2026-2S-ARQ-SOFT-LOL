# Plan de ajustes y mejoras post-validación (sección 3.4)

Completar **después** de analizar los resultados. Cada mejora debe nacer de una observación o métrica
real (citar el participante y la evidencia). La guía exige al menos 2 ajustes realizados o requeridos.

| ID | Hallazgo (evidencia) | Usuarios | Impacto | Ajuste propuesto | Capa afectada | Estado | Verificación |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| MJ1 | Un clic sobre una carta la descarta sin confirmación; se descartó por error la carta recién robada ×2 (PILOTO-1, O2) | Piloto (p1) | Alto | Descarte en dos pasos: tocar la carta la selecciona (se eleva con borde rojo) y el botón «Descartar X» confirma; «Cancelar selección» o tocar otra carta la cambia. También funciona con teclado (Enter/Espacio) | UI (`static/app.js`, `styles.css`) | Implementado | Prueba manual en navegador 2026-11-25: seleccionar, cambiar, cancelar y confirmar envían un solo comando `discard` (versión 1 → 2); captura `evidencias/capturas/mj1-descarte-en-dos-pasos.jpg`. Sin cambios en dominio, API ni fitness functions |
| MJ2 | Dudas de reglas: sobrante descartada, escalera de 4 tomada como cierre, sorpresa por los 75 puntos (PILOTO-1, O1/O4/O6/O7; 4 cierres rechazados en el log) | Piloto (p1) | Medio | Ayuda en pantalla: grupos detectados, sobrante sugerida y aviso de que todas las cartas suman al perder | UI + aplicación (vista) | Requerido | — |
| MJ3 | Modo varios dispositivos y fase actual poco visibles (PILOTO-1, O3/O5) | Piloto (p1) | Bajo | Rótulo «Este navegador es de X» y fase resaltada con la acción esperada | UI | Requerido | — |

Para cada mejora implementada, registrar el commit, la prueba automatizada que la cubre y si afectó
alguna fitness function o decisión (ADR). Si una mejora requiere cambiar la arquitectura, explicarlo
como trade-off en la retrospectiva (sección 5).
