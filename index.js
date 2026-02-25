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

  if (!roomCode || optionIndex === undefined || !voterId) {
    return send(ws, {
      type: "error",
      message: "Se requiere roomCode, optionIndex y voterId",
    });
  }

  const room = rooms.get(roomCode.toUpperCase());

  if (!room) {
    return send(ws, { type: "vote_rejected", reason: "room_not_found" });
  }

  if (!room.isOpen) {
    return send(ws, { type: "vote_rejected", reason: "poll_closed" });
  }

  if (optionIndex < 0 || optionIndex >= room.options.length) {
    return send(ws, { type: "vote_rejected", reason: "invalid_option" });
  }

  if (room.voters.has(voterId)) {
    return send(ws, { type: "vote_rejected", reason: "already_voted" });
  }

  room.voters.set(voterId, optionIndex);
  room.options[optionIndex].votes += 1;

  send(ws, {
    type: "vote_confirmed",
    optionIndex,
    voterId,
  });

  broadcastToRoom(room.roomCode, {
    type: "vote_update",
    options: room.options,
    totalVoters: room.voters.size,
  });

  console.log(
    `Voto en sala ${roomCode}: opción ${optionIndex} (${room.voters.size} votos totales)`
  );
}

// ── Cerrar encuesta ──
function handleClosePoll(ws, msg) {
  const { roomCode } = msg;

  if (!roomCode) {
    return send(ws, { type: "error", message: "Se requiere roomCode" });
  }

  const room = rooms.get(roomCode.toUpperCase());

  if (!room) {
    return send(ws, { type: "error", message: "Sala no encontrada" });
  }

  // Solo el creador puede cerrar la encuesta
  if (room.creatorWs !== ws) {
    return send(ws, {
      type: "error",
      message: "Solo el creador puede cerrar la encuesta",
    });
  }

  if (!room.isOpen) {
    return send(ws, { type: "error", message: "La encuesta ya está cerrada" });
  }

  room.isOpen = false;

  // Broadcast de resultados finales a toda la sala
  broadcastToRoom(room.roomCode, {
    type: "poll_closed",
    finalResults: room.options,
    totalVoters: room.voters.size,
  });

  console.log(
    `Encuesta cerrada en sala ${roomCode} (${room.voters.size} votos totales)`
  );
}

// ── Desconexión de cliente ──
function handleDisconnect(ws) {
  const roomCode = ws.roomCode;
  if (!roomCode) return;

  const room = rooms.get(roomCode);
  if (!room) return;

  // Remover cliente de la sala
  room.clients.delete(ws);

  // Si el creador se desconecta, cerrar la encuesta automáticamente
  if (room.creatorWs === ws && room.isOpen) {
    room.isOpen = false;
    broadcastToRoom(roomCode, {
      type: "poll_closed",
      finalResults: room.options,
      totalVoters: room.voters.size,
    });
    console.log(`Creador desconectado, encuesta cerrada en sala ${roomCode}`);
  }

  // Notificar a los demás cuántos quedan
  if (room.clients.size > 0) {
    broadcastToRoom(roomCode, {
      type: "client_count",
      count: room.clients.size,
    });
  }

  // Si no quedan clientes, eliminar la sala después de 5 minutos
  if (room.clients.size === 0) {
    setTimeout(() => {
      if (rooms.has(roomCode) && rooms.get(roomCode).clients.size === 0) {
        rooms.delete(roomCode);
        console.log(`Sala ${roomCode} eliminada por inactividad`);
      }
    }, 5 * 60 * 1000);
  }

  console.log(`Cliente desconectado de sala ${roomCode} (${room.clients.size} restantes)`);
}

console.log(`VotaYa WS corriendo en puerto ${PORT}`);
