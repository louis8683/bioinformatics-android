package com.louislu.pennbioinformatics.data.session.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
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
class SessionDaoTest {

    private lateinit var sessionDao: SessionDao
    private lateinit var database: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context, AppDatabase::class.java
        ).build()
        sessionDao = database.sessionDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }

    /*** TEST CASES ***/

    /*** 🔹 getAll() Tests 🔹 ***/

    @Test
    fun getAll_WhenNoSessionsExist_ShouldReturnEmptyList() = runBlocking {
        // Act
        val result = sessionDao.getAll().first()

        // Assert
        Assert.assertTrue(result.isEmpty())
    }

    @Test
    fun getAll_WhenOneSessionExists_ShouldReturnCorrectResult() = runBlocking {
        // Arrange
        val session = sampleSession()
        val sessionId = sessionDao.upsert(session) // Get actual Room-assigned ID

        // Act
        val result = sessionDao.getAll().first()

        // Assert
        Assert.assertEquals(1, result.size)
        Assert.assertEquals(sessionId, result.first().localId) // Compare with assigned ID
    }

    @Test
    fun getAll_WhenMultipleSessionsExist_ShouldReturnAllInDescendingOrder() = runBlocking {
        // Arrange
        val session1 = sampleSession(startTimestamp = 1700000000000L)
        val session2 = sampleSession(startTimestamp = 1800000000000L) // Newer session
        val session3 = sampleSession(startTimestamp = 1750000000000L) // Newer session

        val sessionId1 = sessionDao.upsert(session1)
        val sessionId2 = sessionDao.upsert(session2)
        val sessionId3 = sessionDao.upsert(session3)

        // Act
        val result = sessionDao.getAll().first()

        // Assert
        Assert.assertEquals(3, result.size)
        Assert.assertEquals(sessionId2, result[0].localId) // Most recent session first
        Assert.assertEquals(sessionId3, result[1].localId)
        Assert.assertEquals(sessionId1, result[2].localId)
    }

    /*** getById() Tests ***/

    @Test
    fun getById_WhenNoSessionExists_ShouldReturnNull() = runBlocking {
        // Act
        val result = sessionDao.getById(1L).first()

        // Assert
        Assert.assertNull(result)
    }

    @Test
    fun getById_WhenSessionExists_ShouldReturnCorrectSession() = runBlocking {
        // Arrange
        val session = sampleSession()
        val sessionId = sessionDao.upsert(session) // Get actual assigned ID

        // Act
        val result = sessionDao.getById(sessionId).first()

        // Assert
        Assert.assertNotNull(result)
        Assert.assertEquals(sessionId, result?.localId) // Ensure correct ID
    }

    @Test
    fun getById_WhenMultipleSessionsExist_ShouldReturnCorrectSession() = runBlocking {
        // Arrange
        val session1 = sampleSession(userId = "userA")
        val session2 = sampleSession(userId = "userB")

        val sessionId1 = sessionDao.upsert(session1)
        val sessionId2 = sessionDao.upsert(session2)

        // Act
        val result1 = sessionDao.getById(sessionId1).first()
        val result2 = sessionDao.getById(sessionId2).first()

        // Assert
        Assert.assertEquals(sessionId1, result1?.localId)
        Assert.assertEquals("userA", result1?.userId)

        Assert.assertEquals(sessionId2, result2?.localId)
        Assert.assertEquals("userB", result2?.userId)
    }

    @Test
    fun getById_WhenQueryingNonExistentId_ShouldReturnNull() = runBlocking {
        // Arrange
        val session = sampleSession()
        sessionDao.upsert(session) // Insert a session, but we will query a non-existent ID

        // Act
        val result = sessionDao.getById(999L).first() // Querying a non-existent session ID

        // Assert
        Assert.assertNull(result)
    }

    /*** getAllPendingUpload() Tests ***/

    @Test
    fun getAllPendingUpload_WhenNoSessionsExist_ShouldReturnEmptyList() = runBlocking {
        // Act
        val result = sessionDao.getAllPendingUpload().first()

        // Assert
        Assert.assertTrue(result.isEmpty())
    }

    @Test
    fun getAllPendingUpload_WhenOneSessionIsPending_ShouldReturnCorrectResult() = runBlocking {
        // Arrange
        val session = sampleSession(pendingUpload = true)
        val sessionId = sessionDao.upsert(session)

        // Act
        val result = sessionDao.getAllPendingUpload().first()

        // Assert
        Assert.assertEquals(1, result.size)
        Assert.assertEquals(sessionId, result.first().localId)
        Assert.assertTrue(result.first().pendingUpload)
    }

    @Test
    fun getAllPendingUpload_WhenMultipleSessionsArePending_ShouldReturnAllPendingSessions() = runBlocking {
        // Arrange
        val session1 = sampleSession(pendingUpload = true)
        val session2 = sampleSession(pendingUpload = true)

        val sessionId1 = sessionDao.upsert(session1)
        val sessionId2 = sessionDao.upsert(session2)

        // Act
        val result = sessionDao.getAllPendingUpload().first()

        // Assert
        Assert.assertEquals(2, result.size)
        Assert.assertTrue(result.all { it.pendingUpload })
        Assert.assertEquals(setOf(sessionId1, sessionId2), result.map { it.localId }.toSet())
    }

    @Test
    fun getAllPendingUpload_WhenSomeSessionsArePending_ShouldReturnOnlyPendingOnes() = runBlocking {
        // Arrange
        val pendingSession = sampleSession(pendingUpload = true)
        val nonPendingSession = sampleSession(pendingUpload = false)

        val pendingId = sessionDao.upsert(pendingSession)
        sessionDao.upsert(nonPendingSession)

        // Act
        val result = sessionDao.getAllPendingUpload().first()

        // Assert
        Assert.assertEquals(1, result.size)
        Assert.assertEquals(pendingId, result.first().localId)
    }

    @Test
    fun getAllPendingUpload_WhenAllSessionsAreNonPending_ShouldReturnEmptyList() = runBlocking {
        // Arrange
        val session1 = sampleSession(pendingUpload = false)
        val session2 = sampleSession(pendingUpload = false)

        sessionDao.upsert(session1)
        sessionDao.upsert(session2)

        // Act
        val result = sessionDao.getAllPendingUpload().first()

        // Assert
        Assert.assertTrue(result.isEmpty())
    }

    @Test(timeout = 1000)
    fun getAllPendingUpload_WhenSessionIsUpdatedFromPendingToNonPending_ShouldNoLongerBeRetrieved() = runBlocking {
        // Arrange
        val session = sampleSession(pendingUpload = true)

        // Act
        val sessionId = sessionDao.upsert(session)

        sessionDao.getAllPendingUpload().test {
            // Assert
            Assert.assertTrue(awaitItem().all { it.pendingUpload })

            // Act
            val updatedSession = session.copy(localId = sessionId, pendingUpload = false)
            sessionDao.upsert(updatedSession)

            // Assert
            Assert.assertTrue(awaitItem().isEmpty()) // The last observed value should be an empty list

            expectNoEvents()

            cancelAndIgnoreRemainingEvents()
        }
    }

    /*** upsert() ***/

    @Test
    fun upsert_WhenSessionDoesNotExist_ShouldInsertNewSession() = runBlocking {
        val session = sampleSession()
        val sessionId = sessionDao.upsert(session)

        val result = sessionDao.getById(sessionId).first()
        Assert.assertNotNull(result)
        Assert.assertEquals(sessionId, result?.localId)
    }

    @Test
    fun upsert_WhenSessionExists_ShouldUpdateExistingSession() = runBlocking {
        val session = sampleSession()
        val sessionId = sessionDao.upsert(session)

        val updatedSession = session.copy(localId = sessionId, description = "Updated Description")
        sessionDao.upsert(updatedSession)

        val result = sessionDao.getById(sessionId).first()
        Assert.assertEquals("Updated Description", result?.description)
    }

    /*** delete() ***/

    @Test
    fun delete_WhenSessionExists_ShouldRemoveIt() = runBlocking {
        val session = sampleSession()
        val sessionId = sessionDao.upsert(session)

        val insertedSession = sessionDao.getById(sessionId).first()
        Assert.assertNotNull(insertedSession) // Ensure the session was inserted

        sessionDao.delete(insertedSession!!) // Delete it

        val result = sessionDao.getById(sessionId).first()
        Assert.assertNull(result) // Session should no longer exist
    }

    /*** deleteAll() ***/

    @Test
    fun deleteAll_WhenSessionsExist_ShouldRemoveAllSessions() = runBlocking {
        val session1 = sampleSession(userId = "UserA")
        val session2 = sampleSession(userId = "UserB")

        sessionDao.upsert(session1)
        sessionDao.upsert(session2)

        val allSessions = sessionDao.getAll().first()
        Assert.assertEquals(2, allSessions.size) // Ensure both sessions were inserted

        sessionDao.deleteAll() // Remove all

        val result = sessionDao.getAll().first()
        Assert.assertTrue(result.isEmpty()) // Database should now be empty
    }

    /*** Helper Function: Sample Session ***/
    private fun sampleSession(
        localId: Long = 0,
        serverId: Long? = null,
        userId: String = "user123",
        groupId: String? = "groupA",
        deviceMac: String = "00:1A:7D:DA:71:13",
        startTimestamp: Long = 1740729327L,
        endTimestamp: Long? = null,
        description: String? = "Test Session",
        pendingUpload: Boolean = true
    ) = SessionEntity(
        localId, serverId, userId, groupId, deviceMac,
        startTimestamp, endTimestamp, description, pendingUpload
    )
}