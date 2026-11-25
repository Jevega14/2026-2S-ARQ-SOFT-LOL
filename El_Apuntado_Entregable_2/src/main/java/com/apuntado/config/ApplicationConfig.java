package com.apuntado.config;

import com.apuntado.application.GameApplicationService;
import com.apuntado.application.TurnOrchestratorComponent;
import com.apuntado.application.port.IAuditTrail;
import com.apuntado.application.port.IGameRepository;
import com.apuntado.application.port.IRealtimePublisher;
import com.apuntado.audit.JsonLinesAuditTrail;
import com.apuntado.bootstrap.RuleRegistry;
import com.apuntado.domain.gameplay.HandEvaluatorComponent;
import com.apuntado.domain.model.Card;
import com.apuntado.domain.model.StandardDeck;
import com.apuntado.domain.scoring.RoundScoringComponent;
import com.apuntado.persistence.InMemoryGameRepository;
import com.apuntado.persistence.mongo.MongoGameRepository;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoTemplate;

/** Composition root: única capa que conoce implementaciones concretas de varias capas. */
@Configuration
public class ApplicationConfig {

 @Bean
 public RuleRegistry ruleRegistry() {
  return new RuleRegistry();
 }

 @Bean
 public RoundScoringComponent roundScoringComponent() {
  return new RoundScoringComponent();
 }

 @Bean
 public HandEvaluatorComponent handEvaluatorComponent() {
  return new HandEvaluatorComponent();
 }

 /** {@code apuntado.deck=random} (por defecto) o {@code demo} para la sustentación en vivo. */
 @Bean
 public Supplier<List<Card>> deckSupplier(@Value("${apuntado.deck:random}") String mode) {
  if ("demo".equalsIgnoreCase(mode)) return new DemoDeckSupplier();
  if (!"random".equalsIgnoreCase(mode)) throw new IllegalArgumentException("apuntado.deck debe ser random o demo");
  return StandardDeck::shuffled;
 }

 @Bean
 public TurnOrchestratorComponent turnOrchestratorComponent(HandEvaluatorComponent evaluator,
  RuleRegistry rules, RoundScoringComponent scoring, Supplier<List<Card>> deckSupplier) {
  return new TurnOrchestratorComponent(evaluator, rules, scoring, deckSupplier);
 }

 @Bean
 @Profile("!mongo")
 public IGameRepository gameRepository() {
  return new InMemoryGameRepository();
 }

 @Bean
 @Profile("mongo")
 public IGameRepository mongoGameRepository(MongoTemplate template) {
  return new MongoGameRepository(template);
 }

 @Bean
 public IAuditTrail auditTrail() {
  return new JsonLinesAuditTrail();
 }

 @Bean
 public GameApplicationService gameApplicationService(IGameRepository repository,
  TurnOrchestratorComponent orchestrator, RuleRegistry rules, IRealtimePublisher publisher, IAuditTrail auditTrail) {
  return new GameApplicationService(repository, orchestrator, rules, publisher, auditTrail);
 }
}
