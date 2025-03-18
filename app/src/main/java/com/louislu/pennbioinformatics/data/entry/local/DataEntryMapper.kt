package com.louislu.pennbioinformatics.data.entry.local

import com.louislu.pennbioinformatics.domain.model.DataEntry


fun DataEntry.toEntity(): DataEntryEntity {
    return DataEntryEntity(
        localId = localId ?: 0L,  // 0L lets Room auto-generate ID
        serverId = serverId,
        userId = userId,
        localSessionId = localSessionId,
        remoteSessionId = remoteSessionId,
        timestamp = timestamp,
        latitude = latitude,
        longitude = longitude,
        coLevel = coLevel,
        pm25level = pm25level,
        temperature = temperature,
        humidity = humidity,
//        ongoing = ongoing,
        pendingUpload = pendingUpload
    )
}

fun List<DataEntry>.toEntities(): List<DataEntryEntity> {
    return this.map { it.toEntity() }
}