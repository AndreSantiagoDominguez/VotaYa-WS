package com.votaya.app.features.auth.domain.repositories

import com.votaya.app.features.auth.domain.entities.AuthResult

interface AuthRepository {
    suspend fun login(email: String, password: String): AuthResult
    suspend fun register(name: String, email: String, password: String): AuthResult
    suspend fun getStoredToken(): String?
    suspend fun saveToken(token: String)
    suspend fun clearToken()
}
