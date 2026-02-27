package com.votaya.app.features.poll.data.datasources.remote.websocket

import android.util.Log
import com.google.gson.Gson
import com.votaya.app.BuildConfig
import com.votaya.app.features.poll.data.datasources.remote.mapper.toDomainEvent
import com.votaya.app.features.poll.data.datasources.remote.model.ClosePollDto
import com.votaya.app.features.poll.data.datasources.remote.model.CreateRoomDto
import com.votaya.app.features.poll.data.datasources.remote.model.JoinRoomDto
import com.votaya.app.features.poll.data.datasources.remote.model.VoteDto
import com.votaya.app.features.poll.data.datasources.remote.model.WsResponseDto
import com.votaya.app.features.poll.domain.entities.WsEvent
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PollWebSocketService @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) {
    companion object {
        private const val TAG = "VotaYa-WS"
    }

    private var webSocket: WebSocket? = null
    private var isCreator: Boolean = false

    fun connect(token: String): Flow<WsEvent> = callbackFlow {
        val wsUrl = "${BuildConfig.WS_URL}?token=$token"
        Log.d(TAG, "Conectando a: $wsUrl")

        val request = Request.Builder().url(wsUrl).build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket conectado")
                this@PollWebSocketService.webSocket = webSocket
                trySend(WsEvent.Connected)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "Mensaje recibido: $text")
                try {
                    val dto = gson.fromJson(text, WsResponseDto::class.java)
                    val event = dto.toDomainEvent(isCreator)

                    // Track creator status
                    if (event is WsEvent.RoomCreated) isCreator = true

                    trySend(event)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parseando mensaje: ${e.message}")
                    trySend(WsEvent.Error("Error al procesar mensaje"))
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket error: ${t.message}")
                trySend(WsEvent.Error(t.message ?: "Error de conexión"))
                trySend(WsEvent.Disconnected)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket cerrado: $reason")
                trySend(WsEvent.Disconnected)
            }
        })

        awaitClose {
            Log.d(TAG, "Flow cerrado, desconectando WS")
            disconnect()
        }
    }

    fun send(message: Any) {
        val json = gson.toJson(message)
        Log.d(TAG, "Enviando: $json")
        webSocket?.send(json)
    }

    fun createRoom(question: String, options: List<String>) {
        isCreator = true
        send(CreateRoomDto(title = question, options = options))
    }

    fun joinRoom(roomCode: String) {
        isCreator = false
        send(JoinRoomDto(roomCode = roomCode))
    }

    fun vote(roomCode: String, optionIndex: Int) {
        send(VoteDto(roomCode = roomCode, optionIndex = optionIndex))
    }

    fun closePoll(roomCode: String) {
        send(ClosePollDto(roomCode = roomCode))
    }

    fun disconnect() {
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
        isCreator = false
    }
}
