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

// Conexión de un nuevo cliente
wss.on("connection", (ws) => {
  console.log("Cliente conectado");

  ws.on("message", (raw) => {
    let msg;
    try {
      msg = JSON.parse(raw);
    } catch {
      return send(ws, { type: "error", message: "JSON inválido" });
    }

    // Router de mensajes
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
  send(ws, { type: "error", message: "create_poll aún no implementado" });
}

function handleJoinRoom(ws, msg) {
  send(ws, { type: "error", message: "join_room aún no implementado" });
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
