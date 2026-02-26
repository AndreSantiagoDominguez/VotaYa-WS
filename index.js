require("dotenv").config();
const express = require("express");
const http = require("http");
const { WebSocketServer } = require("ws");
const jwt = require("jsonwebtoken");
const url = require("url");
const authRoutes = require("./auth/routes");
const db = require("./auth/db");

const PORT = process.env.PORT;
const JWT_SECRET = process.env.JWT_SECRET;

const app = express();
app.use(express.json());

app.use(authRoutes);

app.get("/health", async (req, res) => {
  const userCount = await db.getUserCount();
  res.json({ status: "ok", users: userCount, rooms: rooms.size });
});

const server = http.createServer(app);

const wss = new WebSocketServer({ server });

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

function authenticateWs(req) {
  try {
    const params = url.parse(req.url, true).query;
    const token = params.token;
    if (!token) return null;
    return jwt.verify(token, JWT_SECRET);
  } catch {
    return null;
  }
}


wss.on("connection", (ws, req) => {
  const userData = authenticateWs(req);

  if (!userData) {
    send(ws, { type: "auth_error", message: "Token inválido o no proporcionado" });
    ws.close();
    return;
  }

  ws.userId = userData.id;
  ws.userName = userData.name;
  ws.userEmail = userData.email;

  send(ws, {
    type: "authenticated",
    user: { id: userData.id, name: userData.name },
  });

  console.log(`Cliente autenticado: ${ws.userName} (${ws.userId})`);

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
    creatorId: ws.userId,
    creatorName: ws.userName,
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
      createdBy: room.creatorName,
    },
  });

  console.log(`Sala ${roomCode} creada por ${ws.userName}: "${title}"`);
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
      createdBy: room.creatorName,
    },
  });

  broadcastToRoom(room.roomCode, {
    type: "user_joined",
    userName: ws.userName,
    clientCount: room.clients.size,
  });

  console.log(`${ws.userName} se unió a sala ${roomCode} (${room.clients.size} conectados)`);
}

function handleCastVote(ws, msg) {
  const { roomCode, optionIndex } = msg;

  if (!roomCode || optionIndex === undefined) {
    return send(ws, {
      type: "error",
      message: "Se requiere roomCode y optionIndex",
    });
  }

  const voterId = ws.userId;
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
    voterName: ws.userName,
  });

  broadcastToRoom(room.roomCode, {
    type: "vote_update",
    options: room.options,
    totalVoters: room.voters.size,
  });

  console.log(
    `${ws.userName} votó en sala ${roomCode}: opción ${optionIndex} (${room.voters.size} votos totales)`
  );
}

function handleClosePoll(ws, msg) {
  const { roomCode } = msg;

  if (!roomCode) {
    return send(ws, { type: "error", message: "Se requiere roomCode" });
  }

  const room = rooms.get(roomCode.toUpperCase());

  if (!room) {
    return send(ws, { type: "error", message: "Sala no encontrada" });
  }

  if (room.creatorId !== ws.userId) {
    return send(ws, {
      type: "error",
      message: "Solo el creador puede cerrar la encuesta",
    });
  }

  if (!room.isOpen) {
    return send(ws, { type: "error", message: "La encuesta ya está cerrada" });
  }

  room.isOpen = false;

  broadcastToRoom(room.roomCode, {
    type: "poll_closed",
    finalResults: room.options,
    totalVoters: room.voters.size,
  });

  console.log(
    `Encuesta cerrada en sala ${roomCode} por ${ws.userName} (${room.voters.size} votos totales)`
  );
}

function handleDisconnect(ws) {
  const roomCode = ws.roomCode;
  if (!roomCode) return;

  const room = rooms.get(roomCode);
  if (!room) return;

  room.clients.delete(ws);

  if (room.creatorId === ws.userId && room.isOpen) {
    room.isOpen = false;
    broadcastToRoom(roomCode, {
      type: "poll_closed",
      finalResults: room.options,
      totalVoters: room.voters.size,
    });
    console.log(`${ws.userName} (creador) desconectado, encuesta cerrada en sala ${roomCode}`);
  }

  if (room.clients.size > 0) {
    broadcastToRoom(roomCode, {
      type: "user_left",
      userName: ws.userName,
      clientCount: room.clients.size,
    });
  }

  if (room.clients.size === 0) {
    setTimeout(() => {
      if (rooms.has(roomCode) && rooms.get(roomCode).clients.size === 0) {
        rooms.delete(roomCode);
        console.log(`Sala ${roomCode} eliminada por inactividad`);
      }
    }, 5 * 60 * 1000);
  }

  console.log(`${ws.userName} desconectado de sala ${roomCode} (${room.clients.size} restantes)`);
}

const colors = {
  reset: "\x1b[0m",
  cyan: "\x1b[36m",
  green: "\x1b[32m",
  yellow: "\x1b[33m",
  red: "\x1b[31m",
  magenta: "\x1b[35m"
};

async function start() {
  await db.initDB();
  const userCount = await db.getUserCount();

  server.listen(PORT, () => {
    console.log(`\n${colors.green}🚀 ¡VotaYa Server inicializado con éxito!${colors.reset}\n`);
    console.log(`  🌐 ${colors.magenta}HTTP${colors.reset}  → ${colors.cyan}http://localhost:${PORT}${colors.reset}`);
    console.log(`  ⚡ ${colors.yellow}WS${colors.reset}    → ${colors.cyan}ws://localhost:${PORT}?token=JWT_TOKEN${colors.reset}`);
    console.log(`  👥 ${colors.magenta}Users${colors.reset} → ${colors.green}${userCount}${colors.reset} registrados en MySQL\n`);
    console.log(`${colors.yellow}📡 Escuchando peticiones...${colors.reset}`);
  });
}

start().catch((err) => {
  console.error(`\n${colors.red}❌ ¡Alerta Roja! Fallo crítico al iniciar el servidor.${colors.reset}\n`);
  console.error(`💥 ${colors.red}Error:${colors.reset} ${err.message}`);
  console.error(`🔍 ${colors.yellow}Pista de depuración:${colors.reset} ¿Está encendido el motor de MySQL en tu computadora?`);
  console.error(`   Revisa tus variables: DB_HOST, DB_USER, DB_PASSWORD, DB_NAME en el archivo .env\n`);
  process.exit(1);
});