package com.votaya.app.features.auth.domain.entities

data class AuthResult(
    val token: String,
    val user: User
)
