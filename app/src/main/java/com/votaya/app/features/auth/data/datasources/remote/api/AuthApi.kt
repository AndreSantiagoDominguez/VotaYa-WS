package com.votaya.app.features.auth.data.datasources.remote.api

import com.votaya.app.features.auth.data.datasources.remote.model.AuthResponseDto
import com.votaya.app.features.auth.data.datasources.remote.model.LoginRequestDto
import com.votaya.app.features.auth.data.datasources.remote.model.RegisterRequestDto
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("login")
    suspend fun login(@Body request: LoginRequestDto): AuthResponseDto

    @POST("register")
    suspend fun register(@Body request: RegisterRequestDto): AuthResponseDto
}
