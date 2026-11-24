const state = { game: null, socket: null, socketViewer: null };

const SUIT_SYMBOL = { H: "♥", D: "♦", S: "♠", C: "♣" };
const RANK_LABEL = { 1: "A", 11: "J", 12: "Q", 13: "K" };
const PHASE_LABEL = { DRAW: "robar", DISCARD: "descartar", CLOSED: "cerrada" };
const STATUS_LABEL = { ACTIVE: "en curso", FINISHED: "finalizada", WAITING: "en espera" };
const VARIANT_LABEL = { CLASICA: "Clásica 4-3-3", APUNTADO101: "Apuntado a 101" };

const setupNames = ["Jugador 1", "Jugador 2"];

const $ = (id) => document.getElementById(id);

function uuid() {
  if (window.crypto && typeof window.crypto.randomUUID === "function") return window.crypto.randomUUID();
  return "cmd-" + Date.now() + "-" + Math.random().toString(16).slice(2);
}

function cardLabel(card) {
  return (RANK_LABEL[card.rank] || card.rank) + SUIT_SYMBOL[card.suit];
}

function variantName(id) {
  return VARIANT_LABEL[id] || id;
}

function playerName(view, playerId) {
  const player = view.players.find((item) => item.playerId === playerId);
  return player ? player.displayName : playerId;
}

async function api(path, options) {
  const response = await fetch(path, options);
  const text = await response.text();
  const data = text ? JSON.parse(text) : null;
  if (!response.ok) {
    throw new Error((data && data.error) || "Error " + response.status);
  }
  return data;
}

function post(path, payload) {
  return api(path, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
}

function clearMessages() {
  $("message").textContent = "";
  $("error").textContent = "";
}

function showError(error) {
  $("error").textContent = error.message || String(error);
}

function cardElement(card, options) {
  const settings = options || {};
  const element = document.createElement("div");
  element.className = "playing-card";
  if (card.suit === "H" || card.suit === "D") element.classList.add("red");
  const rank = document.createElement("span");
  rank.className = "rank";
  rank.textContent = cardLabel(card);
  const points = document.createElement("span");
  points.className = "points";
  points.textContent = card.points + " pts";
  element.append(rank, points);
  if (settings.onClick) {
    element.classList.add("selectable");
    element.tabIndex = 0;
    element.setAttribute("role", "button");
    element.addEventListener("click", settings.onClick);
  }
  return element;
}

async function loadVariants() {
  const select = $("variant");
  select.innerHTML = "";
  const variants = await api("/api/variants");
  variants.forEach((variant) => {
    const option = document.createElement("option");
    option.value = variant.id;
    option.textContent = variantName(variant.id);
    select.appendChild(option);
  });
}

function renderSetupPlayers() {
  const container = $("players");
  container.innerHTML = "";
  setupNames.forEach((name, index) => {
    const row = document.createElement("div");
    row.className = "row";
    const label = document.createElement("label");
    label.textContent = "Jugador " + (index + 1);
    const input = document.createElement("input");
    input.type = "text";
    input.value = name;
    input.maxLength = 30;
    input.dataset.index = String(index);
    input.addEventListener("input", () => {
      setupNames[index] = input.value;
    });
    row.append(label, input);
    container.appendChild(row);
  });
  $("add-player").disabled = setupNames.length >= 4;
  $("remove-player").disabled = setupNames.length <= 2;
}

async function createGame() {
  $("setup-error").textContent = "";
  const variantId = $("variant").value;
  const players = setupNames.map((name, index) => ({
    playerId: "p" + (index + 1),
    seat: index,
    displayName: name.trim() || "Jugador " + (index + 1),
  }));
  try {
    const view = await post("/api/games", { variantId, players });
    showGame(view);
  } catch (error) {
    $("setup-error").textContent = error.message;
  }
}

function viewerFor(view) {
  return view.status === "ACTIVE" ? view.turn.playerId : view.viewerId;
}

async function refresh() {
  const viewer = encodeURIComponent(viewerFor(state.game));
  const view = await api("/api/games/" + state.game.gameId + "?viewerId=" + viewer);
  showGame(view);
}

async function perform(path, payload) {
  clearMessages();
  try {
    state.game = await post(path, payload);
    $("message").textContent = "Acción aplicada.";
    await refresh();
  } catch (error) {
    showError(error);
  }
}

function showGame(view) {
  state.game = view;
  $("setup").classList.add("hidden");
  $("game").classList.remove("hidden");
  render(view);
  ensureSocket();
}

function socketUrl(viewerId) {
  const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
  return protocol + "//" + window.location.host + "/ws/games?gameId=" +
    encodeURIComponent(state.game.gameId) + "&playerId=" + encodeURIComponent(viewerId);
}

function ensureSocket() {
  if (!state.game) {
    closeSocket();
    return;
  }
  const viewer = viewerFor(state.game);
  const active = state.socket && (state.socket.readyState === WebSocket.OPEN ||
    state.socket.readyState === WebSocket.CONNECTING);
  if (active && state.socketViewer === viewer) return;
  closeSocket();
  const socket = new WebSocket(socketUrl(viewer));
  state.socket = socket;
  state.socketViewer = viewer;
  socket.addEventListener("message", () => {
    refresh().catch(showError);
  });
  socket.addEventListener("close", () => {
    if (state.socket === socket) {
      state.socket = null;
      state.socketViewer = null;
    }
  });
}

function closeSocket() {
  if (state.socket) {
    try {
      state.socket.close();
    } catch (error) {
      state.socket = null;
    }
  }
  state.socket = null;
  state.socketViewer = null;
}

function render(view) {
  $("game-title").textContent = "Partida " + variantName(view.variantId);
  $("game-meta").textContent = "Ronda " + view.currentRound + " · " + STATUS_LABEL[view.status] +
    " · versión " + view.version;

  if (view.status === "FINISHED") {
    $("turn-info").textContent = "Ganador: " + playerName(view, view.winnerId);
  } else {
    $("turn-info").textContent = "Turno de " + playerName(view, view.turn.playerId) +
      " · fase: " + (PHASE_LABEL[view.turn.phase] || view.turn.phase);
  }

  renderPlayers(view);
  $("deck-count").textContent = view.deckCount;
  $("discard-count").textContent = view.discardCount;
  $("discard-top").innerHTML = "";
  if (view.discardTop) {
    const label = document.createElement("span");
    label.textContent = "Cima del pozo:";
    $("discard-top").append(label, cardElement(view.discardTop));
  } else {
    $("discard-top").textContent = "Pozo vacío";
  }

  renderHand(view);
  renderActions(view);
  renderResults(view);
}

function renderPlayers(view) {
  const body = $("players-body");
  body.innerHTML = "";
  view.players.forEach((player) => {
    const row = document.createElement("tr");
    if (player.currentTurn) row.classList.add("active-player");
    const name = document.createElement("td");
    name.textContent = player.displayName;
    const total = document.createElement("td");
    total.textContent = player.total;
    const cards = document.createElement("td");
    cards.textContent = player.cardCount;
    const stateCell = document.createElement("td");
    if (player.eliminated) {
      stateCell.textContent = "Eliminado";
      stateCell.className = "eliminated";
    } else if (player.currentTurn) {
      stateCell.textContent = "Turno";
    }
    row.append(name, total, cards, stateCell);
    body.appendChild(row);
  });
}

function canDiscard(view) {
  return view.status === "ACTIVE" && view.turn.playerId === view.viewerId &&
    view.turn.phase === "DISCARD" && view.hand.length === 11;
}

function renderHand(view) {
  const hand = $("hand");
  hand.innerHTML = "";
  const discardable = canDiscard(view);
  if (view.status === "FINISHED") {
    $("hand-hint").textContent = "Partida finalizada.";
  } else if (view.hand.length === 0) {
    $("hand-hint").textContent = "Este jugador no tiene mano activa.";
  } else if (discardable) {
    $("hand-hint").textContent = "Selecciona una carta para descartar (tienes 11).";
  } else if (view.turn.phase === "DRAW" && view.turn.playerId === view.viewerId) {
    $("hand-hint").textContent = "Roba del mazo o del pozo para comenzar tu turno.";
  } else {
    $("hand-hint").textContent = "Mano de " + playerName(view, view.viewerId) + ".";
  }
  view.hand.forEach((card) => {
    const onClick = discardable ? () => discardCard(card.id) : null;
    hand.appendChild(cardElement(card, { onClick }));
  });
}

function renderActions(view) {
  const actions = $("actions");
  actions.innerHTML = "";
  if (view.status !== "ACTIVE" || view.turn.playerId !== view.viewerId) return;

  if (view.turn.phase === "DRAW") {
    actions.appendChild(actionButton("Robar del mazo", "draw-deck"));
    const discardButton = actionButton("Robar del pozo", "draw-discard");
    discardButton.disabled = view.discardCount === 0;
    actions.appendChild(discardButton);
  } else if (view.turn.phase === "DISCARD" && view.hand.length === 10) {
    actions.appendChild(actionButton("Terminar turno", "end-turn"));
    const closeButton = actionButton("Intentar cierre", "close");
    closeButton.classList.add("danger");
    actions.appendChild(closeButton);
  }
}

function actionButton(label, action) {
  const button = document.createElement("button");
  button.type = "button";
  button.textContent = label;
  button.addEventListener("click", () => perform("/api/games/" + state.game.gameId + "/" + action, {
    playerId: state.game.viewerId,
    commandId: uuid(),
  }));
  return button;
}

function discardCard(cardId) {
  perform("/api/games/" + state.game.gameId + "/discard", {
    playerId: state.game.turn.playerId,
    cardId: cardId,
    commandId: uuid(),
  });
}

function renderResults(view) {
  const container = $("last-results");
  container.className = "results";
  container.innerHTML = "";
  if (!view.lastRoundResults || view.lastRoundResults.length === 0) return;
  const title = document.createElement("h3");
  title.textContent = "Última ronda liquidada";
  const table = document.createElement("table");
  const head = document.createElement("tr");
  ["Jugador", "Penalización", "Total acumulado"].forEach((text) => {
    const th = document.createElement("th");
    th.textContent = text;
    head.appendChild(th);
  });
  table.appendChild(head);
  view.lastRoundResults.forEach((result) => {
    const row = document.createElement("tr");
    const name = document.createElement("td");
    name.textContent = playerName(view, result.playerId);
    const penalty = document.createElement("td");
    penalty.textContent = result.penalty;
    const total = document.createElement("td");
    total.textContent = result.totalAfter;
    row.append(name, penalty, total);
    table.appendChild(row);
  });
  container.append(title, table);
}

function newGame() {
  state.game = null;
  closeSocket();
  $("game").classList.add("hidden");
  $("setup").classList.remove("hidden");
  clearMessages();
  $("setup-error").textContent = "";
}

function bindEvents() {
  $("add-player").addEventListener("click", () => {
    if (setupNames.length < 4) setupNames.push("Jugador " + (setupNames.length + 1));
    renderSetupPlayers();
  });
  $("remove-player").addEventListener("click", () => {
    if (setupNames.length > 2) setupNames.pop();
    renderSetupPlayers();
  });
  $("create-game").addEventListener("click", createGame);
  $("refresh").addEventListener("click", () => {
    clearMessages();
    refresh().catch(showError);
  });
  $("new-game").addEventListener("click", newGame);
}

async function init() {
  bindEvents();
  renderSetupPlayers();
  try {
    await loadVariants();
  } catch (error) {
    $("setup-error").textContent = "No se pudieron cargar las modalidades: " + error.message;
  }
}

document.addEventListener("DOMContentLoaded", init);
