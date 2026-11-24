package com.apuntado.application;

import com.apuntado.application.command.CreateGameCommand;
import com.apuntado.application.command.PlayerInput;
import com.apuntado.application.port.IGameRepository;
import com.apuntado.application.port.IRealtimePublisher;
import com.apuntado.application.view.CardView;
import com.apuntado.application.view.GameView;
import com.apuntado.application.view.PlayerView;
import com.apuntado.application.view.RoundResultView;
import com.apuntado.application.view.TurnView;
import com.apuntado.application.view.VariantView;
import com.apuntado.bootstrap.RuleRegistry;
import com.apuntado.domain.model.Card;
import com.apuntado.domain.model.GameSession;
import com.apuntado.domain.model.GameStatus;
import com.apuntado.domain.model.PlayerHandState;
import com.apuntado.domain.model.PlayerSeat;
import com.apuntado.domain.model.RoundState;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;

/** Casos de uso del MVP: coordina dominio, reglas y persistencia, y proyecta vistas autorizadas. */
public final class GameApplicationService {
 private static final int MIN_PLAYERS = 2;
 private static final int MAX_PLAYERS = 4;
 private static final System.Logger LOGGER = System.getLogger(GameApplicationService.class.getName());
 private final IGameRepository repository;
 private final TurnOrchestratorComponent orchestrator;
 private final RuleRegistry rules;
 private final IRealtimePublisher publisher;

 public GameApplicationService(IGameRepository repository, TurnOrchestratorComponent orchestrator, RuleRegistry rules,
  IRealtimePublisher publisher) {
  this.repository = Objects.requireNonNull(repository);
  this.orchestrator = Objects.requireNonNull(orchestrator);
  this.rules = Objects.requireNonNull(rules);
  this.publisher = Objects.requireNonNull(publisher);
 }

 public List<VariantView> variants() {
  return rules.ids().stream().map(VariantView::new).toList();
 }

 public GameView createGame(CreateGameCommand command) {
  Objects.requireNonNull(command, "command");
  String variantId = requireText(command.variantId(), "variante");
  rules.get(variantId);
  List<PlayerSeat> seats = toSeats(command.players());
  String gameId = UUID.randomUUID().toString();
  GameSession session = orchestrator.startGame(gameId, variantId, seats);
  repository.create(session);
  notify(session, "GAME_CREATED");
  return view(session, seats.get(0).playerId());
 }

 public GameView getGame(String gameId, String viewerId) {
  GameSession session = load(gameId);
  return view(session, viewerOf(session, viewerId));
 }

 public GameView drawFromDeck(String gameId, String playerId, String commandId) {
  return mutate(gameId, playerId, commandId, "CARD_DRAWN", session -> orchestrator.drawFromDeck(session, playerId));
 }

 public GameView drawFromDiscard(String gameId, String playerId, String commandId) {
  return mutate(gameId, playerId, commandId, "CARD_DRAWN", session -> orchestrator.drawFromDiscard(session, playerId));
 }

 public GameView discard(String gameId, String playerId, String cardId, String commandId) {
  String card = requireText(cardId, "carta");
  return mutate(gameId, playerId, commandId, "CARD_DISCARDED", session -> orchestrator.discard(session, playerId, card));
 }

 public GameView endTurn(String gameId, String playerId, String commandId) {
  return mutate(gameId, playerId, commandId, "TURN_ENDED", session -> orchestrator.endTurn(session, playerId));
 }

 public GameView closeRound(String gameId, String playerId, String commandId) {
  return mutate(gameId, playerId, commandId, "ROUND_CLOSED", session -> orchestrator.closeRound(session, playerId));
 }

 private GameView mutate(String gameId, String playerId, String commandId, String eventType,
  UnaryOperator<GameSession> operation) {
  String player = requireText(playerId, "jugador");
  String receiptId = requireText(commandId, "commandId");
  Optional<GameSession> receipt = repository.receipt(gameId, receiptId);
  if (receipt.isPresent()) return view(receipt.get(), player);
  GameSession previous = load(gameId);
  GameSession updated = operation.apply(previous);
  repository.save(previous, updated, receiptId);
  GameView result = view(updated, player);
  notify(updated, eventType);
  return result;
 }

 /** Publica después del commit; un fallo de notificación no revierte ni falla el comando. */
 private void notify(GameSession session, String eventType) {
  List<GameView> views = session.players().stream()
   .map(player -> view(session, player.playerId())).toList();
  try {
   publisher.publish(session.id(), eventType, views);
  } catch (RuntimeException exception) {
   LOGGER.log(System.Logger.Level.WARNING,
    "Fallo al publicar " + eventType + " de la partida " + session.id(), exception);
  }
 }

 private GameView view(GameSession session, String viewerId) {
  requirePlayer(session, viewerId);
  List<PlayerView> players = session.players().stream().map(player -> playerView(session, player)).toList();
  List<CardView> hand = handOf(session, viewerId).stream().map(GameApplicationService::cardView).toList();
  return new GameView(session.id(), session.variantId(), session.status().name(), session.currentRound(),
   session.version(), viewerId, turnView(session), session.winnerId(), players, hand,
   session.round().deck().size(), session.round().discard().size(), discardTop(session), lastResults(session));
 }

 private PlayerView playerView(GameSession session, PlayerSeat player) {
  boolean currentTurn = session.status() == GameStatus.ACTIVE && session.turn().playerId().equals(player.playerId());
  return new PlayerView(player.playerId(), player.displayName(), player.seat(), session.scoreOf(player.playerId()).total(),
   session.scoreOf(player.playerId()).eliminated(), handOf(session, player.playerId()).size(), currentTurn);
 }

 private TurnView turnView(GameSession session) {
  return new TurnView(session.turn().playerId(), session.turn().number(), session.turn().phase().name());
 }

 private CardView discardTop(GameSession session) {
  List<Card> discard = session.round().discard();
  return discard.isEmpty() ? null : cardView(discard.get(discard.size() - 1));
 }

 private List<RoundResultView> lastResults(GameSession session) {
  List<RoundState> rounds = session.completedRounds();
  if (rounds.isEmpty()) return List.of();
  return rounds.get(rounds.size() - 1).scores().stream()
   .map(score -> new RoundResultView(score.playerId(), score.penalty(), score.totalAfter())).toList();
 }

 private List<Card> handOf(GameSession session, String playerId) {
  return session.round().hands().stream().filter(hand -> hand.playerId().equals(playerId))
   .findFirst().map(PlayerHandState::cards).orElseGet(List::of);
 }

 private List<PlayerSeat> toSeats(List<PlayerInput> inputs) {
  if (inputs.size() < MIN_PLAYERS || inputs.size() > MAX_PLAYERS) {
   throw new IllegalArgumentException("Participan de 2 a 4 jugadores");
  }
  List<PlayerInput> ordered = new ArrayList<>(inputs);
  ordered.sort(Comparator.comparingInt(PlayerInput::seat));
  List<PlayerSeat> seats = new ArrayList<>();
  for (int index = 0; index < ordered.size(); index++) {
   PlayerInput input = Objects.requireNonNull(ordered.get(index), "jugador");
   if (input.seat() != index) throw new IllegalArgumentException("Las sillas deben ser consecutivas desde cero");
   seats.add(new PlayerSeat(requireText(input.playerId(), "jugador"), input.seat(),
    requireText(input.displayName(), "nombre")));
  }
  return List.copyOf(seats);
 }

 private String viewerOf(GameSession session, String viewerId) {
  if (viewerId == null || viewerId.isBlank()) return session.turn().playerId();
  return viewerId;
 }

 private void requirePlayer(GameSession session, String playerId) {
  if (session.players().stream().noneMatch(player -> player.playerId().equals(playerId))) {
   throw new IllegalArgumentException("El jugador no pertenece a la partida: " + playerId);
  }
 }

 private GameSession load(String gameId) {
  return repository.findById(requireText(gameId, "partida")).orElseThrow(() -> new GameNotFoundException(gameId));
 }

 private static CardView cardView(Card card) {
  return new CardView(card.id(), card.rank(), card.suit(), card.points());
 }

 private static String requireText(String value, String field) {
  if (value == null || value.isBlank()) throw new IllegalArgumentException("Campo obligatorio: " + field);
  return value;
 }
}
