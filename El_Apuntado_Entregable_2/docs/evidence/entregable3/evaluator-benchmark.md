## Benchmark del evaluador de cierre (R8)

| Regla | Mano | Muestras | p50 µs | p95 µs | p99 µs | máx µs | % cierres |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| CLASICA | aleatoria | 20000 | 54.3 | 79.7 | 89.3 | 6668.7 | 0.00 |
| CLASICA | adversa (escaleras) | 20000 | 66.5 | 71.8 | 82.1 | 680.4 | 0.00 |
| APUNTADO101 | aleatoria | 20000 | 23.6 | 38.8 | 51.8 | 1363.7 | 0.00 |
| APUNTADO101 | adversa (escaleras) | 20000 | 84.3 | 91.7 | 111.2 | 710.9 | 0.00 |

JVM 17.0.21, 2 CPU. Umbral de aceptación: p99 < 2000 µs.
