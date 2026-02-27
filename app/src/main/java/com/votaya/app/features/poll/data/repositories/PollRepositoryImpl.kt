package com.votaya.app.features.poll.data.repositories

import com.votaya.app.features.poll.data.datasources.remote.websocket.PollWebSocketService
import com.votaya.app.features.poll.domain.entities.WsEvent
import com.votaya.app.features.poll.domain.repositories.PollRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class PollRepositoryImpl @Inject constructor(
    private val webSocketService: PollWebSocketService
) : PollRepository {

    override fun connect(token: String): Flow<WsEvent> {
        return webSocketService.connect(token)
    }

    override fun createRoom(question: String, options: List<String>) {
        webSocketService.createRoom(question, options)
    }

    override fun joinRoom(roomCode: String) {
        webSocketService.joinRoom(roomCode)
    }

    override fun vote(roomCode: String, optionIndex: Int) {
        webSocketService.vote(roomCode, optionIndex)
    }

    override fun closePoll(roomCode: String) {
        webSocketService.closePoll(roomCode)
    }

    override fun disconnect() {
        webSocketService.disconnect()
    }
}
