package com.louislu.pennbioinformatics.data.session.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.louislu.pennbioinformatics.domain.model.Session

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long,
    val serverId: Long?,
    val userId: String,
    val groupName: String?,
    val className: String,
    val schoolName: String,
    val deviceName: String?,
    val startTimestamp: Long,
    val endTimestamp: Long?,
    val title: String,
    val description: String?,
    val pendingUpload: Boolean
) {
    fun toDomainModel(): Session {
        return Session(
            localId = localId,
            serverId = serverId,
            userId = userId,
            groupName = groupName,
            className = className,
            schoolName = schoolName,
            deviceName = deviceName,
            startTimestamp = startTimestamp,
            endTimestamp = endTimestamp,
            title = title,
            description = description,
            pendingUpload = pendingUpload
        )
    }
}