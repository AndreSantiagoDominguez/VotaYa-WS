package com.votaya.app.features.poll.domain.usecases

import com.votaya.app.features.poll.domain.repositories.PollRepository
import javax.inject.Inject

class ClosePollUseCase @Inject constructor(
    private val repository: PollRepository
) {
    operator fun invoke(roomCode: String): Result<Unit> {
        return try {
            repository.closePoll(roomCode)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
