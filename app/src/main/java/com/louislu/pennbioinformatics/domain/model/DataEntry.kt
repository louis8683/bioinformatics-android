package com.louislu.pennbioinformatics.domain.model

data class DataEntry(
    val localId: Long?,       // Auto-generated locally
    val serverId: Long?,
    val userId: String,
    val localSessionId: Long?,
    val remoteSessionId: Long?,
    val timestamp: Long,     // Stored as epoch time
    val latitude: Double,
    val longitude: Double,
    val coLevel: Float?,
    val pm25level: Float?,
    val temperature: Float?,
    val humidity: Float?,

    // local flags
    val ongoing: Boolean,
    val pendingUpload: Boolean
)
