#!/usr/bin/env python3
"""Métricas de la validación con usuarios (Entregable 3, sección 3.3).

Lee el registro de auditoría del MVP (logs/audit.jsonl, una línea JSON por comando), la relación
participante → partida → asiento (usuarios.csv), la encuesta (encuesta.csv) y las observaciones
(observaciones.csv), y genera la tabla consolidada en Markdown (y opcionalmente JSON).

Solo usa la biblioteca estándar de Python 3.9+.

Uso:
  python3 scripts/validacion_metricas.py --audit docs/validacion/evidencias/logs \
      --usuarios docs/validacion/usuarios.csv --encuesta docs/validacion/encuesta.csv \
      --observaciones docs/validacion/observaciones.csv --salida docs/validacion/resultados.md
"""
from __future__ import annotations

import argparse
import csv
import json
import re
import statistics
from collections import defaultdict
from dataclasses import dataclass, field
from datetime import datetime, timezone
from pathlib import Path

VARIANT_LABEL = {"CLASICA": "Clásica (4-3-3)", "APUNTADO101": "Apuntado (101)"}
SUS_ITEMS = [f"S{i}" for i in range(1, 11)]
EXTRA_ITEMS = ["E1", "E2", "E3", "E4", "E5"]
ERROR_CLASSES = [
    ("Cierre inválido", "cierre inválido (regla)"),
    ("Se evalúan diez cartas", "intentó cerrar sin descartar"),
    ("fuera de turno", "movimiento fuera de turno"),
    ("Debe robar", "descartó sin robar"),
    ("Debe descartar", "terminó turno sin descartar"),
    ("commandId", "commandId reutilizado"),
    ("pozo está vacío", "robó de pozo vacío"),
    ("La carta no pertenece", "carta inválida"),
]
CLOSE_RESULT = re.compile(r"(p[\w-]+):\+(\d+)=(\d+)")


# ---------------------------------------------------------------- lectura de auditoría

def parse_instant(value: str) -> datetime:
    """Convierte un Instant de Java (hasta nanosegundos, sufijo Z) a datetime UTC."""
    text = value.replace("Z", "+00:00")
    match = re.match(r"^(.*T\d{2}:\d{2}:\d{2})(\.\d+)?(\+00:00)$", text)
    if match:
        fraction = (match.group(2) or ".0")[:7].ljust(7, "0")
        text = match.group(1) + fraction + match.group(3)
    return datetime.fromisoformat(text).astimezone(timezone.utc)


def read_audit(path: Path) -> list[dict]:
    files = sorted(path.glob("*.jsonl")) if path.is_dir() else [path]
    entries = []
    for file in files:
        for number, line in enumerate(file.read_text(encoding="utf-8").splitlines(), start=1):
            if not line.strip():
                continue
            try:
                entry = json.loads(line)
            except json.JSONDecodeError as error:
                raise SystemExit(f"{file}:{number}: línea JSON inválida ({error})") from error
            entry["_at"] = parse_instant(entry["at"])
            entries.append(entry)
    entries.sort(key=lambda item: item["_at"])
    return entries


def classify_error(detail: str | None) -> str:
    for needle, label in ERROR_CLASSES:
        if needle in (detail or ""):
            return label
    return (detail or "error sin detalle").split(": ", 1)[-1][:60]


# ---------------------------------------------------------------- métricas por partida

@dataclass
class PlayerStats:
    turn_seconds: list[float] = field(default_factory=list)
    closes_ok: int = 0
    closes_rejected: int = 0
    errors: dict[str, int] = field(default_factory=lambda: defaultdict(int))
    rejected_details: list[str] = field(default_factory=list)
    latencies_ms: list[float] = field(default_factory=list)
    commands: int = 0


@dataclass
class GameStats:
    game_id: str
    variant: str | None = None
    players: dict[str, PlayerStats] = field(default_factory=lambda: defaultdict(PlayerStats))
    rounds_closed: int = 0
    finished: bool = False
    winner: str | None = None
    final_totals: dict[str, int] = field(default_factory=dict)
    started: datetime | None = None
    ended: datetime | None = None


def analyse_games(entries: list[dict]) -> dict[str, GameStats]:
    games: dict[str, GameStats] = {}
    turn_start: dict[str, datetime] = {}
    for entry in entries:
        game = games.setdefault(entry["gameId"], GameStats(entry["gameId"]))
        game.variant = game.variant or entry.get("variantId")
        game.started = game.started or entry["_at"]
        game.ended = entry["_at"]
        if entry["action"] == "CREATE_GAME":
            turn_start[game.game_id] = entry["_at"]
            continue
        player = game.players[entry["playerId"]]
        player.commands += 1
        player.latencies_ms.append(entry.get("durationMicros", 0) / 1000)
        register(entry, game, player, turn_start)
    return games


def register(entry: dict, game: GameStats, player: PlayerStats, turn_start: dict[str, datetime]) -> None:
    outcome, action = entry["outcome"], entry["action"]
    if outcome == "REJECTED":
        label = classify_error(entry.get("detail"))
        player.errors[label] += 1
        if action == "CLOSE":
            player.closes_rejected += 1
            player.rejected_details.append(entry.get("detail") or "")
        return
    if outcome != "OK" or action not in ("END_TURN", "CLOSE"):
        return
    start = turn_start.get(game.game_id)
    if start is not None:
        player.turn_seconds.append((entry["_at"] - start).total_seconds())
    turn_start[game.game_id] = entry["_at"]
    if action == "CLOSE":
        player.closes_ok += 1
        register_close(entry.get("detail") or "", game)


def register_close(detail: str, game: GameStats) -> None:
    game.rounds_closed += 1
    game.final_totals = {pid: int(total) for pid, _penalty, total in CLOSE_RESULT.findall(detail)}
    if "status=FINISHED" in detail:
        game.finished = True
        winner = re.search(r"winner=([\w-]+)", detail)
        game.winner = winner.group(1) if winner else None


# ---------------------------------------------------------------- encuesta y observaciones

def read_csv(path: Path | None) -> list[dict]:
    if path is None or not path.exists():
        return []
    with path.open(encoding="utf-8-sig", newline="") as handle:
        return [row for row in csv.DictReader(handle) if any((value or "").strip() for value in row.values())]


def sus_score(row: dict) -> float | None:
    try:
        answers = [int(row[item]) for item in SUS_ITEMS]
    except (KeyError, TypeError, ValueError):
        return None
    if any(answer < 1 or answer > 5 for answer in answers):
        raise SystemExit(f"Encuesta de {row.get('usuario')}: las respuestas SUS deben estar entre 1 y 5")
    contribution = sum(a - 1 if i % 2 == 0 else 5 - a for i, a in enumerate(answers))
    return contribution * 2.5


def number(row: dict, key: str) -> float | None:
    try:
        return float(str(row.get(key, "")).replace(",", "."))
    except ValueError:
        return None


# ---------------------------------------------------------------- consolidación por participante

@dataclass
class UserResult:
    user: str
    variants: set[str] = field(default_factory=set)
    closed_valid: bool = False
    closes_rejected: int = 0
    errors: dict[str, int] = field(default_factory=lambda: defaultdict(int))
    turn_seconds: list[float] = field(default_factory=list)
    latencies_ms: list[float] = field(default_factory=list)
    observations: list[str] = field(default_factory=list)
    sus: float | None = None
    extras: dict[str, float | None] = field(default_factory=dict)
    tasks_total: int = 0
    tasks_unassisted: int = 0
    games: list[str] = field(default_factory=list)
    rejected_details: list[str] = field(default_factory=list)


def consolidate(games: dict[str, GameStats], users: list[dict], survey: list[dict],
                observations: list[dict]) -> list[UserResult]:
    mapping = users or [{"usuario": f"{gid[:8]}/{pid}", "game_id": gid, "player_id": pid}
                        for gid, game in games.items() for pid in game.players]
    results: dict[str, UserResult] = {}
    for row in mapping:
        result = results.setdefault(row["usuario"], UserResult(row["usuario"]))
        game = games.get(row["game_id"])
        if game is None:
            result.observations.append(f"partida {row['game_id'][:8]} sin registros de auditoría")
            continue
        result.games.append(row["game_id"])
        result.variants.add(game.variant or row.get("modalidad", "?"))
        stats = game.players.get(row["player_id"], PlayerStats())
        result.closed_valid |= stats.closes_ok > 0
        result.closes_rejected += stats.closes_rejected
        result.turn_seconds += stats.turn_seconds
        result.latencies_ms += stats.latencies_ms
        result.rejected_details += stats.rejected_details
        for label, count in stats.errors.items():
            result.errors[label] += count
    for row in survey:
        result = results.setdefault(row["usuario"], UserResult(row["usuario"]))
        result.sus = sus_score(row)
        result.extras = {item: number(row, item) for item in EXTRA_ITEMS}
    for row in observations:
        result = results.setdefault(row["usuario"], UserResult(row["usuario"]))
        if (row.get("tarea") or "").strip():
            result.tasks_total += 1
            result.tasks_unassisted += (row.get("completada_sin_ayuda") or "").strip().lower() in ("si", "sí", "s", "1", "true")
        if (row.get("descripcion") or "").strip():
            result.observations.append(row["descripcion"].strip())
    return sorted(results.values(), key=lambda item: item.user)


# ---------------------------------------------------------------- reporte

def mean(values: list[float]) -> float | None:
    return statistics.fmean(values) if values else None


def percentile(values: list[float], fraction: float) -> float | None:
    if not values:
        return None
    ordered = sorted(values)
    index = max(0, min(len(ordered) - 1, int(round(fraction * len(ordered) + 0.5)) - 1))
    return ordered[index]


def fmt(value: float | None, digits: int = 1, suffix: str = "") -> str:
    return "—" if value is None else f"{value:.{digits}f}".replace(".", ",") + suffix


def summary(results: list[UserResult], games: dict[str, GameStats]) -> dict:
    played = [r for r in results if r.games]
    all_turns = [s for r in played for s in r.turn_seconds]
    all_latency = [ms for game in games.values() for p in game.players.values() for ms in p.latencies_ms]
    sus = [r.sus for r in results if r.sus is not None]
    tasks = sum(r.tasks_total for r in results)
    return {
        "participantes": len(played),
        "tasa_exito_cierre": (sum(r.closed_valid for r in played) / len(played)) if played else None,
        "cierres_rechazados": sum(r.closes_rejected for r in played),
        "turno_media_s": mean(all_turns),
        "turno_mediana_s": statistics.median(all_turns) if all_turns else None,
        "latencia_p50_ms": percentile(all_latency, 0.50),
        "latencia_p95_ms": percentile(all_latency, 0.95),
        "sus_promedio": mean(sus),
        "E1_conteo": mean([r.extras.get("E1") for r in results if r.extras.get("E1") is not None]),
        "E2_rapidez": mean([r.extras.get("E2") for r in results if r.extras.get("E2") is not None]),
        "E3_satisfaccion": mean([r.extras.get("E3") for r in results if r.extras.get("E3") is not None]),
        "E4_rechazo_entendido": mean([r.extras.get("E4") for r in results if r.extras.get("E4") is not None]),
        "tareas_sin_ayuda": (sum(r.tasks_unassisted for r in results) / tasks) if tasks else None,
        "partidas": len(games),
        "partidas_terminadas": sum(game.finished for game in games.values()),
    }


def goal(value: float | None, ok: bool) -> str:
    return "sin datos" if value is None else ("✅ cumple" if ok else "❌ no cumple")


def render(results: list[UserResult], games: dict[str, GameStats], sources: list[str]) -> str:
    s = summary(results, games)
    lines = ["# Resultados de la validación con usuarios", "",
             f"Generado por `scripts/validacion_metricas.py` el {datetime.now().strftime('%Y-%m-%d %H:%M')} "
             f"a partir de: {', '.join(sources)}.", "",
             "## 3.3 Tabla consolidada de métricas de usuario", "",
             "| ID Usuario | Modalidad probada | ¿Logró cerrar ronda válida? | Errores / confusiones detectadas "
             "| Tiempo promedio por turno | Satisfacción (1-5) | SUS (0-100) |",
             "| :-- | :-- | :-- | :-- | --: | --: | --: |"]
    for r in results:
        variants = " y ".join(VARIANT_LABEL.get(v, v) for v in sorted(r.variants)) or "—"
        issues = [f"{label} ×{count}" for label, count in sorted(r.errors.items())] + r.observations
        lines.append(f"| {r.user} | {variants} | {'Sí' if r.closed_valid else 'No'} | "
                     f"{'; '.join(issues) or 'Sin errores registrados'} | {fmt(mean(r.turn_seconds), 1, ' s')} | "
                     f"{fmt(r.extras.get('E3'), 1, ' / 5')} | {fmt(r.sus, 1)} |")
    lines += ["", "## Indicadores frente a las metas del protocolo", "",
              "| Métrica | Resultado | Meta | Estado |", "| :-- | --: | :-- | :-- |",
              f"| M1 Tasa de éxito en cierre | {fmt(pct(s['tasa_exito_cierre']), 0, ' %')} | ≥ 80 % | "
              f"{goal(s['tasa_exito_cierre'], (s['tasa_exito_cierre'] or 0) >= 0.8)} |",
              f"| M2 Cierres rechazados (total) | {s['cierres_rechazados']} | documentar | — |",
              f"| M3 Tiempo por turno (media · mediana) | {fmt(s['turno_media_s'], 1, ' s')} · "
              f"{fmt(s['turno_mediana_s'], 1, ' s')} | reportar | — |",
              f"| M4 Latencia del servidor p50 · p95 | {fmt(s['latencia_p50_ms'], 1, ' ms')} · "
              f"{fmt(s['latencia_p95_ms'], 1, ' ms')} | p95 < 200 ms | "
              f"{goal(s['latencia_p95_ms'], (s['latencia_p95_ms'] or 0) < 200)} |",
              f"| M5 Rapidez percibida (E2) | {fmt(s['E2_rapidez'], 1)} | ≥ 4 | {goal(s['E2_rapidez'], (s['E2_rapidez'] or 0) >= 4)} |",
              f"| M6 Claridad del conteo (E1) | {fmt(s['E1_conteo'], 1)} | ≥ 4 | {goal(s['E1_conteo'], (s['E1_conteo'] or 0) >= 4)} |",
              f"| M7 SUS promedio | {fmt(s['sus_promedio'], 1)} | ≥ 68 | {goal(s['sus_promedio'], (s['sus_promedio'] or 0) >= 68)} |",
              f"| M8 Satisfacción general (E3) | {fmt(s['E3_satisfaccion'], 1)} | ≥ 4 | "
              f"{goal(s['E3_satisfaccion'], (s['E3_satisfaccion'] or 0) >= 4)} |",
              f"| M9 Tareas sin ayuda | {fmt(pct(s['tareas_sin_ayuda']), 0, ' %')} | ≥ 80 % | "
              f"{goal(s['tareas_sin_ayuda'], (s['tareas_sin_ayuda'] or 0) >= 0.8)} |",
              "", f"Participantes con partidas registradas: **{s['participantes']}** · partidas: {s['partidas']} "
              f"(terminadas: {s['partidas_terminadas']}).", "",
              "## Detalle por partida", "",
              "| Partida | Modalidad | Rondas cerradas | ¿Terminó? | Ganador | Totales finales |",
              "| :-- | :-- | --: | :-- | :-- | :-- |"]
    for game in sorted(games.values(), key=lambda item: item.started or datetime.min.replace(tzinfo=timezone.utc)):
        totals = ", ".join(f"{pid}: {total}" for pid, total in sorted(game.final_totals.items())) or "—"
        lines.append(f"| `{game.game_id[:8]}` | {VARIANT_LABEL.get(game.variant, game.variant)} | {game.rounds_closed} | "
                     f"{'Sí' if game.finished else 'No'} | {game.winner or '—'} | {totals} |")
    rejected = [(r.user, detail) for r in results for detail in r.rejected_details]
    lines += ["", "## Cierres rechazados por el motor de reglas", ""]
    lines += [f"- **{user}:** {detail.split(': ', 1)[-1]}" for user, detail in rejected] or ["- Ninguno."]
    return "\n".join(lines) + "\n"


def pct(value: float | None) -> float | None:
    return None if value is None else value * 100


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    parser.add_argument("--audit", type=Path, required=True, help="archivo .jsonl o carpeta con varios")
    parser.add_argument("--usuarios", type=Path, help="usuarios.csv (usuario,game_id,player_id,...)")
    parser.add_argument("--encuesta", type=Path, help="encuesta.csv (usuario,S1..S10,E1..E5,...)")
    parser.add_argument("--observaciones", type=Path, help="observaciones.csv")
    parser.add_argument("--salida", type=Path, help="archivo Markdown de salida (por defecto, consola)")
    parser.add_argument("--json", type=Path, help="también escribe el resumen en JSON")
    args = parser.parse_args(argv)

    entries = read_audit(args.audit)
    games = analyse_games(entries)
    users = read_csv(args.usuarios)
    results = consolidate(games, users, read_csv(args.encuesta), read_csv(args.observaciones))
    sources = [str(path) for path in (args.audit, args.usuarios, args.encuesta, args.observaciones) if path]
    report = render(results, games, sources)
    if args.salida:
        args.salida.write_text(report, encoding="utf-8")
        print(f"Reporte escrito en {args.salida} ({len(entries)} eventos, {len(games)} partidas)")
    else:
        print(report)
    if args.json:
        args.json.write_text(json.dumps(summary(results, games), indent=2, ensure_ascii=False), encoding="utf-8")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
