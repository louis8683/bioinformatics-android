package com.louislu.pennbioinformatics.data.session.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.louislu.pennbioinformatics.domain.model.Session

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
) {
    fun toDomainModel(): Session {
        return Session(
            localId = localId,
            serverId = serverId,
            userId = userId,
            groupId = groupId,
            deviceMac = deviceMac,
            startTimestamp = startTimestamp,
            endTimestamp = endTimestamp,
            description = description,
            pendingUpload = pendingUpload
        )
    }
}