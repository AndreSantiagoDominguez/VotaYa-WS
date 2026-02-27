package com.votaya.app.features.poll.domain.usecases

import com.votaya.app.features.poll.domain.repositories.PollRepository
import javax.inject.Inject

class DisconnectWsUseCase @Inject constructor(
    private val repository: PollRepository
) {
    operator fun invoke() {
        repository.disconnect()
    }
}
