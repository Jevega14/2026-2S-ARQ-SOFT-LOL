package com.apuntado.tests.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.apuntado.config.ApplicationConfig;
import com.apuntado.domain.model.Card;
import com.apuntado.domain.model.Hand;
import com.apuntado.domain.rules.apuntado.ReglaApuntado;
import com.apuntado.domain.rules.clasica.ReglaClasica;
import java.util.List;
import com.apuntado.persistence.InMemoryGameRepository;
import com.apuntado.persistence.mongo.MongoGameRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.mongodb.core.MongoTemplate;

class ApplicationConfigTest {

 @Test
 void defaultWiringUsesTheInMemoryRepository() {
  assertInstanceOf(InMemoryGameRepository.class, new ApplicationConfig().gameRepository());
 }

 @Test
 void mongoWiringUsesTheMongoAdapter() {
  MongoTemplate template = Mockito.mock(MongoTemplate.class);
  assertInstanceOf(MongoGameRepository.class, new ApplicationConfig().mongoGameRepository(template));
 }

 @Test
 void demoDeckDealsAHandThatClosesBothVariants() {
  List<Card> deck = new ApplicationConfig().deckSupplier("demo").get();
  assertEquals(52, deck.stream().map(Card::id).distinct().count());
  Hand firstSeat = new Hand(deck.subList(0, 10));
  assertTrue(new ReglaClasica().permiteCerrar(firstSeat));
  assertTrue(new ReglaApuntado().permiteCerrar(firstSeat));
 }

 @Test
 void randomDeckIsTheDefaultAndUnknownModesFailFast() {
  assertEquals(52, new ApplicationConfig().deckSupplier("random").get().size());
  assertThrows(IllegalArgumentException.class, () -> new ApplicationConfig().deckSupplier("trucada"));
 }
}
