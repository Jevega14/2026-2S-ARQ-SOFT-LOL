package com.apuntado.application.port;

import com.apuntado.application.view.GameView;
import java.util.List;

/** Puerto de notificación posterior al commit. Las implementaciones concretas viven en infraestructura. */
public interface IRealtimePublisher {

 /**
  * Publica una proyección autorizada por jugador. Se invoca solo después de persistir el cambio.
  */
 void publish(String gameId, String eventType, List<GameView> views);
}
