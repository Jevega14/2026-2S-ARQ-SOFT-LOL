package com.apuntado.application;

public class OptimisticLockException extends RuntimeException {
 public OptimisticLockException(String gameId) {
  super("La partida cambió mientras se procesaba el comando: " + gameId);
 }
}
