package com.apuntado.tests.config;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.apuntado.config.ApplicationConfig;
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
}
