package com.louislu.pennbioinformatics.data.session.remote

import com.google.gson.annotations.SerializedName

data class CreateSessionResponse(
    @SerializedName("session_id") val sessionId: Long
)