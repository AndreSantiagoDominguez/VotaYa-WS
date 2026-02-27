package com.votaya.app.features.auth.data.repositories

import android.util.Log
import com.votaya.app.features.auth.data.datasources.local.TokenDataStore
import com.votaya.app.features.auth.data.datasources.remote.api.AuthApi
import com.votaya.app.features.auth.data.datasources.remote.mapper.toDomain
import com.votaya.app.features.auth.data.datasources.remote.model.LoginRequestDto
import com.votaya.app.features.auth.data.datasources.remote.model.RegisterRequestDto
import com.votaya.app.features.auth.domain.entities.AuthResult
import com.votaya.app.features.auth.domain.repositories.AuthRepository
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val api: AuthApi,
    private val tokenDataStore: TokenDataStore
) : AuthRepository {

    override suspend fun login(email: String, password: String): AuthResult {
        val response = api.login(LoginRequestDto(email, password))
        Log.d("VotaYa", "Login exitoso: ${response.user.name}")
        return response.toDomain()
    }

    override suspend fun register(name: String, email: String, password: String): AuthResult {
        val response = api.register(RegisterRequestDto(name, email, password))
        Log.d("VotaYa", "Registro exitoso: ${response.user.name}")
        return response.toDomain()
    }

    override suspend fun getStoredToken(): String? {
        return tokenDataStore.getToken()
    }

    override suspend fun saveToken(token: String) {
        tokenDataStore.saveToken(token)
    }

    override suspend fun clearToken() {
        tokenDataStore.clearToken()
    }
}
