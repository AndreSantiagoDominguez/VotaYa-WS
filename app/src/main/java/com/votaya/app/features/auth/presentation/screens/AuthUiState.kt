package com.votaya.app.features.auth.presentation.screens

import com.votaya.app.features.auth.domain.entities.User

data class AuthUiState(
    val isLoading: Boolean = false,
    val token: String? = null,
    val user: User? = null,
    val error: String? = null,
    val isSuccess: Boolean = false
)
