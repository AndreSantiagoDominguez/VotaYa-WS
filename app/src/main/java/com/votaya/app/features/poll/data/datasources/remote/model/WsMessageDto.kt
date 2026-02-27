package com.votaya.app.features.poll.data.datasources.remote.model

/**
 * DTOs para los mensajes JSON del WebSocket.
 * Matching exacto con el backend Node.js.
 */

// ── Mensajes que ENVÍA el cliente ──

data class CreateRoomDto(
    val type: String = "create_poll",
    val title: String,
    val options: List<String>
)

data class JoinRoomDto(
    val type: String = "join_room",
    val roomCode: String
)

data class VoteDto(
    val type: String = "cast_vote",
    val roomCode: String,
    val optionIndex: Int
)

data class ClosePollDto(
    val type: String = "close_poll",
    val roomCode: String
)

// ── Mensajes que RECIBE el cliente ──

data class WsResponseDto(
    val type: String?,
    val roomCode: String?,
    val poll: PollDto?,
    val message: String?,
    val error: String?,
    // Para actualizaciones de votos y resultados finales
    val options: List<PollOptionDto>?,
    val totalVoters: Int?,
    val finalResults: List<PollOptionDto>?
)

data class PollDto(
    val title: String?,
    val options: List<PollOptionDto>?,
    val isOpen: Boolean?,
    val createdBy: String?,
    val totalVoters: Int?
)

data class PollOptionDto(
    val index: Int?,
    val text: String?,
    val votes: Int?
)
