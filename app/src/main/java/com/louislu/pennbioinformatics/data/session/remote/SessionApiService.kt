package com.louislu.pennbioinformatics.data.session.remote

import com.louislu.pennbioinformatics.data.session.remote.dto.SessionDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface SessionApiService {
    @POST("sessions")
    suspend fun createSession(
        @Header("Authorization") bearerToken: String,
        @Body request: CreateSessionRequest
    ): CreateSessionResponse

    @GET("sessions/{sessionId}")
    suspend fun getSessionById(
        @Header("Authorization") bearerToken: String,
        @Path("sessionId") sessionId: Long
    ): SessionDto
}