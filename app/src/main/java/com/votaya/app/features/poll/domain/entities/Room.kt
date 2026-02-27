package com.votaya.app.features.poll.domain.entities

data class Room(
    val code: String,
    val question: String,
    val options: List<VoteOption>,
    val isCreator: Boolean = false,
    val isClosed: Boolean = false,
    val totalVoters: Int = 0,
    val hasVoted: Boolean = false
)
