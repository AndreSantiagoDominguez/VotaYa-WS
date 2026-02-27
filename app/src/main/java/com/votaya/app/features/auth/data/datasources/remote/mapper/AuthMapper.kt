package com.votaya.app.features.auth.data.datasources.remote.mapper

import com.votaya.app.features.auth.data.datasources.remote.model.AuthResponseDto
import com.votaya.app.features.auth.data.datasources.remote.model.UserDto
import com.votaya.app.features.auth.domain.entities.AuthResult
import com.votaya.app.features.auth.domain.entities.User

fun AuthResponseDto.toDomain(): AuthResult {
    return AuthResult(
        token = this.token,
        user = this.user.toDomain()
    )
}

fun UserDto.toDomain(): User {
    return User(
        id = this.id,
        name = this.name,
        email = this.email
    )
}
