package com.votaya.app.features.auth.data.datasources.remote.model

data class RegisterRequestDto(
    val name: String,
    val email: String,
    val password: String
)
