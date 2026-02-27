package com.votaya.app.features.poll.domain.usecases

import com.votaya.app.features.poll.domain.repositories.PollRepository
import javax.inject.Inject

class CreateRoomUseCase @Inject constructor(
    private val repository: PollRepository
) {
    operator fun invoke(question: String, options: List<String>): Result<Unit> {
        return try {
            if (question.isBlank()) {
                return Result.failure(Exception("La pregunta no puede estar vacía"))
            }
            val validOptions = options.filter { it.isNotBlank() }
            if (validOptions.size < 2) {
                return Result.failure(Exception("Se necesitan al menos 2 opciones"))
            }
            repository.createRoom(question, validOptions)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
