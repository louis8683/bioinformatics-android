package com.louislu.pennbioinformatics.data.entry.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

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
)