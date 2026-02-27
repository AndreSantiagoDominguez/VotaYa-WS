package com.votaya.app.features.poll.domain.usecases

import com.votaya.app.features.poll.domain.entities.WsEvent
import com.votaya.app.features.poll.domain.repositories.PollRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ConnectWsUseCase @Inject constructor(
    private val repository: PollRepository
) {
    operator fun invoke(token: String): Flow<WsEvent> {
        return repository.connect(token)
    }
}
