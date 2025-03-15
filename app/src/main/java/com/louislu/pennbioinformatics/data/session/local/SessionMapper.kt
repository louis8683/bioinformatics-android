package com.louislu.pennbioinformatics.data.session.local

import com.louislu.pennbioinformatics.domain.model.Session

fun Session.toEntity(): SessionEntity {
    return SessionEntity(
        localId = localId ?: 0L,  // 0L lets Room auto-generate ID
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

fun List<Session>.toEntities(): List<SessionEntity> {
    return this.map { it.toEntity() }
}