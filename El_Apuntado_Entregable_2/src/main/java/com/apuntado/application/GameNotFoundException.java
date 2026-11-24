package com.apuntado.application;

public class GameNotFoundException extends RuntimeException {
 public GameNotFoundException(String gameId) {
  super("Partida no encontrada: " + gameId);
 }
}
