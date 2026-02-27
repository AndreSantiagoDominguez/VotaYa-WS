package com.votaya.app.features.poll.domain.entities

/**
 * Mensajes del WebSocket tipados para el domain layer.
 */
sealed class WsEvent {
    data class RoomCreated(val room: Room) : WsEvent()
    data class RoomJoined(val room: Room) : WsEvent()
    data class VoteUpdate(val room: Room) : WsEvent()
    data class PollClosed(val room: Room) : WsEvent()
    data class Error(val message: String) : WsEvent()
    data object Connected : WsEvent()
    data object Disconnected : WsEvent()
}
