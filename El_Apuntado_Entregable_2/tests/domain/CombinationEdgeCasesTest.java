package com.apuntado.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.apuntado.domain.model.Card;
import com.apuntado.domain.model.Hand;
import com.apuntado.domain.rules.apuntado.ReglaApuntado;
import com.apuntado.domain.rules.clasica.ReglaClasica;
import com.apuntado.domain.rules.common.Groups;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Checklist de pruebas unitarias y funcionales (Entregable 3, sección 2.3): casos borde de
 * combinaciones del perfil MVP1. Las cartas se escriben como rango + palo (1 = As, 11 = J, 12 = Q, 13 = K).
 */
class CombinationEdgeCasesTest {
 private final ReglaClasica clasica = new ReglaClasica();
 private final ReglaApuntado apuntado = new ReglaApuntado();

 private static Card card(String id) {
  int rank = Integer.parseInt(id.substring(0, id.length() - 1));
  return new Card(id, rank, id.substring(id.length() - 1), rank);
 }

 private static List<Card> cards(String... ids) {
  return Arrays.stream(ids).map(CombinationEdgeCasesTest::card).toList();
 }

 private static Hand hand(String... ids) {
  return new Hand(cards(ids));
 }

 @ParameterizedTest(name = "grupo válido: {0}")
 @CsvSource({
  "7H 7D 7S", "7H 7D 7S 7C", "5H 6H 7H", "1S 2S 3S", "10C 11C 12C 13C", "11D 12D 13D"
 })
 void validGroups(String group) {
  assertTrue(Groups.valid(cards(group.split(" "))));
 }

 @ParameterizedTest(name = "grupo inválido: {0}")
 @CsvSource({
  "7H 7D 8S", "5H 6D 7H", "5H 7H 8H", "12S 13S 1S", "13H 1H 2H", "2C 3C 5C 6C"
 })
 void invalidGroups(String group) {
  assertFalse(Groups.valid(cards(group.split(" "))));
 }

 @Test
 @DisplayName("Trío con palo repetido no es grupo aunque las identidades difieran")
 void sameRankWithRepeatedSuitIsInvalid() {
  List<Card> group = List.of(new Card("7H", 7, "H", 7), new Card("7H-b", 7, "H", 7), new Card("7S", 7, "S", 7));
  assertFalse(Groups.valid(group));
 }

 @Test
 @DisplayName("Clásica acepta la cuarta formada por una escalera")
 void classicAcceptsARunAsTheFourGroup() {
  assertTrue(clasica.permiteCerrar(hand("3H", "4H", "5H", "6H", "9C", "9D", "9S", "12H", "12D", "12C")));
 }

 @Test
 @DisplayName("Clásica: tres ternas incompletas (dos ternas y cartas sueltas) no cierran")
 void classicRejectsIncompleteTriples() {
  assertFalse(clasica.permiteCerrar(hand("7H", "7D", "7S", "9H", "9D", "9S", "11H", "11D", "2C", "5S")));
 }

 @Test
 @DisplayName("Apuntado: dos ternas y una terna incompleta no cierran aunque el sobrante sea bajo")
 void apuntadoRejectsIncompleteTriples() {
  assertFalse(apuntado.permiteCerrar(hand("7H", "7D", "7S", "9H", "9D", "9S", "11H", "11D", "1C", "2S")));
 }

 @Test
 @DisplayName("Clásica no admite sobrantes: 4-3-3 exige usar las diez cartas")
 void classicRejectsThreeTriplesPlusALowCard() {
  assertFalse(clasica.permiteCerrar(hand("7H", "7D", "7S", "9H", "9D", "9S", "11H", "11D", "11S", "1C")));
 }

 @ParameterizedTest(name = "Apuntado con sobrante de {0} punto(s) → cierra = {1}")
 @CsvSource({"1, true", "2, true", "3, true", "4, true", "5, false", "6, false", "10, false"})
 void apuntadoRemainderMustBeStrictlyLessThanFive(int remainder, boolean closes) {
  String rest = remainder + "C";
  assertEquals(closes, apuntado.permiteCerrar(hand("7H", "7D", "7S", "8H", "8D", "8S", "12H", "12D", "12S", rest)));
 }

 @Test
 @DisplayName("Apuntado: J, Q o K sobrante vale 10 y no permite cerrar")
 void apuntadoFaceRemainderCountsTen() {
  assertFalse(apuntado.permiteCerrar(hand("7H", "7D", "7S", "8H", "8D", "8S", "2H", "2D", "2S", "13C")));
 }

 @Test
 @DisplayName("Apuntado acepta ternas por escalera y elige la partición con sobrante válido")
 void apuntadoFindsTheValidPartition() {
  // 2♥3♥4♥5♥ admite dejar el 5♥ (5 pts, inválido) o el 2♥ (2 pts, válido): el evaluador debe encontrar la segunda.
  assertTrue(apuntado.permiteCerrar(hand("2H", "3H", "4H", "5H", "9C", "9D", "9S", "12H", "12D", "12C")));
 }

 @Test
 @DisplayName("Apuntado: tres ternas disjuntas no pueden compartir cartas")
 void groupsMustBeDisjoint() {
  // 7♥ formaría parte de la terna de sietes y de la escalera 5♥6♥7♥ a la vez: no hay partición válida.
  assertFalse(apuntado.permiteCerrar(hand("7H", "7D", "7S", "5H", "6H", "12H", "12D", "12C", "2C", "3D")));
 }
}
