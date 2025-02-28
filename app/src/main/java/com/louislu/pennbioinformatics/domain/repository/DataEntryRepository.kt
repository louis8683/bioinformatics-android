package com.louislu.pennbioinformatics.domain.repository

import com.louislu.pennbioinformatics.domain.model.DataEntry
import kotlinx.coroutines.flow.Flow

interface DataEntryRepository {

    suspend fun upsert(entry: DataEntry)

    fun getAllBySession(sessionId: String): Flow<List<DataEntry>>

    fun getOngoing(): Flow<List<DataEntry>>

    suspend fun syncPendingUploads()
}