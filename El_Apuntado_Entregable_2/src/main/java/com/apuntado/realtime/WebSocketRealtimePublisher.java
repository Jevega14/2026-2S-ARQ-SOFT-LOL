package com.apuntado.realtime;

import com.apuntado.application.port.IRealtimePublisher;
import com.apuntado.application.view.GameView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Objects;

/** Adaptador WebSocket del puerto de notificación. */
public final class WebSocketRealtimePublisher implements IRealtimePublisher {
 private final GameWebSocketHandler handler;
 private final ObjectMapper mapper;

 public WebSocketRealtimePublisher(GameWebSocketHandler handler, ObjectMapper mapper) {
  this.handler = Objects.requireNonNull(handler);
  this.mapper = Objects.requireNonNull(mapper);
 }

 @Override
 public void publish(String gameId, String eventType, List<GameView> views) {
  for (GameView view : views) {
   handler.send(gameId, view.viewerId(), serialize(eventType, view));
  }
 }

 private String serialize(String eventType, GameView view) {
  try {
   return mapper.writeValueAsString(new GameEventMessage(eventType, view));
  } catch (JsonProcessingException exception) {
   throw new IllegalStateException("No se pudo serializar la notificación", exception);
  }
 }
}
