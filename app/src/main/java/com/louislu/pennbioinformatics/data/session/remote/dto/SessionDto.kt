package com.louislu.pennbioinformatics.data.session.remote.dto

import com.google.gson.annotations.SerializedName
import com.louislu.pennbioinformatics.domain.model.Session
import java.time.Instant

data class SessionDto(
    val id: Long,
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("group_id")
    val groupId: String?,
    @SerializedName("device_mac")
    val deviceMac: String,  // API uses MACADDR, but we use String
    @SerializedName("start_timestamp")
    val startTimestamp: String,  // API provides TIMESTAMPTZ (ISO 8601 format)
    @SerializedName("end_timestamp")
    val endTimestamp: String?,  // Nullable
    val description: String?
) {
    fun toDomainModel(): Session {
        return Session(
            localId = null,
            serverId = id,
            userId = userId,
            groupId = groupId,
            deviceMac = deviceMac,
            startTimestamp = Instant.parse(startTimestamp).toEpochMilli(), // Convert TIMESTAMPTZ to Long
            endTimestamp = endTimestamp?.let { Instant.parse(it).toEpochMilli() }, // Convert if not null
            description = description,

            // local flags
            pendingUpload = false
        )
    }
}