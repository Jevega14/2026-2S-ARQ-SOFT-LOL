package com.apuntado.application.port;

import com.apuntado.domain.model.GameSession;
import java.util.Optional;

/** Puerto de persistencia del agregado partida. La implementación concreta vive en infraestructura. */
public interface IGameRepository {

 Optional<GameSession> findById(String gameId);

 void create(GameSession session);

 /**
  * Persiste una actualización solo si la versión almacenada coincide con {@code previous.version()}.
  * Registra el recibo de {@code commandId} para que el comando sea idempotente.
  */
 void save(GameSession previous, GameSession updated, String commandId);

 Optional<GameSession> receipt(String gameId, String commandId);
}
