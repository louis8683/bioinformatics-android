package com.louislu.pennbioinformatics.domain.repository

import com.louislu.pennbioinformatics.domain.model.Session
import kotlinx.coroutines.flow.Flow

interface SessionRepository {

    fun getAllSessions(): Flow<List<Session>>

    fun getSessionById(sessionId: Long): Flow<Session?>

    suspend fun createSession(session: Session)

    suspend fun syncPendingUploads()
}