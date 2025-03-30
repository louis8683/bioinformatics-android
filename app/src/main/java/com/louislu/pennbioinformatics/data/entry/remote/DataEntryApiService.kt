package com.louislu.pennbioinformatics.data.entry.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface DataEntryApiService {
    @POST("sessions/{sessionId}/data/batch")
    suspend fun uploadDataBatch(
        @Header("Authorization") bearerToken: String,
        @Path("sessionId") sessionId: Long,
        @Body request: DataEntryUploadRequest
    ): DataEntryUploadResponse
}



