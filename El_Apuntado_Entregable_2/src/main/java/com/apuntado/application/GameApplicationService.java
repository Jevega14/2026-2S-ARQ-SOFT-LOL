package com.apuntado.application;

import com.apuntado.application.command.CreateGameCommand;
import com.apuntado.application.command.PlayerInput;
import com.apuntado.application.port.AuditEntry;
import com.apuntado.application.port.CommandReceipt;
import com.apuntado.application.port.IAuditTrail;
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
import com.apuntado.domain.rules.api.IReglaJuego;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.function.UnaryOperator;

/**
 * Casos de uso del MVP: coordina dominio, reglas y persistencia, y proyecta vistas autorizadas.
 * Cada mutación sigue ADR-04: recibo idempotente, escritura con versión esperada, auditoría y
 * notificación posterior al commit.
 */
public final class GameApplicationService {
 private static final int MIN_PLAYERS = 2;
 private static final int MAX_PLAYERS = 4;
 private static final String OK = "OK";
 private static final String REPLAY = "REPLAY";
 private static final String REJECTED = "REJECTED";
 private static final System.Logger LOGGER = System.getLogger(GameApplicationService.class.getName());
 private final IGameRepository repository;
 private final TurnOrchestratorComponent orchestrator;
 private final RuleRegistry rules;
 private final IRealtimePublisher publisher;
 private final IAuditTrail audit;

 private enum Action {
  DRAW_DECK("CARD_DRAWN"), DRAW_DISCARD("CARD_DRAWN"), DISCARD("CARD_DISCARDED"), END_TURN("TURN_ENDED"),
  CLOSE("ROUND_CLOSED");

  private final String eventType;

  Action(String eventType) {
   this.eventType = eventType;
  }
 }

 private record PendingCommand(String gameId, String playerId, String commandId, Action action, String argument) {
  CommandReceipt receipt(int resultVersion) {
   return new CommandReceipt(commandId, playerId, fingerprint(action + "\n" + playerId + "\n" + argument), resultVersion);
  }
 }

 public GameApplicationService(IGameRepository repository, TurnOrchestratorComponent orchestrator, RuleRegistry rules,
  IRealtimePublisher publisher) {
  this(repository, orchestrator, rules, publisher, IAuditTrail.NONE);
 }

 public GameApplicationService(IGameRepository repository, TurnOrchestratorComponent orchestrator, RuleRegistry rules,
  IRealtimePublisher publisher, IAuditTrail audit) {
  this.repository = Objects.requireNonNull(repository);
  this.orchestrator = Objects.requireNonNull(orchestrator);
  this.rules = Objects.requireNonNull(rules);
  this.publisher = Objects.requireNonNull(publisher);
  this.audit = Objects.requireNonNull(audit);
 }

 public List<VariantView> variants() {
  return rules.ids().stream().map(rules::get)
   .map(rule -> new VariantView(rule.id(), rule.descripcionCierre(), rule.terminaPartidaAlCerrar())).toList();
 }

 public GameView createGame(CreateGameCommand command) {
  Objects.requireNonNull(command, "command");
  long started = System.nanoTime();
  String variantId = requireText(command.variantId(), "variante");
  rules.get(variantId);
  List<PlayerSeat> seats = toSeats(command.players());
  String gameId = UUID.randomUUID().toString();
  GameSession session = orchestrator.startGame(gameId, variantId, seats);
  repository.create(session);
  String names = seats.stream().map(seat -> seat.playerId() + "=" + seat.displayName()).collect(Collectors.joining(","));
  record(new PendingCommand(gameId, seats.get(0).playerId(), "create-" + gameId, null, null), session, OK, started,
   "action=CREATE_GAME players=" + names);
  notify(session, "GAME_CREATED");
  return view(session, seats.get(0).playerId());
 }

 public GameView getGame(String gameId, String viewerId) {
  GameSession session = load(gameId);
  return view(session, viewerOf(session, viewerId));
 }

 public GameView drawFromDeck(String gameId, String playerId, String commandId) {
  return mutate(command(gameId, playerId, commandId, Action.DRAW_DECK, ""),
   session -> orchestrator.drawFromDeck(session, playerId));
 }

 public GameView drawFromDiscard(String gameId, String playerId, String commandId) {
  return mutate(command(gameId, playerId, commandId, Action.DRAW_DISCARD, ""),
   session -> orchestrator.drawFromDiscard(session, playerId));
 }

 public GameView discard(String gameId, String playerId, String cardId, String commandId) {
  String card = requireText(cardId, "carta");
  return mutate(command(gameId, playerId, commandId, Action.DISCARD, card),
   session -> orchestrator.discard(session, playerId, card));
 }

 public GameView endTurn(String gameId, String playerId, String commandId) {
  return mutate(command(gameId, playerId, commandId, Action.END_TURN, ""),
   session -> orchestrator.endTurn(session, playerId));
 }

 public GameView closeRound(String gameId, String playerId, String commandId) {
  return mutate(command(gameId, playerId, commandId, Action.CLOSE, ""),
   session -> orchestrator.closeRound(session, playerId));
 }

 private static PendingCommand command(String gameId, String playerId, String commandId, Action action,
  String argument) {
  return new PendingCommand(requireText(gameId, "partida"), requireText(playerId, "jugador"),
   requireText(commandId, "commandId"), action, argument);
 }

 private GameView mutate(PendingCommand command, UnaryOperator<GameSession> operation) {
  long started = System.nanoTime();
  try {
   return execute(command, operation, started);
  } catch (RuntimeException exception) {
   record(command, contextOf(command.gameId()), REJECTED, started, exception.getClass().getSimpleName() + ": " + exception.getMessage());
   throw exception;
  }
 }

 private GameView execute(PendingCommand command, UnaryOperator<GameSession> operation, long started) {
  Optional<GameView> replay = replay(command, started);
  if (replay.isPresent()) return replay.get();
  GameSession previous = load(command.gameId());
  GameSession updated = operation.apply(previous);
  try {
   repository.save(previous, updated, command.receipt(updated.version()));
  } catch (OptimisticLockException conflict) {
   // Un reintento concurrente del mismo comando pudo confirmarse primero: se devuelve su resultado.
   return replay(command, started).orElseThrow(() -> conflict);
  }
  record(command, updated, OK, started, detail(command, updated));
  notify(updated, command.action().eventType);
  return view(updated, command.playerId());
 }

 private GameSession contextOf(String gameId) {
  try {
   return repository.findById(gameId).orElse(null);
  } catch (RuntimeException unavailable) {
   return null;
  }
 }

 private Optional<GameView> replay(PendingCommand command, long started) {
  Optional<CommandReceipt> stored = repository.receipt(command.gameId(), command.commandId());
  if (stored.isEmpty()) return Optional.empty();
  CommandReceipt receipt = stored.get();
  if (!receipt.sameCommandAs(command.receipt(receipt.resultVersion()))) {
   throw new CommandConflictException(command.commandId());
  }
  GameSession current = load(command.gameId());
  record(command, current, REPLAY, started, "resultVersion=" + receipt.resultVersion());
  return Optional.of(view(current, command.playerId()));
 }

 private static String detail(PendingCommand command, GameSession updated) {
  if (command.action() == Action.CLOSE) return closeDetail(updated);
  if (command.action() == Action.DISCARD) return "card=" + command.argument();
  return "deck=" + updated.round().deck().size() + " discard=" + updated.round().discard().size();
 }

 private static String closeDetail(GameSession updated) {
  RoundState closed = updated.completedRounds().get(updated.completedRounds().size() - 1);
  String results = closed.scores().stream()
   .map(score -> score.playerId() + ":+" + score.penalty() + "=" + score.totalAfter())
   .collect(Collectors.joining(","));
  return "closedRound=" + closed.number() + " closer=" + closed.closedBy() + " results=" + results
   + " status=" + updated.status() + (updated.winnerId() == null ? "" : " winner=" + updated.winnerId());
 }

 /** La auditoría es observabilidad: un fallo se registra en el log técnico y no altera el comando. */
 private void record(PendingCommand command, GameSession session, String outcome, long started, String detail) {
  long micros = (System.nanoTime() - started) / 1_000;
  String action = command.action() == null ? "CREATE_GAME" : command.action().name();
  AuditEntry entry = session == null
   ? new AuditEntry(Instant.now(), command.gameId(), null, command.playerId(), action, command.commandId(), outcome,
    -1, 0, 0, micros, detail)
   : new AuditEntry(Instant.now(), command.gameId(), session.variantId(), command.playerId(), action,
    command.commandId(), outcome, session.version(), session.currentRound(), session.turn().number(), micros, detail);
  try {
   audit.record(entry);
  } catch (RuntimeException exception) {
   LOGGER.log(System.Logger.Level.WARNING, "Fallo al auditar " + action + " de la partida " + command.gameId(),
    exception);
  }
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
  IReglaJuego rule = rules.get(session.variantId());
  return new GameView(session.id(), session.variantId(), session.status().name(), session.currentRound(),
   session.version(), viewerId, turnView(session), session.winnerId(), players, hand,
   session.round().deck().size(), session.round().discard().size(), discardTop(session), lastResults(session),
   lastClosedBy(session), rule.descripcionCierre());
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

 private static String lastClosedBy(GameSession session) {
  List<RoundState> rounds = session.completedRounds();
  return rounds.isEmpty() ? null : rounds.get(rounds.size() - 1).closedBy();
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

 private static String fingerprint(String payload) {
  try {
   MessageDigest digest = MessageDigest.getInstance("SHA-256");
   return HexFormat.of().formatHex(digest.digest(payload.getBytes(StandardCharsets.UTF_8)));
  } catch (NoSuchAlgorithmException exception) {
   throw new IllegalStateException("SHA-256 no disponible", exception);
  }
 }

 private static String requireText(String value, String field) {
  if (value == null || value.isBlank()) throw new IllegalArgumentException("Campo obligatorio: " + field);
  return value;
 }
}
