package com.louislu.pennbioinformatics.data.session.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long,
    val serverId: Long?,
    val userId: String,
    val groupId: String?,
    val deviceMac: String,
    val startTimestamp: Long,
    val endTimestamp: Long?,
    val description: String?,
    val pendingUpload: Boolean
)