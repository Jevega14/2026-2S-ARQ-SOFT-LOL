package com.apuntado.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoTemplate;

/** Configuración explícita de MongoDB; solo se activa con el perfil {@code mongo}. */
@Configuration
@Profile("mongo")
public class MongoConfig {

 @Bean
 public MongoClient mongoClient(@Value("${spring.data.mongodb.uri:mongodb://localhost:27017}") String uri) {
  return MongoClients.create(uri);
 }

 @Bean
 public MongoTemplate mongoTemplate(MongoClient client,
  @Value("${spring.data.mongodb.database:apuntado}") String database) {
  return new MongoTemplate(client, database);
 }
}
