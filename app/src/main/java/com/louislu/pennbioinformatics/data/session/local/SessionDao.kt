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

    @Query("SELECT * FROM sessions WHERE localId = :localId LIMIT 1")
    fun getByLocalId(localId: Long): Flow<SessionEntity?>

    @Query("SELECT * FROM sessions WHERE serverId = :serverId LIMIT 1")
    fun getByServerId(serverId: Long): Flow<SessionEntity?>

    @Query("SELECT COUNT(*) FROM sessions WHERE pendingUpload = 1")
    suspend fun getPendingUploadCount(): Int

    @Query("SELECT * FROM sessions WHERE pendingUpload = 1")
    suspend fun getAllPendingUpload(): List<SessionEntity>

    @Upsert
    suspend fun upsert(sessionEntity: SessionEntity): Long

    @Delete
    suspend fun delete(session: SessionEntity)

    @Query("DELETE FROM sessions")
    suspend fun deleteAll()
}