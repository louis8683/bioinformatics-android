package com.louislu.pennbioinformatics.domain.repository

import com.louislu.pennbioinformatics.domain.model.Session
import kotlinx.coroutines.flow.Flow

interface SessionRepository {

    fun getAll(): Flow<List<Session>>

    fun getById(localId: Long? = null, serverId: Long? = null): Flow<Session?>

    suspend fun upsert(session: Session): Long

    suspend fun syncPendingUploads()

    suspend fun createSession(
        userId: String,
        groupName: String?,
        className: String,
        schoolName: String,
        deviceName: String?,
        title: String,
        description: String?
    ): Long
}