package com.votaya.app.features.auth.domain.usecases

import com.votaya.app.features.auth.domain.entities.AuthResult
import com.votaya.app.features.auth.domain.repositories.AuthRepository
import javax.inject.Inject

class RegisterUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(name: String, email: String, password: String): Result<AuthResult> {
        return try {
            if (name.isBlank() || email.isBlank() || password.isBlank()) {
                return Result.failure(Exception("Todos los campos son requeridos"))
            }
            if (password.length < 6) {
                return Result.failure(Exception("La contraseña debe tener al menos 6 caracteres"))
            }
            val result = repository.register(name, email, password)
            repository.saveToken(result.token)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
