package com.louislu.pennbioinformatics.domain.repository

import com.louislu.pennbioinformatics.domain.model.DataEntry
import kotlinx.coroutines.flow.Flow

interface BleRepository {

    fun isConnected(): Flow<Boolean>

    suspend fun connect(deviceAddress: String): Boolean

    suspend fun updateName(deviceName: String)

    suspend fun disconnect(): Boolean

    fun observeSensorData(): Flow<DataEntry>
}