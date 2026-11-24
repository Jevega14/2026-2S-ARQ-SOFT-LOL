package com.apuntado.tests.realtime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.apuntado.realtime.GameWebSocketHandler;
import java.net.URI;
import java.util.HashMap;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

class GameWebSocketHandlerTest {

 @Test
 void routesMessagesOnlyToTheAddressedPlayer() throws Exception {
  GameWebSocketHandler handler = new GameWebSocketHandler();
  WebSocketSession ana = session("game-1", "p1");
  WebSocketSession luis = session("game-1", "p2");
  handler.afterConnectionEstablished(ana);
  handler.afterConnectionEstablished(luis);

  handler.send("game-1", "p1", "{\"type\":\"CARD_DRAWN\"}");

  verify(ana).sendMessage(any(TextMessage.class));
  verify(luis, never()).sendMessage(any(TextMessage.class));
 }

 @Test
 void rejectsConnectionsWithoutGameOrPlayer() throws Exception {
  GameWebSocketHandler handler = new GameWebSocketHandler();
  WebSocketSession session = mock(WebSocketSession.class);
  when(session.getUri()).thenReturn(URI.create("ws://localhost/ws/games"));
  when(session.getAttributes()).thenReturn(new HashMap<>());

  handler.afterConnectionEstablished(session);

  verify(session).close(CloseStatus.BAD_DATA);
 }

 @Test
 void stopsSendingAfterTheConnectionIsClosed() throws Exception {
  GameWebSocketHandler handler = new GameWebSocketHandler();
  WebSocketSession ana = session("game-1", "p1");
  handler.afterConnectionEstablished(ana);
  handler.afterConnectionClosed(ana, CloseStatus.NORMAL);

  handler.send("game-1", "p1", "{\"type\":\"CARD_DRAWN\"}");

  verify(ana, never()).sendMessage(any(TextMessage.class));
 }

 private WebSocketSession session(String gameId, String playerId) {
  WebSocketSession session = mock(WebSocketSession.class);
  when(session.getUri()).thenReturn(URI.create(
   "ws://localhost/ws/games?gameId=" + gameId + "&playerId=" + playerId));
  when(session.getAttributes()).thenReturn(new HashMap<>());
  when(session.isOpen()).thenReturn(true);
  return session;
 }
}
