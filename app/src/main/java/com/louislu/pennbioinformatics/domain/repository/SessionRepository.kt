package com.louislu.pennbioinformatics.domain.repository

import com.louislu.pennbioinformatics.domain.model.Session
import kotlinx.coroutines.flow.Flow

interface SessionRepository {

    fun getAll(): Flow<List<Session>>

    fun getById(sessionId: Long): Flow<Session?>

    suspend fun upsert(session: Session): Long

    suspend fun syncPendingUploads()
}