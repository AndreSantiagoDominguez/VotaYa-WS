package com.votaya.app.features.poll.domain.entities

data class VoteOption(
    val text: String,
    val votes: Int = 0
)
