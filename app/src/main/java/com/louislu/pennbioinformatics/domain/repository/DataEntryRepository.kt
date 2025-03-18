package com.louislu.pennbioinformatics.domain.repository

import com.louislu.pennbioinformatics.domain.model.DataEntry
import kotlinx.coroutines.flow.Flow

interface DataEntryRepository {

    suspend fun upsert(entry: DataEntry): Long

    fun getAllBySession(localSessionId: Long? = null, remoteSessionId: Long? = null): Flow<List<DataEntry>>

//    fun getOngoing(): Flow<List<DataEntry>>

    fun getLatestBySession(localSessionId: Long? = null, remoteSessionId: Long? = null): Flow<DataEntry?>

    suspend fun syncPendingUploads()
}