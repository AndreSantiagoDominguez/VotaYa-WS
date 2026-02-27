package com.votaya.app.features.poll.data.datasources.remote.mapper

import com.votaya.app.features.poll.data.datasources.remote.model.WsResponseDto
import com.votaya.app.features.poll.domain.entities.Room
import com.votaya.app.features.poll.domain.entities.VoteOption
import com.votaya.app.features.poll.domain.entities.WsEvent

fun WsResponseDto.toDomainEvent(isCreator: Boolean = false): WsEvent {
    return when (this.type) {
        "room_created" -> WsEvent.RoomCreated(
            room = this.poll?.toRoom(
                code = this.roomCode ?: "",
                isCreator = true
            ) ?: Room(code = this.roomCode ?: "", question = "", options = emptyList(), isCreator = true)
        )
        "room_joined" -> WsEvent.RoomJoined(
            room = this.poll?.toRoom(
                code = this.roomCode ?: "",
                isCreator = false
            ) ?: Room(code = this.roomCode ?: "", question = "", options = emptyList(), isCreator = false)
        )
        "vote_update" -> WsEvent.VoteUpdate(
            room = Room(
                code = this.roomCode ?: "",
                question = "",
                options = this.options?.map { it.toDomain() } ?: emptyList(),
                totalVoters = this.totalVoters ?: 0,
                isCreator = isCreator
            )
        )
        "poll_closed" -> WsEvent.PollClosed(
            room = Room(
                code = this.roomCode ?: "",
                question = "",
                options = (this.finalResults ?: this.options)?.map { it.toDomain() } ?: emptyList(),
                totalVoters = this.totalVoters ?: 0,
                isCreator = isCreator,
                isClosed = true
            )
        )
        "error", "auth_error" -> WsEvent.Error(
            message = this.error ?: this.message ?: "Error del servidor"
        )
        else -> WsEvent.Error(
            message = this.message ?: this.error ?: "Mensaje desconocido: ${this.type}"
        )
    }
}

private fun com.votaya.app.features.poll.data.datasources.remote.model.PollDto.toRoom(
    code: String,
    isCreator: Boolean = false,
    isClosed: Boolean = false
): Room {
    return Room(
        code = code,
        question = this.title ?: "",
        options = this.options?.map { it.toDomain() } ?: emptyList(),
        isCreator = isCreator,
        isClosed = this.isOpen == false || isClosed,
        totalVoters = this.totalVoters ?: 0
    )
}

private fun com.votaya.app.features.poll.data.datasources.remote.model.PollOptionDto.toDomain(): VoteOption {
    return VoteOption(
        text = this.text ?: "",
        votes = this.votes ?: 0
    )
}
