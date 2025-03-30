package com.louislu.pennbioinformatics.data.session.local

import com.louislu.pennbioinformatics.domain.model.Session

fun Session.toEntity(): SessionEntity {
    return SessionEntity(
        localId = localId ?: 0L,  // Room auto-generates if 0
        serverId = serverId,
        userId = userId,
        groupName = groupName,       // updated from groupId
        className = className,
        schoolName = schoolName,
        deviceName = deviceName,     // updated from deviceMac
        startTimestamp = startTimestamp,
        endTimestamp = endTimestamp,
        title = title,
        description = description,
        pendingUpload = pendingUpload
    )
}

fun List<Session>.toEntities(): List<SessionEntity> {
    return map { it.toEntity() }
}