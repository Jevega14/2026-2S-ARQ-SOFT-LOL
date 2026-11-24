package com.apuntado.persistence.mongo;

import com.apuntado.domain.model.Card;
import com.apuntado.domain.model.GameSession;
import com.apuntado.domain.model.GameStatus;
import com.apuntado.domain.model.PlayerHandState;
import com.apuntado.domain.model.PlayerScore;
import com.apuntado.domain.model.PlayerSeat;
import com.apuntado.domain.model.RoundScore;
import com.apuntado.domain.model.RoundState;
import com.apuntado.domain.model.RoundStatus;
import com.apuntado.domain.model.TurnPhase;
import com.apuntado.domain.model.TurnState;
import java.util.List;
import org.bson.Document;

/** Traduce el agregado de dominio a un documento BSON explícito, sin anotaciones en el dominio. */
public final class GameSessionMapper {
 static final String ID = "_id";
 static final String RECEIPTS = "receipts";
 private GameSessionMapper() {
 }

 public static Document toDocument(GameSession session) {
  return new Document(ID, session.id())
   .append("variantId", session.variantId())
   .append("players", session.players().stream().map(GameSessionMapper::playerToDocument).toList())
   .append("currentRound", session.currentRound())
   .append("round", roundToDocument(session.round()))
   .append("turn", turnToDocument(session.turn()))
   .append("status", session.status().name())
   .append("version", session.version())
   .append("totals", session.totals().stream().map(GameSessionMapper::scoreToDocument).toList())
   .append("completedRounds", session.completedRounds().stream().map(GameSessionMapper::roundToDocument).toList())
   .append("winnerId", session.winnerId());
 }

 public static GameSession toDomain(Document document) {
  List<PlayerSeat> players = documents(document, "players").stream()
   .map(GameSessionMapper::playerFromDocument).toList();
  return new GameSession(
   document.getString(ID),
   document.getString("variantId"),
   players,
   integer(document, "currentRound"),
   roundFromDocument(document.get("round", Document.class)),
   turnFromDocument(document.get("turn", Document.class)),
   GameStatus.valueOf(document.getString("status")),
   integer(document, "version"),
   documents(document, "totals").stream().map(GameSessionMapper::scoreFromDocument).toList(),
   documents(document, "completedRounds").stream().map(GameSessionMapper::roundFromDocument).toList(),
   document.getString("winnerId"));
 }

 private static Document playerToDocument(PlayerSeat player) {
  return new Document("playerId", player.playerId())
   .append("seat", player.seat())
   .append("displayName", player.displayName());
 }

 private static PlayerSeat playerFromDocument(Document document) {
  return new PlayerSeat(document.getString("playerId"), integer(document, "seat"), document.getString("displayName"));
 }

 private static Document scoreToDocument(PlayerScore score) {
  return new Document("playerId", score.playerId()).append("total", score.total());
 }

 private static PlayerScore scoreFromDocument(Document document) {
  return new PlayerScore(document.getString("playerId"), integer(document, "total"));
 }

 private static Document roundToDocument(RoundState round) {
  return new Document("number", round.number())
   .append("hands", round.hands().stream().map(GameSessionMapper::handToDocument).toList())
   .append("deck", round.deck().stream().map(GameSessionMapper::cardToDocument).toList())
   .append("discard", round.discard().stream().map(GameSessionMapper::cardToDocument).toList())
   .append("status", round.status().name())
   .append("closedBy", round.closedBy())
   .append("scores", round.scores().stream().map(GameSessionMapper::roundScoreToDocument).toList());
 }

 private static RoundState roundFromDocument(Document document) {
  return new RoundState(
   integer(document, "number"),
   documents(document, "hands").stream().map(GameSessionMapper::handFromDocument).toList(),
   documents(document, "deck").stream().map(GameSessionMapper::cardFromDocument).toList(),
   documents(document, "discard").stream().map(GameSessionMapper::cardFromDocument).toList(),
   RoundStatus.valueOf(document.getString("status")),
   document.getString("closedBy"),
   documents(document, "scores").stream().map(GameSessionMapper::roundScoreFromDocument).toList());
 }

 private static Document handToDocument(PlayerHandState hand) {
  return new Document("playerId", hand.playerId())
   .append("cards", hand.cards().stream().map(GameSessionMapper::cardToDocument).toList());
 }

 private static PlayerHandState handFromDocument(Document document) {
  return new PlayerHandState(document.getString("playerId"),
   documents(document, "cards").stream().map(GameSessionMapper::cardFromDocument).toList());
 }

 private static Document roundScoreToDocument(RoundScore score) {
  return new Document("playerId", score.playerId())
   .append("penalty", score.penalty())
   .append("totalAfter", score.totalAfter());
 }

 private static RoundScore roundScoreFromDocument(Document document) {
  return new RoundScore(document.getString("playerId"), integer(document, "penalty"),
   integer(document, "totalAfter"));
 }

 private static Document cardToDocument(Card card) {
  return new Document("id", card.id())
   .append("rank", card.rank())
   .append("suit", card.suit())
   .append("points", card.points());
 }

 private static Card cardFromDocument(Document document) {
  return new Card(document.getString("id"), integer(document, "rank"), document.getString("suit"),
   integer(document, "points"));
 }

 private static Document turnToDocument(TurnState turn) {
  return new Document("playerId", turn.playerId())
   .append("number", turn.number())
   .append("phase", turn.phase().name());
 }

 private static TurnState turnFromDocument(Document document) {
  return new TurnState(document.getString("playerId"), integer(document, "number"),
   TurnPhase.valueOf(document.getString("phase")));
 }

 private static List<Document> documents(Document parent, String key) {
  List<Document> values = parent.getList(key, Document.class);
  return values == null ? List.of() : values;
 }

 private static int integer(Document document, String key) {
  Number value = document.get(key, Number.class);
  if (value == null) throw new IllegalArgumentException("Campo numérico ausente: " + key);
  return value.intValue();
 }
}
