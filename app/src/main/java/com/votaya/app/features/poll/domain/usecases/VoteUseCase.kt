package com.votaya.app.features.poll.domain.usecases

import com.votaya.app.features.poll.domain.repositories.PollRepository
import javax.inject.Inject

class VoteUseCase @Inject constructor(
    private val repository: PollRepository
) {
    operator fun invoke(roomCode: String, optionIndex: Int): Result<Unit> {
        return try {
            repository.vote(roomCode, optionIndex)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
