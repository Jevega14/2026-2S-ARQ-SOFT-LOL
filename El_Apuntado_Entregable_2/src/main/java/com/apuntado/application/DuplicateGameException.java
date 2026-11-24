package com.apuntado.application;

public class DuplicateGameException extends RuntimeException {
 public DuplicateGameException(String gameId) {
  super("Ya existe una partida con el identificador: " + gameId);
 }
}
