package com.apuntado.presentation;

import com.apuntado.application.GameApplicationService;
import com.apuntado.application.command.CreateGameCommand;
import com.apuntado.application.command.PlayerInput;
import com.apuntado.application.view.GameView;
import com.apuntado.application.view.VariantView;
import com.apuntado.presentation.dto.CreateGameRequest;
import com.apuntado.presentation.dto.DiscardRequest;
import com.apuntado.presentation.dto.PlayerActionRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Adaptador HTTP: solo traduce peticiones y delega en la capa de aplicación. */
@RestController
@RequestMapping("/api")
public class GameController {
 private final GameApplicationService service;

 public GameController(GameApplicationService service) {
  this.service = service;
 }

 @GetMapping("/variants")
 public List<VariantView> variants() {
  return service.variants();
 }

 @PostMapping("/games")
 public ResponseEntity<GameView> create(@Valid @RequestBody CreateGameRequest request) {
  GameView view = service.createGame(toCommand(request));
  return ResponseEntity.status(HttpStatus.CREATED).body(view);
 }

 @GetMapping("/games/{gameId}")
 public GameView get(@PathVariable String gameId,
  @RequestParam(name = "viewerId", required = false) String viewerId) {
  return service.getGame(gameId, viewerId);
 }

 @PostMapping("/games/{gameId}/draw-deck")
 public GameView drawFromDeck(@PathVariable String gameId, @Valid @RequestBody PlayerActionRequest request) {
  return service.drawFromDeck(gameId, request.playerId(), request.commandId());
 }

 @PostMapping("/games/{gameId}/draw-discard")
 public GameView drawFromDiscard(@PathVariable String gameId, @Valid @RequestBody PlayerActionRequest request) {
  return service.drawFromDiscard(gameId, request.playerId(), request.commandId());
 }

 @PostMapping("/games/{gameId}/discard")
 public GameView discard(@PathVariable String gameId, @Valid @RequestBody DiscardRequest request) {
  return service.discard(gameId, request.playerId(), request.cardId(), request.commandId());
 }

 @PostMapping("/games/{gameId}/end-turn")
 public GameView endTurn(@PathVariable String gameId, @Valid @RequestBody PlayerActionRequest request) {
  return service.endTurn(gameId, request.playerId(), request.commandId());
 }

 @PostMapping("/games/{gameId}/close")
 public GameView closeRound(@PathVariable String gameId, @Valid @RequestBody PlayerActionRequest request) {
  return service.closeRound(gameId, request.playerId(), request.commandId());
 }

 private static CreateGameCommand toCommand(CreateGameRequest request) {
  List<PlayerInput> players = request.players().stream()
   .map(player -> new PlayerInput(player.playerId(), player.seat(), player.displayName())).toList();
  return new CreateGameCommand(request.variantId(), players);
 }
}
