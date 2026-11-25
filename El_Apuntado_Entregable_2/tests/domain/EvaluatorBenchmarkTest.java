package com.apuntado.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.apuntado.domain.model.Card;
import com.apuntado.domain.model.Hand;
import com.apuntado.domain.model.StandardDeck;
import com.apuntado.domain.rules.api.IReglaJuego;
import com.apuntado.domain.rules.apuntado.ReglaApuntado;
import com.apuntado.domain.rules.clasica.ReglaClasica;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

/**
 * Benchmark del evaluador combinatorio (riesgo R8): mide la latencia de {@code permiteCerrar} con manos
 * aleatorias y con manos adversas (muchas escaleras posibles). Escribe target/evaluator-benchmark.md.
 * El umbral es deliberadamente holgado para no fallar por ruido del runner de CI.
 */
class EvaluatorBenchmarkTest {
 private static final int WARMUP = 3_000;
 private static final int SAMPLES = 20_000;
 private static final double P99_LIMIT_MICROS = 2_000;

 private record Stats(String rule, String handType, int samples, double p50, double p95, double p99, double max,
  double closeRate) {
 }

 @Test
 void evaluatorLatencyStaysWellBelowPerceivedLatency() throws IOException {
  List<Stats> results = new ArrayList<>();
  for (IReglaJuego rule : List.of(new ReglaClasica(), new ReglaApuntado())) {
   results.add(measure(rule, "aleatoria", randomHands()));
   results.add(measure(rule, "adversa (escaleras)", adversarialHands()));
  }
  StringBuilder report = new StringBuilder("## Benchmark del evaluador de cierre (R8)\n\n")
   .append("| Regla | Mano | Muestras | p50 µs | p95 µs | p99 µs | máx µs | % cierres |\n")
   .append("| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |\n");
  for (Stats stats : results) {
   report.append(String.format(Locale.ROOT, "| %s | %s | %d | %.1f | %.1f | %.1f | %.1f | %.2f |%n", stats.rule(),
    stats.handType(), stats.samples(), stats.p50(), stats.p95(), stats.p99(), stats.max(), stats.closeRate() * 100));
  }
  report.append("\nJVM ").append(System.getProperty("java.version")).append(", ")
   .append(Runtime.getRuntime().availableProcessors()).append(" CPU. Umbral de aceptación: p99 < ")
   .append((int) P99_LIMIT_MICROS).append(" µs.\n");
  Files.createDirectories(Path.of("target"));
  Files.writeString(Path.of("target", "evaluator-benchmark.md"), report, StandardCharsets.UTF_8);
  System.out.println(report);
  for (Stats stats : results) {
   assertTrue(stats.p99() < P99_LIMIT_MICROS, stats.rule() + " " + stats.handType() + " p99=" + stats.p99());
  }
 }

 private static Stats measure(IReglaJuego rule, String handType, List<Hand> hands) {
  for (int index = 0; index < WARMUP; index++) rule.permiteCerrar(hands.get(index % hands.size()));
  double[] micros = new double[SAMPLES];
  int closes = 0;
  for (int index = 0; index < SAMPLES; index++) {
   Hand hand = hands.get(index % hands.size());
   long start = System.nanoTime();
   boolean closed = rule.permiteCerrar(hand);
   micros[index] = (System.nanoTime() - start) / 1_000.0;
   if (closed) closes++;
  }
  Arrays.sort(micros);
  return new Stats(rule.id(), handType, SAMPLES, percentile(micros, 0.50), percentile(micros, 0.95),
   percentile(micros, 0.99), micros[micros.length - 1], closes / (double) SAMPLES);
 }

 private static double percentile(double[] sorted, double fraction) {
  return sorted[Math.min(sorted.length - 1, (int) Math.ceil(fraction * sorted.length) - 1)];
 }

 private static List<Hand> randomHands() {
  List<Hand> hands = new ArrayList<>();
  for (int index = 0; index < 2_000; index++) hands.add(new Hand(StandardDeck.shuffled().subList(0, 10)));
  return hands;
 }

 /** Diez cartas casi consecutivas de dos palos: maximiza combinaciones candidatas que el evaluador debe probar. */
 private static List<Hand> adversarialHands() {
  List<Hand> hands = new ArrayList<>();
  for (int start = 1; start <= 8; start++) {
   List<Card> cards = new ArrayList<>();
   for (int offset = 0; offset < 5; offset++) {
    int rank = start + offset;
    cards.add(new Card(rank + "H", rank, "H", rank));
    cards.add(new Card(rank + "S", rank, "S", rank));
   }
   hands.add(new Hand(cards));
  }
  return hands;
 }
}
