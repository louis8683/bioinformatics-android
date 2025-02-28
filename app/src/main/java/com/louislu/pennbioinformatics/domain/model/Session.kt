package com.louislu.pennbioinformatics.domain.model

data class Session(
    val localId: Long?,          // Primary key
    val serverId: Long?,
    val userId: String,        // User owning the session
    val groupId: String?,      // Optional group association
    val deviceMac: String,     // MAC address of the device
    val startTimestamp: Long,  // Stored as epoch milliseconds (TIMESTAMPTZ)
    val endTimestamp: Long?,   // Nullable for ongoing sessions
    val description: String?,   // Optional session notes

    // local flags
    val pendingUpload: Boolean
)