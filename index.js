const { WebSocketServer } = require("ws");

const PORT = process.env.PORT || 3000;
const wss = new WebSocketServer({ port: PORT });

// ── Almacén de salas en memoria ──
const rooms = new Map();

// ── Utilidad: generar código de sala (6 caracteres) ──
function generateRoomCode() {
  const chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  let code = "";
  for (let i = 0; i < 6; i++) {
    code += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return rooms.has(code) ? generateRoomCode() : code;
}

// ── Utilidad: enviar mensaje JSON a un cliente ──
function send(ws, data) {
  if (ws.readyState === ws.OPEN) {
    ws.send(JSON.stringify(data));
  }
}

// ── Utilidad: broadcast a todos los clientes de una sala ──
function broadcastToRoom(roomCode, data) {
  const room = rooms.get(roomCode);
  if (!room) return;
  for (const client of room.clients) {
    send(client, data);
  }
}

// ── Conexión de un nuevo cliente ──
wss.on("connection", (ws) => {
  console.log("Cliente conectado");

  ws.on("message", (raw) => {
    let msg;
    try {
      msg = JSON.parse(raw);
    } catch {
      return send(ws, { type: "error", message: "JSON inválido" });
    }

    // ── Router de mensajes ──
    switch (msg.type) {
      case "create_poll":
        handleCreatePoll(ws, msg);
        break;
      case "join_room":
        handleJoinRoom(ws, msg);
        break;
      case "cast_vote":
        handleCastVote(ws, msg);
        break;
      case "close_poll":
        handleClosePoll(ws, msg);
        break;
      default:
        send(ws, { type: "error", message: `Tipo desconocido: ${msg.type}` });
    }
  });

  ws.on("close", () => {
    handleDisconnect(ws);
  });
});

// ── Crear encuesta ──
function handleCreatePoll(ws, msg) {
  const { title, options } = msg;

  if (!title || !options || !Array.isArray(options) || options.length < 2) {
    return send(ws, {
      type: "error",
      message: "Se requiere título y al menos 2 opciones",
    });
  }

  const roomCode = generateRoomCode();

  const room = {
    roomCode,
    title,
    options: options.map((opt, i) => ({
      index: i,
      text: opt,
      votes: 0,
    })),
    voters: new Map(),
    clients: new Set([ws]),
    creatorWs: ws,
    isOpen: true,
    createdAt: Date.now(),
  };

  rooms.set(roomCode, room);
  ws.roomCode = roomCode;

  send(ws, {
    type: "room_created",
    roomCode,
    poll: {
      title: room.title,
      options: room.options,
      isOpen: room.isOpen,
    },
  });

  console.log(`Sala ${roomCode} creada: "${title}" con ${options.length} opciones`);
}

// ── Unirse a sala ──
function handleJoinRoom(ws, msg) {
  const { roomCode } = msg;

  if (!roomCode) {
    return send(ws, { type: "error", message: "Se requiere roomCode" });
  }

  const room = rooms.get(roomCode.toUpperCase());

  if (!room) {
    return send(ws, { type: "room_not_found", roomCode });
  }

  room.clients.add(ws);
  ws.roomCode = roomCode.toUpperCase();

  send(ws, {
    type: "room_joined",
    roomCode: room.roomCode,
    poll: {
      title: room.title,
      options: room.options,
      isOpen: room.isOpen,
      totalVoters: room.voters.size,
    },
  });

  broadcastToRoom(room.roomCode, {
    type: "client_count",
    count: room.clients.size,
  });

  console.log(`Cliente se unió a sala ${roomCode} (${room.clients.size} conectados)`);
}

// ── Votar ──
function handleCastVote(ws, msg) {
  const { roomCode, optionIndex, voterId } = msg;

  // Validar campos requeridos
  if (!roomCode || optionIndex === undefined || !voterId) {
    return send(ws, {
      type: "error",
      message: "Se requiere roomCode, optionIndex y voterId",
    });
  }

  const room = rooms.get(roomCode.toUpperCase());

  // Validar que la sala existe
  if (!room) {
    return send(ws, { type: "vote_rejected", reason: "room_not_found" });
  }

  // Validar que la encuesta sigue abierta
  if (!room.isOpen) {
    return send(ws, { type: "vote_rejected", reason: "poll_closed" });
  }

  // Validar que la opción existe
  if (optionIndex < 0 || optionIndex >= room.options.length) {
    return send(ws, { type: "vote_rejected", reason: "invalid_option" });
  }

  // Validar que no haya votado antes
  if (room.voters.has(voterId)) {
    return send(ws, { type: "vote_rejected", reason: "already_voted" });
  }

  // Registrar voto
  room.voters.set(voterId, optionIndex);
  room.options[optionIndex].votes += 1;

  // Confirmar al votante
  send(ws, {
    type: "vote_confirmed",
    optionIndex,
    voterId,
  });

  // Broadcast de resultados actualizados a toda la sala
  broadcastToRoom(room.roomCode, {
    type: "vote_update",
    options: room.options,
    totalVoters: room.voters.size,
  });

  console.log(
    `Voto en sala ${roomCode}: opción ${optionIndex} (${room.voters.size} votos totales)`
  );
}

// ── Handlers pendientes ──

function handleClosePoll(ws, msg) {
  // TODO: ULISES-2
  send(ws, { type: "error", message: "close_poll aún no implementado" });
}

function handleDisconnect(ws) {
  // TODO: ULISES-2
  console.log("Cliente desconectado");
}

console.log(`VotaYa WS corriendo en puerto ${PORT}`);
