package com.votaya.app.features.poll.presentation.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.votaya.app.features.auth.domain.usecases.GetTokenUseCase
import com.votaya.app.features.poll.domain.entities.WsEvent
import com.votaya.app.features.poll.domain.usecases.ClosePollUseCase
import com.votaya.app.features.poll.domain.usecases.ConnectWsUseCase
import com.votaya.app.features.poll.domain.usecases.CreateRoomUseCase
import com.votaya.app.features.poll.domain.usecases.DisconnectWsUseCase
import com.votaya.app.features.poll.domain.usecases.JoinRoomUseCase
import com.votaya.app.features.poll.domain.usecases.VoteUseCase
import com.votaya.app.features.poll.presentation.screens.PollUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PollViewModel @Inject constructor(
    private val connectWsUseCase: ConnectWsUseCase,
    private val disconnectWsUseCase: DisconnectWsUseCase,
    private val createRoomUseCase: CreateRoomUseCase,
    private val joinRoomUseCase: JoinRoomUseCase,
    private val voteUseCase: VoteUseCase,
    private val closePollUseCase: ClosePollUseCase,
    private val getTokenUseCase: GetTokenUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PollUiState())
    val uiState = _uiState.asStateFlow()

    private var pendingAction: (() -> Unit)? = null
    private var isWsConnected = false

    fun connectAndCreateRoom(question: String, options: List<String>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val tokenResult = getTokenUseCase()
            val token = tokenResult.getOrNull()

            if (token == null) {
                _uiState.update { it.copy(isLoading = false, error = "No hay sesión activa") }
                return@launch
            }

            pendingAction = {
                viewModelScope.launch {
                    val result = createRoomUseCase(question, options)
                    result.onFailure { e ->
                        _uiState.update { it.copy(isLoading = false, error = e.message) }
                    }
                }
            }

            connectAndListen(token)
        }
    }

    fun connectAndJoinRoom(roomCode: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val tokenResult = getTokenUseCase()
            val token = tokenResult.getOrNull()

            if (token == null) {
                _uiState.update { it.copy(isLoading = false, error = "No hay sesión activa") }
                return@launch
            }

            pendingAction = {
                viewModelScope.launch {
                    val result = joinRoomUseCase(roomCode)
                    result.onFailure { e ->
                        _uiState.update { it.copy(isLoading = false, error = e.message) }
                    }
                }
            }

            connectAndListen(token)
        }
    }

    fun vote(roomCode: String, optionIndex: Int) {
        val result = voteUseCase(roomCode, optionIndex)
        result.onFailure { e ->
            _uiState.update { it.copy(error = e.message) }
        }
        _uiState.update { it.copy(hasVoted = true) }
    }

    fun closePoll(roomCode: String) {
        val result = closePollUseCase(roomCode)
        result.onFailure { e ->
            _uiState.update { it.copy(error = e.message) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun connectAndListen(token: String) {
        if (isWsConnected) {
            // Si ya está conectado, ejecutar acción directamente
            pendingAction?.invoke()
            pendingAction = null
            return
        }
        isWsConnected = true

        viewModelScope.launch {
            connectWsUseCase(token).collect { event ->
                when (event) {
                    is WsEvent.Connected -> {
                        Log.d("PollViewModel", "WS Conectado, ejecutando acción pendiente")
                        _uiState.update { it.copy(isConnected = true) }
                        pendingAction?.invoke()
                        pendingAction = null
                    }
                    is WsEvent.RoomCreated -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isConnected = true,
                                room = event.room
                            )
                        }
                    }
                    is WsEvent.RoomJoined -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isConnected = true,
                                room = event.room
                            )
                        }
                    }
                    is WsEvent.VoteUpdate -> {
                        _uiState.update {
                            it.copy(
                                room = event.room.copy(
                                    isCreator = it.room?.isCreator ?: false
                                )
                            )
                        }
                    }
                    is WsEvent.PollClosed -> {
                        _uiState.update {
                            it.copy(
                                isPollClosed = true,
                                room = event.room.copy(
                                    isCreator = it.room?.isCreator ?: false,
                                    isClosed = true
                                )
                            )
                        }
                    }
                    is WsEvent.Error -> {
                        pendingAction = null
                        _uiState.update {
                            it.copy(isLoading = false, error = event.message)
                        }
                    }
                    is WsEvent.Disconnected -> {
                        isWsConnected = false
                        pendingAction = null
                        _uiState.update { it.copy(isConnected = false, isLoading = false) }
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        disconnectWsUseCase()
    }
}
