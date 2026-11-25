package com.apuntado.tests.realtime;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.apuntado.application.view.CardView;
import com.apuntado.application.view.GameView;
import com.apuntado.application.view.TurnView;
import com.apuntado.realtime.GameWebSocketHandler;
import com.apuntado.realtime.WebSocketRealtimePublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class WebSocketRealtimePublisherTest {

 @Test
 void sendsAnAuthorizedProjectionToEachViewer() {
  GameWebSocketHandler handler = mock(GameWebSocketHandler.class);
  WebSocketRealtimePublisher publisher = new WebSocketRealtimePublisher(handler, new ObjectMapper());
  GameView ana = view("p1", 11);
  GameView luis = view("p2", 10);

  publisher.publish("game-1", "CARD_DRAWN", List.of(ana, luis));

  ArgumentCaptor<String> anaPayload = ArgumentCaptor.forClass(String.class);
  verify(handler).send(eq("game-1"), eq("p1"), anaPayload.capture());
  assertTrue(anaPayload.getValue().contains("\"viewerId\":\"p1\""));
  assertTrue(anaPayload.getValue().contains("\"type\":\"CARD_DRAWN\""));
  assertTrue(anaPayload.getValue().contains("\"hand\":["));
 }

 private GameView view(String viewerId, int handSize) {
  List<CardView> hand = IntStream.range(0, handSize)
   .mapToObj(index -> new CardView(viewerId + "-" + index, 1, "H", 1)).toList();
  return new GameView("game-1", "CLASICA", "ACTIVE", 1, 1, viewerId,
   new TurnView(viewerId, 1, "DRAW"), null, List.of(), hand, 31, 1, null, List.of(), null,
   "una cuarta y dos ternas (4-3-3) sin cartas sobrantes");
 }
}
