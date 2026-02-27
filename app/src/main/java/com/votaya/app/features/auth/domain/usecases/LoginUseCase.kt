package com.votaya.app.features.auth.domain.usecases

import com.votaya.app.features.auth.domain.entities.AuthResult
import com.votaya.app.features.auth.domain.repositories.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Result<AuthResult> {
        return try {
            if (email.isBlank() || password.isBlank()) {
                return Result.failure(Exception("Email y contraseña son requeridos"))
            }
            val result = repository.login(email, password)
            repository.saveToken(result.token)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
