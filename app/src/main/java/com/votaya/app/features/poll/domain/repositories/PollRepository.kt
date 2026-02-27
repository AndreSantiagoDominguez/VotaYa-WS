package com.votaya.app.features.poll.domain.repositories

import com.votaya.app.features.poll.domain.entities.WsEvent
import kotlinx.coroutines.flow.Flow

interface PollRepository {
    fun connect(token: String): Flow<WsEvent>
    fun createRoom(question: String, options: List<String>)
    fun joinRoom(roomCode: String)
    fun vote(roomCode: String, optionIndex: Int)
    fun closePoll(roomCode: String)
    fun disconnect()
}
