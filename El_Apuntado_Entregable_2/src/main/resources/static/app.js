const state = {
  game: null,
  socket: null,
  socketViewer: null,
  variants: [],
  sortMode: "none",
  // Modo dispositivo: el enlace ?game=...&player=... fija al jugador que usa este navegador.
  deviceViewer: null,
  // Modo relevo (un solo dispositivo): jugador que ya confirmó que tiene el dispositivo.
  revealedFor: null,
  // MJ1: el descarte es en dos pasos (seleccionar la carta y confirmar con «Descartar»).
  selectedCardId: null,
};

const SUIT_SYMBOL = { H: "♥", D: "♦", S: "♠", C: "♣" };
const RANK_LABEL = { 1: "A", 11: "J", 12: "Q", 13: "K" };
const PHASE_LABEL = { DRAW: "robar", DISCARD: "descartar", CLOSED: "cerrada" };
const STATUS_LABEL = { ACTIVE: "en curso", FINISHED: "finalizada", WAITING: "en espera" };
const VARIANT_LABEL = { CLASICA: "Clásica 4-3-3", APUNTADO101: "Apuntado a 101" };

const SUIT_ORDER = { H: 0, D: 1, S: 2, C: 3 };

const setupNames = ["Jugador 1", "Jugador 2"];

const $ = (id) => document.getElementById(id);

function uuid() {
  if (window.crypto && typeof window.crypto.randomUUID === "function") return window.crypto.randomUUID();
  return "cmd-" + Date.now() + "-" + Math.random().toString(16).slice(2);
}

function pointsLabel(points) {
  return points + (points === 1 ? " punto" : " puntos");
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
  element.setAttribute("aria-label", cardLabel(card) + " (" + pointsLabel(card.points) + ")");
  if (settings.onClick) {
    element.classList.add("selectable");
    element.tabIndex = 0;
    element.setAttribute("role", "button");
    element.addEventListener("click", settings.onClick);
    element.addEventListener("keydown", (event) => {
      if (event.key === "Enter" || event.key === " ") {
        event.preventDefault();
        settings.onClick();
      }
    });
  }
  if (settings.selected) {
    element.classList.add("selected");
    element.setAttribute("aria-pressed", "true");
  }
  return element;
}

async function loadVariants() {
  const select = $("variant");
  select.innerHTML = "";
  state.variants = await api("/api/variants");
  state.variants.forEach((variant) => {
    const option = document.createElement("option");
    option.value = variant.id;
    option.textContent = variantName(variant.id);
    select.appendChild(option);
  });
  renderVariantRule();
}

function variantRuleText(variant) {
  const ending = variant.singleRound
    ? "Se juega una sola ronda: gana quien cierre primero."
    : "Se juegan rondas: quien acumule 101 o más queda eliminado y gana el último en pie.";
  return "Para cerrar: " + variant.closeRequirement + ". " + ending;
}

function renderVariantRule() {
  const variant = state.variants.find((item) => item.id === $("variant").value);
  $("variant-rule").textContent = variant ? variantRuleText(variant) : "";
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
    state.deviceViewer = null;
    state.revealedFor = null;
    window.history.replaceState(null, "", "/?game=" + encodeURIComponent(view.gameId));
    showGame(view);
  } catch (error) {
    $("setup-error").textContent = error.message;
  }
}

function viewerFor(view) {
  if (state.deviceViewer) return state.deviceViewer;
  return view.status === "ACTIVE" ? view.turn.playerId : view.viewerId;
}

function awaitingHandoff(view) {
  return !state.deviceViewer && view.status === "ACTIVE" && state.revealedFor !== view.turn.playerId;
}

function sortedHand(cards) {
  const hand = cards.slice();
  if (state.sortMode === "suit") {
    hand.sort((a, b) => SUIT_ORDER[a.suit] - SUIT_ORDER[b.suit] || a.rank - b.rank);
  } else if (state.sortMode === "rank") {
    hand.sort((a, b) => a.rank - b.rank || SUIT_ORDER[a.suit] - SUIT_ORDER[b.suit]);
  }
  return hand;
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

  const variant = state.variants.find((item) => item.id === view.variantId);
  $("close-rule").textContent = variant ? variantRuleText(variant) : "Para cerrar: " + view.closeRequirement + ".";
  if (view.status === "FINISHED") {
    $("turn-info").textContent = "🏆 Partida terminada. Ganador: " + playerName(view, view.winnerId);
    $("turn-info").classList.add("winner-banner");
  } else {
    $("turn-info").classList.remove("winner-banner");
    const you = state.deviceViewer && state.deviceViewer === view.turn.playerId ? " (tú)" : "";
    $("turn-info").textContent = "Turno de " + playerName(view, view.turn.playerId) + you +
      " · fase: " + (PHASE_LABEL[view.turn.phase] || view.turn.phase);
  }
  renderViewerBadge(view);
  renderTurnSteps(view);
  renderInvite(view);

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

// MJ3: deja claro de quién es este navegador y cuál es el paso esperado del turno.
function renderViewerBadge(view) {
  const badge = $("viewer-badge");
  if (state.deviceViewer) {
    badge.textContent = "Este navegador es de " + playerName(view, state.deviceViewer) +
      ": aquí solo ves tu mano, también durante el turno de los demás.";
  } else {
    badge.textContent = "Un solo dispositivo: al cambiar el turno, la mano se oculta hasta pulsar «Ver mi mano».";
  }
}

function currentStep(view) {
  if (view.turn.phase === "DRAW") return 0;
  return view.hand.length === 11 ? 1 : 2;
}

function renderTurnSteps(view) {
  const list = $("turn-steps");
  const visible = view.status === "ACTIVE" && view.turn.playerId === view.viewerId && !awaitingHandoff(view);
  list.classList.toggle("hidden", !visible);
  list.innerHTML = "";
  if (!visible) return;
  const steps = ["Roba del mazo o del pozo", "Elige la carta y descártala", "Termina el turno o intenta el cierre"];
  const current = currentStep(view);
  steps.forEach((text, index) => {
    const item = document.createElement("li");
    item.textContent = text;
    if (index < current) item.className = "done";
    if (index === current) {
      item.className = "current";
      item.setAttribute("aria-current", "step");
    }
    list.append(item);
  });
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

function renderInvite(view) {
  const list = $("invite-links");
  list.innerHTML = "";
  view.players.forEach((player) => {
    const item = document.createElement("li");
    const link = document.createElement("a");
    const url = window.location.origin + "/?game=" + encodeURIComponent(view.gameId) +
      "&player=" + encodeURIComponent(player.playerId);
    link.href = url;
    link.target = "_blank";
    link.textContent = player.displayName + ": " + url;
    item.appendChild(link);
    list.appendChild(item);
  });
  $("invite").classList.toggle("hidden", view.status !== "ACTIVE");
}

function renderHandoff(view) {
  const pending = awaitingHandoff(view);
  $("handoff").classList.toggle("hidden", !pending);
  if (pending) {
    const name = playerName(view, view.turn.playerId);
    $("handoff-text").textContent = "Turno de " + name + ". Pasa el dispositivo a " + name +
      " y pulsa «Ver mi mano».";
  }
  return pending;
}

function renderHand(view) {
  const hand = $("hand");
  hand.innerHTML = "";
  $("hand-title").textContent = view.hand.length ? "Mano de " + playerName(view, view.viewerId) : "Tu mano";
  if (renderHandoff(view)) {
    $("hand-hint").textContent = "";
    return;
  }
  const discardable = canDiscard(view);
  if (!discardable || !view.hand.some((card) => card.id === state.selectedCardId)) state.selectedCardId = null;
  const selected = view.hand.find((card) => card.id === state.selectedCardId);
  const newestId = discardable ? view.hand[view.hand.length - 1].id : null;
  if (view.status === "FINISHED") {
    $("hand-hint").textContent = "Partida finalizada.";
  } else if (view.hand.length === 0) {
    $("hand-hint").textContent = "Este jugador no tiene mano activa.";
  } else if (discardable) {
    $("hand-hint").textContent = selected
      ? "Vas a descartar " + cardLabel(selected) + ". Pulsa «Descartar " + cardLabel(selected) +
        "» para confirmar o toca otra carta para cambiarla."
      : "Tienes 11 cartas: toca la carta que quieres botar y luego pulsa «Descartar».";
  } else if (view.turn.phase === "DRAW" && view.turn.playerId === view.viewerId) {
    $("hand-hint").textContent = "Roba del mazo o del pozo para comenzar tu turno.";
  } else {
    $("hand-hint").textContent = "Mano de " + playerName(view, view.viewerId) + ".";
  }
  sortedHand(view.hand).forEach((card) => {
    const onClick = discardable ? () => toggleSelection(card.id) : null;
    const element = cardElement(card, { onClick, selected: card.id === state.selectedCardId });
    if (card.id === newestId) {
      element.classList.add("new-card");
      element.title = "Carta recién robada";
      element.setAttribute("aria-label", cardLabel(card) + " (" + pointsLabel(card.points) + "), recién robada");
    }
    hand.appendChild(element);
  });
}

function renderActions(view) {
  const actions = $("actions");
  actions.innerHTML = "";
  if (view.status !== "ACTIVE" || view.turn.playerId !== view.viewerId || awaitingHandoff(view)) return;

  if (canDiscard(view)) {
    const selected = view.hand.find((card) => card.id === state.selectedCardId);
    const button = document.createElement("button");
    button.type = "button";
    button.id = "discard-button";
    button.textContent = selected ? "Descartar " + cardLabel(selected) : "Descartar";
    button.disabled = !selected;
    button.addEventListener("click", () => discardCard(state.selectedCardId));
    actions.appendChild(button);
    if (selected) {
      const cancel = document.createElement("button");
      cancel.type = "button";
      cancel.className = "secondary";
      cancel.textContent = "Cancelar selección";
      cancel.addEventListener("click", () => toggleSelection(selected.id));
      actions.appendChild(cancel);
    }
  } else if (view.turn.phase === "DRAW") {
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

function toggleSelection(cardId) {
  state.selectedCardId = state.selectedCardId === cardId ? null : cardId;
  renderHand(state.game);
  renderActions(state.game);
}

function discardCard(cardId) {
  if (!cardId) return;
  state.selectedCardId = null;
  perform("/api/games/" + state.game.gameId + "/discard", {
    playerId: state.game.viewerId,
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
  title.textContent = "Última ronda liquidada" +
    (view.lastClosedBy ? " · cerró " + playerName(view, view.lastClosedBy) + " (0 puntos)" : "");
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
  state.deviceViewer = null;
  state.revealedFor = null;
  window.history.replaceState(null, "", "/");
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
  $("variant").addEventListener("change", renderVariantRule);
  $("reveal-hand").addEventListener("click", () => {
    state.revealedFor = state.game.turn.playerId;
    render(state.game);
  });
  $("sort-suit").addEventListener("click", () => {
    state.sortMode = "suit";
    render(state.game);
  });
  $("sort-rank").addEventListener("click", () => {
    state.sortMode = "rank";
    render(state.game);
  });
}

/** Reanuda una partida desde la URL: recuperación tras recargar o enlace de un jugador. */
async function resumeFromUrl() {
  const params = new URLSearchParams(window.location.search);
  const gameId = params.get("game");
  if (!gameId) return;
  state.deviceViewer = params.get("player");
  const viewer = state.deviceViewer ? "?viewerId=" + encodeURIComponent(state.deviceViewer) : "";
  try {
    showGame(await api("/api/games/" + encodeURIComponent(gameId) + viewer));
  } catch (error) {
    state.deviceViewer = null;
    $("setup-error").textContent = "No se pudo abrir la partida: " + error.message;
  }
}

async function init() {
  bindEvents();
  renderSetupPlayers();
  try {
    await loadVariants();
  } catch (error) {
    $("setup-error").textContent = "No se pudieron cargar las modalidades: " + error.message;
  }
  await resumeFromUrl();
}

document.addEventListener("DOMContentLoaded", init);
