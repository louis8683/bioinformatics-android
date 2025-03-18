package com.louislu.pennbioinformatics.data.entry.local

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.louislu.pennbioinformatics.data.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException


@RunWith(AndroidJUnit4::class)
class DataEntryDaoTest {

    private lateinit var dataEntryDao: DataEntryDao
    private lateinit var database: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context, AppDatabase::class.java).build()
        dataEntryDao = database.dataEntryDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }

    /*** TEST CASES ***/

    /*** upsert() ***/

    @Test
    fun upsert_NewDataEntry_ShouldInsertSuccessfully() = runBlocking {
        // Arrange
        val entry = sampleDataEntry()

        // Act
        dataEntryDao.upsert(entry)
        val result = dataEntryDao.getAllBySession(entry.localSessionId).first()

        // Assert
        Assert.assertEquals(1, result.size)
        Assert.assertEquals(entry.userId, result.first().userId)
    }

    @Test
    fun upsert_ExistingDataEntry_ShouldUpdateSuccessfully() = runBlocking {
        // Arrange
        val entry = sampleDataEntry(localSessionId = 1L)
        val entryId = dataEntryDao.upsert(entry)

        val updatedEntry = entry.copy(localId = entryId, coLevel = 0.8f, pendingUpload = false)

        // Act
        dataEntryDao.upsert(updatedEntry)
        val result = dataEntryDao.getAllBySession(localSessionId = entry.localSessionId).first()

        Log.d("DatabaseTest", "Entries in DB: $result")

        // Assert
        Assert.assertEquals(1, result.size) // Ensures no duplicates
        Assert.assertEquals(0.8f, result.first().coLevel)
        Assert.assertFalse(result.first().pendingUpload)
    }

    @Test
    fun upsert_MultipleEntries_ShouldStoreAllCorrectly() = runBlocking {
        // Arrange
        val entry1 = sampleDataEntry(localSessionId = 1L, userId = "user1")
        val entry2 = sampleDataEntry(localSessionId = 1L, userId = "user2")

        // Act
        dataEntryDao.upsert(entry1)
        dataEntryDao.upsert(entry2)
        val result = dataEntryDao.getAllBySession(1L).first()

        // Assert
        Assert.assertEquals(2, result.size)
        Assert.assertTrue(result.any { it.userId == "user1" })
        Assert.assertTrue(result.any { it.userId == "user2" })
    }

    /*** getAllBySession() ***/

    @Test
    fun getAllBySession_WhenNoEntriesExist_ShouldReturnEmptyList() = runBlocking {
        // Act
        val result = dataEntryDao.getAllBySession(localSessionId = 1L).first()

        // Assert
        Assert.assertTrue(result.isEmpty())
    }

    @Test
    fun getAllBySession_WhenOneEntryExists_ShouldReturnCorrectResult() = runBlocking {
        // Arrange
        val entry = sampleDataEntry(localSessionId = 1L)
        val entryId = dataEntryDao.upsert(entry)

        // Act
        val result = dataEntryDao.getAllBySession(localSessionId = 1L).first()

        // Assert
        Assert.assertEquals(1, result.size)
        Assert.assertEquals(entryId, result.first().localId)
    }

    @Test
    fun getAllBySession_WhenMultipleEntriesExist_ShouldReturnAllInCorrectOrder() = runBlocking {
        // Arrange
        val entry1 = sampleDataEntry(localSessionId = 1L, timestamp = 1000)
        val entry2 = sampleDataEntry(localSessionId = 1L, timestamp = 3000)
        val entry3 = sampleDataEntry(localSessionId = 1L, timestamp = 2000)

        dataEntryDao.upsert(entry1)
        dataEntryDao.upsert(entry2)
        dataEntryDao.upsert(entry3)

        // Act
        val result = dataEntryDao.getAllBySession(localSessionId = 1L).first()

        // Assert
        Assert.assertEquals(3, result.size)
        Assert.assertEquals(1000, result[0].timestamp)
        Assert.assertEquals(2000, result[1].timestamp)
        Assert.assertEquals(3000, result[2].timestamp)
    }

    @Test
    fun getAllBySession_WhenEntriesExistForDifferentSessions_ShouldNotReturnUnrelatedData() = runBlocking {
        // Arrange
        val entry1 = sampleDataEntry(localSessionId = 1L)
        val entry2 = sampleDataEntry(localSessionId = 2L) // Different session
        val entryId1 = dataEntryDao.upsert(entry1)
        dataEntryDao.upsert(entry2)

        // Act
        val result = dataEntryDao.getAllBySession(localSessionId = 1L).first()

        // Assert
        Assert.assertEquals(1, result.size)
        Assert.assertEquals(entryId1, result.first().localId)
    }

    @Test
    fun getAllBySession_WhenEntriesExistForMultipleSessions_ShouldReturnOnlyMatchingSessionEntries() = runBlocking {
        // Arrange
        val session1Entry1 = sampleDataEntry(localSessionId = 1L, timestamp = 1000)
        val session1Entry2 = sampleDataEntry(localSessionId = 1L, timestamp = 2000)
        val session2Entry1 = sampleDataEntry(localSessionId = 2L, timestamp = 3000)
        val session2Entry2 = sampleDataEntry(localSessionId = 2L, timestamp = 4000)

        dataEntryDao.upsert(session1Entry1)
        dataEntryDao.upsert(session1Entry2)
        dataEntryDao.upsert(session2Entry1)
        dataEntryDao.upsert(session2Entry2)

        // Act
        val session1Results = dataEntryDao.getAllBySession(localSessionId = 1L).first()
        val session2Results = dataEntryDao.getAllBySession(localSessionId = 2L).first()

        // Assert
        Assert.assertEquals(2, session1Results.size)
        Assert.assertEquals(2, session2Results.size)
        Assert.assertEquals(1000, session1Results[0].timestamp)
        Assert.assertEquals(2000, session1Results[1].timestamp)
        Assert.assertEquals(3000, session2Results[0].timestamp)
        Assert.assertEquals(4000, session2Results[1].timestamp)
    }

    @Test
    fun getAllBySession_WhenFilteringByRemoteSessionId_ShouldReturnMatchingEntries() = runBlocking {
        // Arrange
        val entry1 = sampleDataEntry(localSessionId = 1L, remoteSessionId = 100L)
        val entry2 = sampleDataEntry(localSessionId = 2L, remoteSessionId = 200L) // Different remote ID
        dataEntryDao.upsert(entry1)
        dataEntryDao.upsert(entry2)

        // Act
        val result = dataEntryDao.getAllBySession(remoteSessionId = 100L).first()

        // Assert
        Assert.assertEquals(1, result.size)
        Assert.assertEquals(entry1.userId, result.first().userId)
    }

    @Test
    fun getAllBySession_BothIdsProvided_ShouldReturnMatchingEntries() = runBlocking {
        // Arrange
        val entry1 = sampleDataEntry(localSessionId = 1L, remoteSessionId = 100L)
        val entry2 = sampleDataEntry(localSessionId = 1L, remoteSessionId = null) // Should still match
        val entry3 = sampleDataEntry(localSessionId = null, remoteSessionId = 100L) // Should still match
        val entry4 = sampleDataEntry(localSessionId = 2L, remoteSessionId = 200L) // Should NOT match

        val id = dataEntryDao.upsert(entry1)
        dataEntryDao.upsert(entry2)
        dataEntryDao.upsert(entry3)
        dataEntryDao.upsert(entry4)

        // Act
        val result = dataEntryDao.getAllBySession(localSessionId = 1L, remoteSessionId = 100L).first().map { it.localId }

        // Assert
        Assert.assertEquals(1, result.size)
        Assert.assertTrue(result.contains(id))
    }

    @Test
    fun getAllBySession_OnlyLocalSessionIdProvided_ShouldReturnMatchingEntries() = runBlocking {
        // Arrange
        val entry1 = sampleDataEntry(localSessionId = 1L, remoteSessionId = null)
        val entry2 = sampleDataEntry(localSessionId = 1L, remoteSessionId = 101L) // Should match
        val entry3 = sampleDataEntry(localSessionId = 2L, remoteSessionId = 100L) // Should NOT match

        val id1 = dataEntryDao.upsert(entry1)
        val id2 = dataEntryDao.upsert(entry2)
        dataEntryDao.upsert(entry3)

        // Act
        val resultIds = dataEntryDao.getAllBySession(localSessionId = 1L).first().map { it.localId }

        // Assert
        Assert.assertEquals(2, resultIds.size)
        Assert.assertTrue(resultIds.contains(id1))
        Assert.assertTrue(resultIds.contains(id2))
    }

    @Test
    fun getAllBySession_OnlyRemoteSessionIdProvided_ShouldReturnMatchingEntries() = runBlocking {
        // Arrange
        val entry1 = sampleDataEntry(localSessionId = null, remoteSessionId = 100L)
        val entry2 = sampleDataEntry(localSessionId = 1L, remoteSessionId = 100L) // Should match
        val entry3 = sampleDataEntry(localSessionId = 2L, remoteSessionId = 101L) // Should NOT match

        val id1 = dataEntryDao.upsert(entry1)
        val id2 = dataEntryDao.upsert(entry2)
        dataEntryDao.upsert(entry3)

        // Act
        val result = dataEntryDao.getAllBySession(remoteSessionId = 100L).first().map { it.localId }

        // Assert
        Assert.assertEquals(2, result.size)
        Assert.assertTrue(result.contains(id1))
        Assert.assertTrue(result.contains(id2))
    }

    @Test
    fun getAllBySession_NoIdsProvided_ShouldReturnEmptyList() = runBlocking {
        // Act
        val result = dataEntryDao.getAllBySession().first()

        // Assert
        Assert.assertTrue(result.isEmpty())
    }

    @Test
    fun getAllBySession_MismatchedIds_ShouldReturnEmptyList() = runBlocking {
        // Arrange
        val entry1 = sampleDataEntry(localSessionId = 1L, remoteSessionId = 100L)
        dataEntryDao.upsert(entry1)

        // Act
        val result = dataEntryDao.getAllBySession(localSessionId = 2L, remoteSessionId = 200L).first()

        // Assert
        Assert.assertTrue(result.isEmpty())
    }

    /*** getAllOngoing() Tests ***/

//    @Test
//    fun getAllOngoing_WhenNoEntriesExist_ShouldReturnEmptyList() = runBlocking {
//        // Act
//        val result = dataEntryDao.getAllOngoing().first()
//
//        // Assert
//        Assert.assertTrue(result.isEmpty())
//    }
//
//    @Test
//    fun getAllOngoing_WhenOneEntryExists_ShouldReturnCorrectResult() = runBlocking {
//        // Arrange
//        val entry = sampleDataEntry(ongoing = true)
//        dataEntryDao.upsert(entry)
//
//        // Act
//        val result = dataEntryDao.getAllOngoing().first()
//
//        // Assert
//        Assert.assertEquals(1, result.size)
//        Assert.assertTrue(result.first().ongoing)
//    }
//
//    @Test
//    fun getAllOngoing_WhenMultipleEntriesExist_ShouldReturnAllOngoingEntries() = runBlocking {
//        // Arrange
//        val entry1 = sampleDataEntry(ongoing = true)
//        val entry2 = sampleDataEntry(ongoing = true)
//        dataEntryDao.upsert(entry1)
//        dataEntryDao.upsert(entry2)
//
//        // Act
//        val result = dataEntryDao.getAllOngoing().first()
//
//        // Assert
//        Assert.assertEquals(2, result.size)
//        Assert.assertTrue(result.all { it.ongoing })
//    }
//
//    @Test
//    fun getAllOngoing_WhenSomeEntriesAreOngoing_ShouldReturnOnlyOngoingOnes() = runBlocking {
//        // Arrange
//        val ongoingEntry = sampleDataEntry(ongoing = true)
//        val nonOngoingEntry = sampleDataEntry(ongoing = false)
//        dataEntryDao.upsert(ongoingEntry)
//        dataEntryDao.upsert(nonOngoingEntry)
//
//        // Act
//        val result = dataEntryDao.getAllOngoing().first()
//
//        // Assert
//        Assert.assertEquals(1, result.size)
//        Assert.assertTrue(result.first().ongoing)
//    }
//
//    @Test
//    fun getAllOngoing_WhenAllEntriesAreNonOngoing_ShouldReturnEmptyList() = runBlocking {
//        // Arrange
//        val entry1 = sampleDataEntry(ongoing = false)
//        val entry2 = sampleDataEntry(ongoing = false)
//        dataEntryDao.upsert(entry1)
//        dataEntryDao.upsert(entry2)
//
//        // Act
//        val result = dataEntryDao.getAllOngoing().first()
//
//        // Assert
//        Assert.assertTrue(result.isEmpty())
//    }

    /*** getAllPendingUpload() Tests ***/

    @Test
    fun getAllPendingUpload_WhenNoEntriesExist_ShouldReturnEmptyList() = runBlocking {
        // Act
        val result = dataEntryDao.getAllPendingUpload().first()

        // Assert
        Assert.assertTrue(result.isEmpty())
    }

    @Test
    fun getAllPendingUpload_WhenOneEntryExists_ShouldReturnCorrectResult() = runBlocking {
        // Arrange
        val entry = sampleDataEntry(pendingUpload = true)
        dataEntryDao.upsert(entry)

        // Act
        val result = dataEntryDao.getAllPendingUpload().first()

        // Assert
        Assert.assertEquals(1, result.size)
        Assert.assertTrue(result.first().pendingUpload)
    }

    @Test
    fun getAllPendingUpload_WhenMultipleEntriesExist_ShouldReturnAllAllPendingUpload() = runBlocking {
        // Arrange
        val entry1 = sampleDataEntry(pendingUpload = true)
        val entry2 = sampleDataEntry(pendingUpload = true)
        dataEntryDao.upsert(entry1)
        dataEntryDao.upsert(entry2)

        // Act
        val result = dataEntryDao.getAllPendingUpload().first()

        // Assert
        Assert.assertEquals(2, result.size)
        Assert.assertTrue(result.all { it.pendingUpload })
    }

    @Test
    fun getAllPendingUpload_WhenSomeEntriesArePending_ShouldReturnOnlyAllPendingOnes() = runBlocking {
        // Arrange
        val pendingEntry = sampleDataEntry(pendingUpload = true)
        val nonPendingEntry = sampleDataEntry(pendingUpload = false)
        dataEntryDao.upsert(pendingEntry)
        dataEntryDao.upsert(nonPendingEntry)

        // Act
        val result = dataEntryDao.getAllPendingUpload().first()

        // Assert
        Assert.assertEquals(1, result.size)
        Assert.assertTrue(result.first().pendingUpload)
    }

    @Test
    fun getAllPendingUpload_WhenAllEntriesAreNonAllPending_ShouldReturnEmptyList() = runBlocking {
        // Arrange
        val entry1 = sampleDataEntry(pendingUpload = false)
        val entry2 = sampleDataEntry(pendingUpload = false)
        dataEntryDao.upsert(entry1)
        dataEntryDao.upsert(entry2)

        // Act
        val result = dataEntryDao.getAllPendingUpload().first()

        // Assert
        Assert.assertTrue(result.isEmpty())
    }

    /*** getLatestBySession() ***/
    @Test
    fun getLatestBySession_WhenNoEntriesExist_ShouldReturnNull() = runBlocking {
        // Act
        val result = dataEntryDao.getLatestBySession(localSessionId = 1L).first()

        // Assert
        Assert.assertNull(result)
    }

    @Test
    fun getLatestBySession_WhenOneEntryExists_ShouldReturnThatEntry() = runBlocking {
        // Arrange
        val entry = sampleDataEntry(localSessionId = 1L, timestamp = 1000)
        val entryId = dataEntryDao.upsert(entry)

        // Act
        val result = dataEntryDao.getLatestBySession(localSessionId = 1L).first()

        // Assert
        Assert.assertNotNull(result)
        Assert.assertEquals(entryId, result?.localId)
    }

    @Test
    fun getLatestBySession_WhenMultipleEntriesExist_ShouldReturnLatestEntry() = runBlocking {
        // Arrange
        val oldEntry = sampleDataEntry(localSessionId = 1L, timestamp = 1000)
        val newEntry = sampleDataEntry(localSessionId = 1L, timestamp = 3000) // Latest timestamp

        dataEntryDao.upsert(oldEntry)
        val latestId = dataEntryDao.upsert(newEntry) // Should be returned

        // Act
        val result = dataEntryDao.getLatestBySession(localSessionId = 1L).first()

        // Assert
        Assert.assertNotNull(result)
        Assert.assertEquals(latestId, result?.localId)
    }

    @Test
    fun getLatestBySession_WhenEntriesExistForDifferentSessions_ShouldReturnOnlyMatchingSessionEntry() = runBlocking {
        // Arrange
        val session1Entry = sampleDataEntry(localSessionId = 1L, timestamp = 2000)
        val session2Entry = sampleDataEntry(localSessionId = 2L, timestamp = 3000) // Different session

        val session1Id = dataEntryDao.upsert(session1Entry)
        dataEntryDao.upsert(session2Entry)

        // Act
        val result = dataEntryDao.getLatestBySession(localSessionId = 1L).first()

        // Assert
        Assert.assertNotNull(result)
        Assert.assertEquals(session1Id, result?.localId)
    }

    @Test
    fun getLatestBySession_WhenEntriesExistForRemoteSession_ShouldReturnLatestRemoteSessionEntry() = runBlocking {
        // Arrange
        val entry1 = sampleDataEntry(localSessionId = 1L, remoteSessionId = 100L, timestamp = 1000)
        val entry2 = sampleDataEntry(localSessionId = 1L, remoteSessionId = 100L, timestamp = 2000) // Latest

        dataEntryDao.upsert(entry1)
        val latestId = dataEntryDao.upsert(entry2) // Should be returned

        // Act
        val result = dataEntryDao.getLatestBySession(remoteSessionId = 100L).first()

        // Assert
        Assert.assertNotNull(result)
        Assert.assertEquals(latestId, result?.localId)
    }

    /*** Helper Function ***/

    private fun sampleDataEntry(
        localId: Long = 0,
        serverId: Long? = null,
        userId: String = "user123",
        localSessionId: Long? = 1L,
        remoteSessionId: Long? = 1L,
        timestamp: Long = System.currentTimeMillis(),
        latitude: Double = 40.7128,
        longitude: Double = -74.0060,
        coLevel: Float? = 0.5f,
        pm25level: Float? = 12.3f,
        temperature: Float? = 22.0f,
        humidity: Float? = 60.0f,
//        ongoing: Boolean = true,
        pendingUpload: Boolean = true
    ) = DataEntryEntity(
        localId, serverId, userId, localSessionId, remoteSessionId, timestamp, latitude, longitude,
        coLevel, pm25level, temperature, humidity, pendingUpload
    )
}