package com.apuntado.realtime;

import com.apuntado.application.view.GameView;

/** Mensaje enviado al cliente: tipo de evento y proyección autorizada de su jugador. */
public record GameEventMessage(String type, GameView state) {
}
