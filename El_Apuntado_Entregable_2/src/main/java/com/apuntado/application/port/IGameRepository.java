package com.apuntado.application.port;

import com.apuntado.domain.model.GameSession;
import java.util.Optional;

/** Puerto de persistencia del agregado partida. La implementación concreta vive en infraestructura. */
public interface IGameRepository {

 Optional<GameSession> findById(String gameId);

 void create(GameSession session);

 /**
  * Persiste una actualización solo si la versión almacenada coincide con {@code previous.version()}.
  * El recibo se guarda en la misma escritura para que el comando sea idempotente.
  */
 void save(GameSession previous, GameSession updated, CommandReceipt receipt);

 Optional<CommandReceipt> receipt(String gameId, String commandId);
}
