package com.louislu.pennbioinformatics.data.session.remote.dto

import com.google.gson.annotations.SerializedName
import com.louislu.pennbioinformatics.domain.model.Session
import java.time.Instant

data class SessionDto(
    val id: Long, // API-generated session ID

    @SerializedName("user_id")
    val userId: String,

    @SerializedName("group_name")
    val groupName: String?,

    @SerializedName("class_name")
    val className: String,

    @SerializedName("school_name")
    val schoolName: String,

    @SerializedName("device_name")
    val deviceName: String?,

    @SerializedName("start_timestamp")
    val startTimestamp: Long,  // Epoch millis from backend

    @SerializedName("end_timestamp")
    val endTimestamp: Long?,   // Nullable if ongoing

    val title: String,

    val description: String?
) {
    fun toDomainModel(): Session {
        return Session(
            localId = null, // Will be set by Room
            serverId = id,
            userId = userId,
            groupName = groupName, // Re-mapped to groupId in domain model
            className = className,
            schoolName = schoolName,
            deviceName = deviceName, // Safe fallback
            startTimestamp = startTimestamp,
            endTimestamp = endTimestamp,
            title = title,
            description = description,

            // local flags
            pendingUpload = false
        )
    }
}