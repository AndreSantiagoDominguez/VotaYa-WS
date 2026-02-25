const { WebSocketServer } = require("ws");

const PORT = process.env.PORT || 3000;
const wss = new WebSocketServer({ port: PORT });

const rooms = new Map();

function generateRoomCode() {
  const chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  let code = "";
  for (let i = 0; i < 6; i++) {
    code += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return rooms.has(code) ? generateRoomCode() : code;
}

function send(ws, data) {
  if (ws.readyState === ws.OPEN) {
    ws.send(JSON.stringify(data));
  }
}


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
    voters: new Map(), // voterId -> optionIndex
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


function handleCastVote(ws, msg) {
  send(ws, { type: "error", message: "cast_vote aún no implementado" });
}

function handleClosePoll(ws, msg) {
  send(ws, { type: "error", message: "close_poll aún no implementado" });
}

function handleDisconnect(ws) {
  console.log("Cliente desconectado");
}

console.log(`VotaYa WS corriendo en puerto ${PORT}`);
