package com.louislu.pennbioinformatics.data.entry.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.louislu.pennbioinformatics.domain.model.DataEntry

@Entity(
    tableName = "data_entries",
    indices = [Index(value = ["localSessionId"])] // query optimization
)
data class DataEntryEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long, // Room auto-generates ID
    val serverId: Long?, // API-provided ID, nullable for local entries
    val userId: String,
    val localSessionId: Long?,
    val remoteSessionId: Long?, // No foreign key, just a reference
    val timestamp: Long, // Epoch time
    val latitude: Double,
    val longitude: Double,
    val coLevel: Float?,
    val pm25level: Float?,
    val temperature: Float?,
    val humidity: Float?,

    // local flags
    val ongoing: Boolean,
    val pendingUpload: Boolean
) {
    fun toDomainModel(): DataEntry {
        return DataEntry(
            localId = localId,
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
            ongoing = ongoing,
            pendingUpload = pendingUpload
        )
    }
}