package com.apuntado.presentation;

import com.apuntado.application.CommandConflictException;
import com.apuntado.application.DuplicateGameException;
import com.apuntado.application.GameNotFoundException;
import com.apuntado.application.OptimisticLockException;
import com.apuntado.presentation.dto.ApiError;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

 @ExceptionHandler(GameNotFoundException.class)
 public ResponseEntity<ApiError> notFound(GameNotFoundException exception) {
  return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(exception.getMessage()));
 }

 @ExceptionHandler({OptimisticLockException.class, DuplicateGameException.class, CommandConflictException.class,
  IllegalStateException.class})
 public ResponseEntity<ApiError> conflict(RuntimeException exception) {
  return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(exception.getMessage()));
 }

 @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class})
 public ResponseEntity<ApiError> badRequest(RuntimeException exception) {
  return ResponseEntity.badRequest().body(new ApiError(exception.getMessage()));
 }

 @ExceptionHandler(MethodArgumentNotValidException.class)
 public ResponseEntity<ApiError> invalidBody(MethodArgumentNotValidException exception) {
  String message = exception.getBindingResult().getFieldErrors().stream()
   .map(error -> error.getField() + ": " + error.getDefaultMessage()).collect(Collectors.joining(", "));
  return ResponseEntity.badRequest().body(new ApiError(message));
 }
}
