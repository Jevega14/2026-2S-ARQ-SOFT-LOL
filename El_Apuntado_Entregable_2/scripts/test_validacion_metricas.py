"""Pruebas del script de métricas. Los datos de aquí son SINTÉTICOS y solo verifican los cálculos;
no son resultados de usuarios. Ejecutar: python3 -m unittest discover -s scripts"""
import json
import tempfile
import unittest
from datetime import datetime, timedelta, timezone
from pathlib import Path

import validacion_metricas as vm

T0 = datetime(2026, 11, 28, 15, 0, 0, tzinfo=timezone.utc)
REJECT = "IllegalArgumentException: Cierre inválido: se requiere tres ternas y una carta sobrante de menos de 5 puntos"


def event(seconds, player, action, outcome="OK", detail=None, game="g1", micros=8000):
    at = (T0 + timedelta(seconds=seconds)).isoformat().replace("+00:00", "") + ".123456789Z"
    return {"at": at, "gameId": game, "variantId": "APUNTADO101", "playerId": player, "action": action,
            "commandId": f"c{seconds}", "outcome": outcome, "version": 1, "round": 1, "turn": 1,
            "durationMicros": micros, "detail": detail}


SESSION = [
    event(0, "p1", "CREATE_GAME", detail="action=CREATE_GAME players=p1=A,p2=B"),
    event(5, "p1", "DRAW_DECK"), event(10, "p1", "DISCARD"), event(12, "p1", "END_TURN"),
    event(20, "p2", "DRAW_DISCARD"), event(25, "p2", "DISCARD"),
    event(26, "p2", "CLOSE", "REJECTED", REJECT), event(30, "p2", "END_TURN"),
    event(35, "p1", "DRAW_DECK"), event(40, "p1", "DISCARD"),
    event(42, "p1", "CLOSE", detail="closedRound=1 closer=p1 results=p1:+0=0,p2:+66=66 status=ACTIVE"),
    event(50, "p2", "DRAW_DECK", micros=150000), event(55, "p2", "DISCARD"), event(60, "p2", "END_TURN"),
    event(61, "p2", "DRAW_DECK", "REJECTED", "IllegalStateException: Movimiento fuera de turno"),
    event(65, "p1", "DRAW_DECK"), event(68, "p1", "DISCARD"),
    event(70, "p1", "CLOSE", detail="closedRound=2 closer=p1 results=p1:+0=0,p2:+40=106 status=FINISHED winner=p1"),
]


class MetricsTest(unittest.TestCase):
    def setUp(self):
        self.tmp = Path(tempfile.mkdtemp())
        (self.tmp / "audit.jsonl").write_text("\n".join(json.dumps(e) for e in SESSION) + "\n", encoding="utf-8")
        (self.tmp / "usuarios.csv").write_text(
            "usuario,game_id,player_id,modalidad\nU01,g1,p1,APUNTADO101\nU02,g1,p2,APUNTADO101\n", encoding="utf-8")
        (self.tmp / "encuesta.csv").write_text(
            "usuario,S1,S2,S3,S4,S5,S6,S7,S8,S9,S10,E1,E2,E3,E4,E5\n"
            "U01,5,1,5,1,5,1,5,1,5,1,5,4,4.5,5,4\n"
            "U02,3,3,3,3,3,3,3,3,3,3,3,5,4,2,3\n", encoding="utf-8")
        (self.tmp / "observaciones.csv").write_text(
            "usuario,tarea,completada_sin_ayuda,ayudas,minuto,tipo,descripcion,cita\n"
            "U02,T3a,No,1,9,regla,No entendía por qué no podía cerrar con 6 pts sobrantes,\n"
            "U02,T2,Sí,0,,,,\nU01,T2,Sí,0,,,,\nU01,T3a,Sí,0,,,,\n", encoding="utf-8")

    def run_script(self):
        out = self.tmp / "resultados.md"
        summary = self.tmp / "resumen.json"
        vm.main(["--audit", str(self.tmp / "audit.jsonl"), "--usuarios", str(self.tmp / "usuarios.csv"),
                 "--encuesta", str(self.tmp / "encuesta.csv"), "--observaciones", str(self.tmp / "observaciones.csv"),
                 "--salida", str(out), "--json", str(summary)])
        return out.read_text(encoding="utf-8"), json.loads(summary.read_text(encoding="utf-8"))

    def test_parses_java_instants_with_nanoseconds(self):
        self.assertEqual(vm.parse_instant("2026-11-28T15:00:05.123456789Z"),
                         datetime(2026, 11, 28, 15, 0, 5, 123456, tzinfo=timezone.utc))
        self.assertEqual(vm.parse_instant("2026-11-28T15:00:05Z").second, 5)

    def test_turn_time_runs_from_receiving_the_turn_to_ending_or_closing_it(self):
        games = vm.analyse_games(vm.read_audit(self.tmp / "audit.jsonl"))
        self.assertEqual([12, 12, 10], games["g1"].players["p1"].turn_seconds)
        self.assertEqual([18, 18], games["g1"].players["p2"].turn_seconds)

    def test_game_outcome_and_final_totals(self):
        game = vm.analyse_games(vm.read_audit(self.tmp / "audit.jsonl"))["g1"]
        self.assertEqual(2, game.rounds_closed)
        self.assertTrue(game.finished)
        self.assertEqual("p1", game.winner)
        self.assertEqual({"p1": 0, "p2": 106}, game.final_totals)

    def test_sus_scoring(self):
        self.assertEqual(100.0, vm.sus_score({f"S{i}": (5 if i % 2 else 1) for i in range(1, 11)}))
        self.assertEqual(50.0, vm.sus_score({f"S{i}": 3 for i in range(1, 11)}))
        self.assertIsNone(vm.sus_score({"S1": "5"}))

    def test_consolidated_table_and_goals(self):
        report, summary = self.run_script()
        self.assertIn("| U01 | Apuntado (101) | Sí | Sin errores registrados | 11,3 s | 4,5 / 5 | 100,0 |", report)
        self.assertIn("| U02 | Apuntado (101) | No | cierre inválido (regla) ×1; movimiento fuera de turno ×1; "
                      "No entendía por qué no podía cerrar con 6 pts sobrantes | 18,0 s | 4,0 / 5 | 50,0 |", report)
        self.assertIn("Cierre inválido: se requiere tres ternas", report)
        self.assertEqual(0.5, summary["tasa_exito_cierre"])
        self.assertEqual(75.0, summary["sus_promedio"])
        self.assertEqual(0.75, summary["tareas_sin_ayuda"])
        self.assertEqual(1, summary["partidas_terminadas"])
        self.assertIn("❌ no cumple", report)

    def test_without_user_mapping_reports_each_seat(self):
        out = self.tmp / "solo-audit.md"
        vm.main(["--audit", str(self.tmp), "--salida", str(out)])
        report = out.read_text(encoding="utf-8")
        self.assertIn("| g1/p1 |", report)
        self.assertIn("| g1/p2 |", report)


if __name__ == "__main__":
    unittest.main()
