package com.louislu.pennbioinformatics.data.entry

import com.louislu.pennbioinformatics.auth.AuthRepository
import com.louislu.pennbioinformatics.data.entry.local.DataEntryDao
import com.louislu.pennbioinformatics.data.entry.local.toEntity
import com.louislu.pennbioinformatics.data.entry.remote.DataEntryApiService
import com.louislu.pennbioinformatics.data.entry.remote.DataEntryUploadRequest
import com.louislu.pennbioinformatics.data.entry.remote.toUploadItem
import com.louislu.pennbioinformatics.domain.model.DataEntry
import com.louislu.pennbioinformatics.domain.repository.DataEntryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import timber.log.Timber
import javax.inject.Inject

class DataEntryRepositoryImpl @Inject constructor(
    private val dataEntryDao: DataEntryDao,
    private val dataEntryApiService: DataEntryApiService,
    private val authRepository: AuthRepository
) : DataEntryRepository {

    /*** Insert or Update a Data Entry ***/
    override suspend fun upsert(entry: DataEntry): Long {
        return dataEntryDao.upsert(entry.toEntity())
    }

    /*** Retrieve All Data Entries for a Given Session ***/
    override fun getAllBySession(
        localSessionId: Long?,
        remoteSessionId: Long?
    ): Flow<List<DataEntry>> {
        // TODO: retrieve data from remote if not exist in local
        return dataEntryDao.getAllBySession(localSessionId, remoteSessionId)
            .map { entities -> entities.map { it.toDomainModel() } }
    }

    /*** Retrieve the Latest Data Entry for a Given Session ***/
    override fun getLatestBySession(
        localSessionId: Long?,
        remoteSessionId: Long?
    ): Flow<DataEntry?> {
        return dataEntryDao.getLatestBySession(localSessionId, remoteSessionId)
            .map { it?.toDomainModel() } // Convert entity to domain model, return null if not found
    }

    /*** `syncPendingUploads()` is Not Implemented Yet ***/
    override suspend fun syncPendingUploads() {
        val pending = dataEntryDao.getAllPendingUpload()

        // Convert to domain model and group by remoteSessionId
        val grouped = pending
            .map { it.toDomainModel() }
            .filter { it.pendingUpload && it.remoteSessionId != null }
            .groupBy { it.remoteSessionId!! }

        val token = authRepository.getAccessToken()

        for ((sessionId, entries) in grouped) {
            try {
                val uploadItems = entries.map { it.toUploadItem() }

                val request = DataEntryUploadRequest(entries = uploadItems)

                val response = dataEntryApiService.uploadDataBatch(
                    bearerToken = "Bearer $token",
                    sessionId = sessionId,
                    request = request
                )

                if (response.insertedIds.size != entries.size) {
                    Timber.e("Mismatch: uploaded ${entries.size} but received ${response.insertedIds.size} IDs.")
                    continue // skip saving if response doesn't match
                }

                // Assign returned IDs back to entries by index
                entries.zip(response.insertedIds).forEach { (entry, serverId) ->
                    val updated = entry.copy(
                        serverId = serverId,
                        pendingUpload = false
                    )
                    dataEntryDao.upsert(updated.toEntity())
                }

                Timber.i("Synced ${entries.size} entries for sessionId=$sessionId")

            } catch (e: Exception) {
                Timber.e(e, "Failed to sync entries for sessionId=$sessionId")
            }
        }
    }
}