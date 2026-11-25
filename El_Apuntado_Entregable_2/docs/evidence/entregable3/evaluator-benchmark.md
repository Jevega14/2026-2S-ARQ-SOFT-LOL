## Benchmark del evaluador de cierre (R8)

| Regla | Mano | Muestras | p50 µs | p95 µs | p99 µs | máx µs | % cierres |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| CLASICA | aleatoria | 20000 | 47.5 | 77.7 | 89.8 | 4130.1 | 0.00 |
| CLASICA | adversa (escaleras) | 20000 | 65.8 | 72.6 | 81.7 | 1156.0 | 0.00 |
| APUNTADO101 | aleatoria | 20000 | 24.4 | 39.5 | 51.9 | 1521.9 | 0.00 |
| APUNTADO101 | adversa (escaleras) | 20000 | 85.3 | 93.7 | 107.8 | 814.2 | 0.00 |

JVM 17.0.21, 2 CPU. Umbral de aceptación: p99 < 2000 µs.
