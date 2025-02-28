package com.louislu.pennbioinformatics.data.session.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY startTimestamp DESC")
    fun getAll(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE localId = :sessionId LIMIT 1")
    fun getById(sessionId: Long): Flow<SessionEntity?>

    @Query("SELECT * FROM sessions WHERE pendingUpload = 1")
    fun getAllPendingUpload(): Flow<List<SessionEntity>>

    @Upsert
    suspend fun upsert(sessionEntity: SessionEntity): Long

    @Delete
    suspend fun delete(session: SessionEntity)

    @Query("DELETE FROM sessions")
    suspend fun deleteAll()
}