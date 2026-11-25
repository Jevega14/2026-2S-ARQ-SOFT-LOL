package com.apuntado.realtime;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * Registro de conexiones WebSocket por partida y jugador.
 * Solo envía a la conexión del jugador destinatario; no conoce reglas ni persistencia.
 */
@Component
public class GameWebSocketHandler extends TextWebSocketHandler {
 private static final System.Logger LOGGER = System.getLogger(GameWebSocketHandler.class.getName());
 private static final String KEY_ATTRIBUTE = "apuntado.sessionKey";
 private final Map<String, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();

 @Override
 public void afterConnectionEstablished(WebSocketSession session) throws Exception {
  Map<String, String> params = query(session.getUri());
  String gameId = params.get("gameId");
  String playerId = params.get("playerId");
  if (isBlank(gameId) || isBlank(playerId)) {
   session.close(CloseStatus.BAD_DATA);
   return;
  }
  String key = key(gameId, playerId);
  sessions.computeIfAbsent(key, ignored -> new CopyOnWriteArraySet<>()).add(session);
  session.getAttributes().put(KEY_ATTRIBUTE, key);
 }

 @Override
 public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
  remove(session);
 }

 public void send(String gameId, String playerId, String payload) {
  Set<WebSocketSession> targets = sessions.get(key(gameId, playerId));
  if (targets == null) return;
  targets.forEach(session -> sendTo(session, payload));
 }

 private void sendTo(WebSocketSession session, String payload) {
  try {
   if (!session.isOpen()) return;
   synchronized (session) {
    session.sendMessage(new TextMessage(payload));
   }
  } catch (IOException exception) {
   // Conexión rota: se descarta la sesión; el cliente se recupera con GET /api/games/{id} al reconectar.
   LOGGER.log(System.Logger.Level.INFO, "Se cierra una sesión WebSocket que no aceptó el mensaje", exception);
   remove(session);
  }
 }

 private void remove(WebSocketSession session) {
  Object key = session.getAttributes().get(KEY_ATTRIBUTE);
  if (!(key instanceof String)) return;
  Set<WebSocketSession> targets = sessions.get((String) key);
  if (targets == null) return;
  targets.remove(session);
  if (targets.isEmpty()) sessions.remove((String) key);
 }

 private static String key(String gameId, String playerId) {
  return gameId + "::" + playerId;
 }

 private static boolean isBlank(String value) {
  return value == null || value.isBlank();
 }

 private static Map<String, String> query(URI uri) {
  Map<String, String> params = new HashMap<>();
  if (uri == null || uri.getQuery() == null) return params;
  for (String pair : uri.getQuery().split("&")) {
   int separator = pair.indexOf('=');
   if (separator <= 0) continue;
   params.put(decode(pair.substring(0, separator)), decode(pair.substring(separator + 1)));
  }
  return params;
 }

 private static String decode(String value) {
  return URLDecoder.decode(value, StandardCharsets.UTF_8);
 }
}
