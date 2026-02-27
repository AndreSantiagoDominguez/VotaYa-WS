package com.votaya.app.features.poll.domain.usecases

import com.votaya.app.features.poll.domain.repositories.PollRepository
import javax.inject.Inject

class JoinRoomUseCase @Inject constructor(
    private val repository: PollRepository
) {
    operator fun invoke(roomCode: String): Result<Unit> {
        return try {
            if (roomCode.isBlank()) {
                return Result.failure(Exception("El código de sala no puede estar vacío"))
            }
            repository.joinRoom(roomCode.uppercase().trim())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
