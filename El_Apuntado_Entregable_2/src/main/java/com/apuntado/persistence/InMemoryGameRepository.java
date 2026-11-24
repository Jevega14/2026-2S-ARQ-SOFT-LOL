package com.apuntado.persistence;

import com.apuntado.application.DuplicateGameException;
import com.apuntado.application.GameNotFoundException;
import com.apuntado.application.OptimisticLockException;
import com.apuntado.application.port.IGameRepository;
import com.apuntado.domain.model.GameSession;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementación en memoria del agregado partida.
 * Aplica control de concurrencia optimista por versión e idempotencia por {@code commandId}.
 * Es una copia de trabajo: en producción MongoDB será la fuente de verdad (ADR-04).
 */
public final class InMemoryGameRepository implements IGameRepository {
 private final Map<String, GameSession> games = new ConcurrentHashMap<>();
 private final Map<String, GameSession> receipts = new ConcurrentHashMap<>();

 @Override
 public Optional<GameSession> findById(String gameId) {
  if (gameId == null || gameId.isBlank()) return Optional.empty();
  return Optional.ofNullable(games.get(gameId));
 }

 @Override
 public synchronized void create(GameSession session) {
  Objects.requireNonNull(session, "session");
  if (games.putIfAbsent(session.id(), session) != null) throw new DuplicateGameException(session.id());
 }

 @Override
 public synchronized void save(GameSession previous, GameSession updated, String commandId) {
  Objects.requireNonNull(previous, "previous");
  Objects.requireNonNull(updated, "updated");
  String receiptId = Objects.requireNonNull(commandId, "commandId");
  GameSession current = games.get(previous.id());
  if (current == null) throw new GameNotFoundException(previous.id());
  if (current.version() != previous.version()) throw new OptimisticLockException(previous.id());
  if (updated.version() != previous.version() + 1) {
   throw new IllegalArgumentException("La actualización debe incrementar la versión en uno");
  }
  games.put(updated.id(), updated);
  receipts.put(receiptKey(updated.id(), receiptId), updated);
 }

 @Override
 public Optional<GameSession> receipt(String gameId, String commandId) {
  if (gameId == null || commandId == null) return Optional.empty();
  return Optional.ofNullable(receipts.get(receiptKey(gameId, commandId)));
 }

 private static String receiptKey(String gameId, String commandId) {
  return gameId + "::" + commandId;
 }
}
