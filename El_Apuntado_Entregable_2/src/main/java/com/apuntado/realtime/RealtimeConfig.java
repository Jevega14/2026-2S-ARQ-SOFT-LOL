package com.apuntado.realtime;

import com.apuntado.application.port.IRealtimePublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/** Expone el canal WebSocket de notificaciones posterior al commit. */
@Configuration
@EnableWebSocket
public class RealtimeConfig implements WebSocketConfigurer {
 private final GameWebSocketHandler handler;

 public RealtimeConfig(GameWebSocketHandler handler) {
  this.handler = handler;
 }

 @Override
 public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
  registry.addHandler(handler, "/ws/games").setAllowedOriginPatterns("*");
 }

 @Bean
 public IRealtimePublisher realtimePublisher(GameWebSocketHandler gameWebSocketHandler, ObjectMapper mapper) {
  return new WebSocketRealtimePublisher(gameWebSocketHandler, mapper);
 }
}
