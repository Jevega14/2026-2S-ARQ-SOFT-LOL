package com.apuntado.persistence.mongo;

import com.apuntado.application.DuplicateGameException;
import com.apuntado.application.GameNotFoundException;
import com.apuntado.application.OptimisticLockException;
import com.apuntado.application.port.CommandReceipt;
import com.apuntado.application.port.IGameRepository;
import com.apuntado.domain.model.GameSession;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.bson.Document;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

/**
 * Adaptador MongoDB del agregado partida (ADR-04).
 * Guarda rondas, manos y puntajes embebidos, controla concurrencia por versión
 * y registra los {@code commandId} aplicados dentro del mismo documento.
 */
public final class MongoGameRepository implements IGameRepository {
 static final String COLLECTION = "game_sessions";
 static final int MAX_DOCUMENT_BYTES = 12 * 1024 * 1024;
 private final MongoTemplate template;

 public MongoGameRepository(MongoTemplate template) {
  this.template = Objects.requireNonNull(template);
 }

 @Override
 public Optional<GameSession> findById(String gameId) {
  if (gameId == null || gameId.isBlank()) return Optional.empty();
  Document document = template.findById(gameId, Document.class, COLLECTION);
  return Optional.ofNullable(document).map(GameSessionMapper::toDomain);
 }

 @Override
 public void create(GameSession session) {
  Objects.requireNonNull(session, "session");
  Document document = GameSessionMapper.toDocument(session).append(GameSessionMapper.RECEIPTS, List.of());
  ensureSize(document);
  try {
   template.insert(document, COLLECTION);
  } catch (DuplicateKeyException exception) {
   throw new DuplicateGameException(session.id());
  }
 }

 @Override
 public void save(GameSession previous, GameSession updated, CommandReceipt receipt) {
  Objects.requireNonNull(previous, "previous");
  Objects.requireNonNull(updated, "updated");
  Objects.requireNonNull(receipt, "receipt");
  if (updated.version() != previous.version() + 1) {
   throw new IllegalArgumentException("La actualización debe incrementar la versión en uno");
  }
  Query query = Query.query(Criteria.where(GameSessionMapper.ID).is(previous.id())
   .and("version").is(previous.version()));
  Update update = new Update().push(GameSessionMapper.RECEIPTS, GameSessionMapper.receiptToDocument(receipt));
  applyState(update, updated);
  if (template.updateFirst(query, update, COLLECTION).getMatchedCount() == 0) resolveFailure(previous.id());
 }

 @Override
 public Optional<CommandReceipt> receipt(String gameId, String commandId) {
  if (gameId == null || commandId == null) return Optional.empty();
  Query query = Query.query(Criteria.where(GameSessionMapper.ID).is(gameId)
   .and(GameSessionMapper.RECEIPTS + ".commandId").is(commandId));
  query.fields().include(GameSessionMapper.RECEIPTS);
  Document document = template.findOne(query, Document.class, COLLECTION);
  if (document == null) return Optional.empty();
  return document.getList(GameSessionMapper.RECEIPTS, Document.class).stream()
   .filter(receipt -> commandId.equals(receipt.getString("commandId")))
   .findFirst().map(GameSessionMapper::receiptFromDocument);
 }

 private void applyState(Update update, GameSession session) {
  Document state = GameSessionMapper.toDocument(session);
  state.remove(GameSessionMapper.ID);
  ensureSize(state);
  state.forEach(update::set);
 }

 private void resolveFailure(String gameId) {
  if (!template.exists(Query.query(Criteria.where(GameSessionMapper.ID).is(gameId)), COLLECTION)) {
   throw new GameNotFoundException(gameId);
  }
  throw new OptimisticLockException(gameId);
 }

 private static void ensureSize(Document document) {
  int bytes = document.toJson().getBytes(StandardCharsets.UTF_8).length;
  if (bytes > MAX_DOCUMENT_BYTES) {
   throw new IllegalArgumentException("La partida excede el límite BSON de 12 MiB");
  }
 }
}
