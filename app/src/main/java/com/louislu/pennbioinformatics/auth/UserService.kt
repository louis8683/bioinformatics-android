package com.louislu.pennbioinformatics.auth

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface UserService {
    @POST("update-user-attributes")
    suspend fun updateUserInfo(
        @Header("Authorization") authHeader: String,
        @Body request: UpdateUserInfoRequest
    )
}