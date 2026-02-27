package com.votaya.app.features.auth.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.votaya.app.features.auth.domain.usecases.GetTokenUseCase
import com.votaya.app.features.auth.domain.usecases.LoginUseCase
import com.votaya.app.features.auth.domain.usecases.LogoutUseCase
import com.votaya.app.features.auth.domain.usecases.RegisterUseCase
import com.votaya.app.features.auth.presentation.screens.AuthUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
    private val getTokenUseCase: GetTokenUseCase,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()

    init {
        checkStoredToken()
    }

    private fun checkStoredToken() {
        viewModelScope.launch {
            val result = getTokenUseCase()
            result.fold(
                onSuccess = { token ->
                    _uiState.update { it.copy(token = token) }
                },
                onFailure = { /* No token stored */ }
            )
        }
    }

    fun login(email: String, password: String) {
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            val result = loginUseCase(email, password)
            _uiState.update { currentState ->
                result.fold(
                    onSuccess = { authResult ->
                        currentState.copy(
                            isLoading = false,
                            token = authResult.token,
                            user = authResult.user,
                            isSuccess = true
                        )
                    },
                    onFailure = { error ->
                        currentState.copy(
                            isLoading = false,
                            error = error.message ?: "Error desconocido"
                        )
                    }
                )
            }
        }
    }

    fun register(name: String, email: String, password: String) {
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            val result = registerUseCase(name, email, password)
            _uiState.update { currentState ->
                result.fold(
                    onSuccess = { authResult ->
                        currentState.copy(
                            isLoading = false,
                            token = authResult.token,
                            user = authResult.user,
                            isSuccess = true
                        )
                    },
                    onFailure = { error ->
                        currentState.copy(
                            isLoading = false,
                            error = error.message ?: "Error desconocido"
                        )
                    }
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            logoutUseCase()
            _uiState.update { AuthUiState() }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
