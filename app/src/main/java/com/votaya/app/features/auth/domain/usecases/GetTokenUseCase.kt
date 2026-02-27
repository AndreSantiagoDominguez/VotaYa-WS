package com.votaya.app.features.auth.domain.usecases

import com.votaya.app.features.auth.domain.repositories.AuthRepository
import javax.inject.Inject

class GetTokenUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(): Result<String?> {
        return try {
            Result.success(repository.getStoredToken())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
