package com.louislu.pennbioinformatics.data.entry.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface DataEntryDao {

    @Upsert
    suspend fun upsert(entry: DataEntryEntity): Long

    @Query("""
    SELECT * FROM data_entries 
    WHERE 
        (:localSessionId IS NOT NULL OR :remoteSessionId IS NOT NULL) -- Ensure at least one ID is provided
        AND (localSessionId = :localSessionId OR :localSessionId IS NULL)
        AND (remoteSessionId = :remoteSessionId OR :remoteSessionId IS NULL)
    ORDER BY timestamp ASC
    """)
    fun getAllBySession(localSessionId: Long? = null, remoteSessionId: Long? = null): Flow<List<DataEntryEntity>>

    @Query("""
        SELECT * FROM data_entries 
        WHERE 
            (:localSessionId IS NOT NULL OR :remoteSessionId IS NOT NULL)
            AND (localSessionId = :localSessionId OR :localSessionId IS NULL)
            AND (remoteSessionId = :remoteSessionId OR :remoteSessionId IS NULL)
        ORDER BY timestamp DESC 
        LIMIT 1
    """)
    fun getLatestBySession(localSessionId: Long? = null, remoteSessionId: Long? = null): Flow<DataEntryEntity?>

//    @Query("SELECT * FROM data_entries WHERE ongoing = 1")
//    fun getAllOngoing(): Flow<List<DataEntryEntity>>


    @Query("SELECT COUNT(*) FROM data_entries WHERE pendingUpload = 1")
    suspend fun getPendingUploadCount(): Int

    @Query("SELECT * FROM data_entries WHERE pendingUpload = 1")
    suspend fun getAllPendingUpload(): List<DataEntryEntity>
}