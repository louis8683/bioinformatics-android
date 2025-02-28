package com.louislu.pennbioinformatics.data.entry.remote

import com.google.gson.annotations.SerializedName
import com.louislu.pennbioinformatics.domain.model.DataEntry
import java.time.Instant

data class DataEntryDto(
    val id: Long,
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("session_id")
    val sessionId: Long,
    val timestamp: String,  // ISO 8601 string from API
    val latitude: Double,
    val longitude: Double,
    @SerializedName("co_level")
    val coLevel: Float?,
    @SerializedName("pm2_5_level")
    val pm25Level: Float?,
    val temperature: Float?,
    val humidity: Float?
) {
    fun toDomainModel(): DataEntry {
        return DataEntry(
            localId = null,
            serverId = id,
            userId = userId,
            localSessionId = null,
            remoteSessionId = sessionId,
            timestamp = Instant.parse(timestamp).toEpochMilli(), // Convert API timestamp to epoch
            latitude = latitude,
            longitude = longitude,
            coLevel = coLevel,
            pm25level = pm25Level,
            temperature = temperature,
            humidity = humidity,

            // local flags
            ongoing = false,
            pendingUpload = false
        )
    }
}