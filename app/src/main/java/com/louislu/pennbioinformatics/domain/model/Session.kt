package com.louislu.pennbioinformatics.domain.model

data class Session(
    val localId: Long?,          // Local Room primary key
    val serverId: Long?,         // API session ID (nullable if offline)
    val userId: String,          // Cognito user ID
    val groupName: String?,        // Optional group association
    val className: String,       // Class at time of session creation
    val schoolName: String,      // School at time of session creation
    val deviceName: String?,      // Device name
    val startTimestamp: Long,    // Epoch millis
    val endTimestamp: Long?,     // Nullable if ongoing
    val title: String,           // Session title (required)
    val description: String?,    // Optional

    // Local-only
    val pendingUpload: Boolean   // True if not yet uploaded to server
)