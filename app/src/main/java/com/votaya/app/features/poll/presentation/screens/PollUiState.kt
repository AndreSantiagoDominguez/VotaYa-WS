package com.votaya.app.features.poll.presentation.screens

import com.votaya.app.features.poll.domain.entities.Room

data class PollUiState(
    val isLoading: Boolean = false,
    val isConnected: Boolean = false,
    val room: Room? = null,
    val error: String? = null,
    val hasVoted: Boolean = false,
    val isPollClosed: Boolean = false
)
