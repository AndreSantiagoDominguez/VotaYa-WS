# VotaYa — WebSocket Server

Servidor WebSocket + Auth con MySQL para votaciones en tiempo real.

## Estructura

```
VotaYa-WS/
├── index.js          # Server principal (Express + WebSocket)
├── auth/
│   ├── db.js         # MySQL — conexión y queries de usuarios
│   └── routes.js     # POST /register y POST /login
├── package.json
├── .gitignore
└── README.md
```

## Requisitos

- Node.js 18+
- MySQL 8+ corriendo en localhost
- Hola 
## Setup

```bash
# 1. Crear la base de datos en MySQL
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS votaya"

# 2. Instalar dependencias
npm install

# 3. Correr el server
npm run dev
```

Si tu MySQL tiene contraseña o está en otro host, usa variables de entorno:
```bash
DB_HOST=localhost DB_USER=root DB_PASSWORD=tu_pass DB_NAME=votaya npm run dev
```

## Autenticación (HTTP)

### Registro
```bash
curl -X POST http://localhost:3000/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Kami","email":"kami@test.com","password":"123456"}'
```

### Login
```bash
curl -X POST http://localhost:3000/login \
  -H "Content-Type: application/json" \
  -d '{"email":"kami@test.com","password":"123456"}'
```

### Health Check
```bash
curl http://localhost:3000/health
```

## WebSocket (requiere token)

```
ws://localhost:3000?token=TU_JWT_TOKEN
```

## Protocolo de mensajes

### Cliente → Servidor

| Tipo | Payload |
|------|---------|
| `create_poll` | `{ title, options: ["A","B"] }` |
| `join_room` | `{ roomCode }` |
| `cast_vote` | `{ roomCode, optionIndex }` |
| `close_poll` | `{ roomCode }` |

### Servidor → Cliente(s)

| Tipo | Descripción |
|------|-------------|
| `authenticated` | Conexión WS autenticada |
| `auth_error` | Token inválido, conexión cerrada |
| `room_created` | Confirmación con roomCode |
| `room_joined` | Estado actual de la encuesta |
| `user_joined` | Notifica que un usuario se unió |
| `user_left` | Notifica que un usuario salió |
| `vote_confirmed` | Confirmación de voto |
| `vote_update` | Resultados actualizados (broadcast) |
| `vote_rejected` | `already_voted`, `poll_closed`, `room_not_found`, `invalid_option` |
| `poll_closed` | Resultados finales (broadcast) |
| `client_count` | Número de conectados |
| `error` | Error genérico |
