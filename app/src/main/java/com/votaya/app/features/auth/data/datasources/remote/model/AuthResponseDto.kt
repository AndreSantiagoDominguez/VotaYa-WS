package com.votaya.app.features.auth.data.datasources.remote.model

data class AuthResponseDto(
    val token: String,
    val user: UserDto
)

data class UserDto(
    val id: String,
    val name: String,
    val email: String
)

data class ErrorResponseDto(
    val error: String
)
