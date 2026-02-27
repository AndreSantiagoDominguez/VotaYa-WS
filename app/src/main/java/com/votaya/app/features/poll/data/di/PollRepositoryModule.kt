package com.votaya.app.features.poll.data.di

import com.votaya.app.features.poll.data.repositories.PollRepositoryImpl
import com.votaya.app.features.poll.domain.repositories.PollRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PollRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindPollRepository(
        pollRepositoryImpl: PollRepositoryImpl
    ): PollRepository
}
