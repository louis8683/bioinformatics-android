package com.louislu.pennbioinformatics.domain.model

import java.util.UUID
import kotlin.random.Random

data class DataEntry(
    val localId: Long?,       // Auto-generated locally
    val serverId: Long?,
    val userId: String,
    val localSessionId: Long?,
    val remoteSessionId: Long?,
    val timestamp: Long,     // Stored as epoch time
    val latitude: Double?,
    val longitude: Double?,
    val coLevel: Float?,
    val pm25level: Float?,
    val temperature: Float?,
    val humidity: Float?,

    // local flags
//    val ongoing: Boolean,
    val pendingUpload: Boolean
)

fun generateFakeDataEntries(count: Int = 5): List<DataEntry> {
    return List(count) {
        DataEntry(
            localId = it.toLong(),
            serverId = if (Random.nextBoolean()) Random.nextLong(1000, 9999) else null,  // Simulate unsynced data
            userId = UUID.randomUUID().toString(),  // Unique user identifier
            localSessionId = Random.nextLong(1000, 9999),
            remoteSessionId = if (Random.nextBoolean()) Random.nextLong(1000, 9999) else null,
            timestamp = System.currentTimeMillis() - Random.nextLong(0, 1000000),  // Random past timestamps
            latitude = Random.nextDouble(-90.0, 90.0),
            longitude = Random.nextDouble(-180.0, 180.0),
            coLevel = Random.nextFloat() * 10,  // Simulating 0 - 10 ppm
            pm25level = Random.nextFloat() * 150,  // Simulating 0 - 150 µg/m³
            temperature = Random.nextFloat() * 40,  // Simulating 0 - 40°C
            humidity = Random.nextFloat() * 100,  // Simulating 0 - 100%
//            ongoing = Random.nextBoolean(),
            pendingUpload = Random.nextBoolean()
        )
    }
}
