package com.apuntado.config;

import com.apuntado.application.GameApplicationService;
import com.apuntado.application.TurnOrchestratorComponent;
import com.apuntado.application.port.IGameRepository;
import com.apuntado.application.port.IRealtimePublisher;
import com.apuntado.bootstrap.RuleRegistry;
import com.apuntado.domain.gameplay.HandEvaluatorComponent;
import com.apuntado.domain.scoring.RoundScoringComponent;
import com.apuntado.persistence.InMemoryGameRepository;
import com.apuntado.persistence.mongo.MongoGameRepository;
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

 @Bean
 public TurnOrchestratorComponent turnOrchestratorComponent(HandEvaluatorComponent evaluator,
  RuleRegistry rules, RoundScoringComponent scoring) {
  return new TurnOrchestratorComponent(evaluator, rules, scoring);
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
 public GameApplicationService gameApplicationService(IGameRepository repository,
  TurnOrchestratorComponent orchestrator, RuleRegistry rules, IRealtimePublisher publisher) {
  return new GameApplicationService(repository, orchestrator, rules, publisher);
 }
}
