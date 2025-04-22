package com.louislu.pennbioinformatics.data.session

import com.louislu.pennbioinformatics.auth.AuthRepository
import com.louislu.pennbioinformatics.data.session.local.SessionDao
import com.louislu.pennbioinformatics.data.session.local.toEntity
import com.louislu.pennbioinformatics.data.session.remote.CreateSessionRequest
import com.louislu.pennbioinformatics.data.session.remote.SessionApiService
import com.louislu.pennbioinformatics.data.session.remote.UpdateSessionRequest
import com.louislu.pennbioinformatics.domain.model.Session
import com.louislu.pennbioinformatics.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject

class SessionRepositoryImpl @Inject constructor(
    private val sessionDao: SessionDao,
    private val sessionApiService: SessionApiService,
    private val authRepository: AuthRepository
) : SessionRepository {

    /*** Retrieve All Sessions (Ordered by Start Timestamp) ***/
    override fun getAll(): Flow<List<Session>> {
        return sessionDao.getAll()
            .map { entities -> entities.map { it.toDomainModel() } }
    }

    /*** Retrieve a Single Session by ID ***/
    override fun getById(localId: Long?, serverId: Long?): Flow<Session?> {
        if ((localId == null && serverId == null) || (localId != null && serverId != null)) {
            throw IllegalArgumentException("Exactly one of localId or serverId must be provided")
        }

        return when {
            localId != null -> sessionDao.getByLocalId(localId)
            serverId != null -> sessionDao.getByServerId(serverId)
            else -> throw IllegalStateException("Unreachable")
        }.map { it?.toDomainModel() }
    }

    /*** Insert or Update a Session ***/
    override suspend fun upsert(session: Session): Long {
        // NOTE: current flow -- set pending to true, instead of trying to update directly
        //       In the future, we can try to upload directly.
        Timber.i("Updating session: $session")
        val result = sessionDao.upsert(
            session.copy(pendingUpload = true).toEntity())
        Timber.i("Result: $result")
        Timber.i("Updated session: ${sessionDao.getByLocalId(localId = result).firstOrNull()}")
        return result
    }

    override suspend fun getPendingUploadCount(): Int {
        return sessionDao.getPendingUploadCount()
    }

    /*** syncPendingUploads() ***/
    override suspend fun syncPendingUploads() {
        val pendingSessions = sessionDao.getAllPendingUpload()

        for (entity in pendingSessions) {
            // TODO: differentiate between those that are already in the cloud DB, and those that are new
            try {
                val session = entity.toDomainModel()

                val tokenResult = authRepository.getAccessToken()
                if (tokenResult.isFailure) {
                    Timber.w("Sync: failed to get token")
                    return
                }

                val token = tokenResult.getOrThrow()

                if (session.serverId == null) {
                    val request = CreateSessionRequest(
                        userId = session.userId,
                        groupName = session.groupName,
                        className = session.className,
                        schoolName = session.schoolName,
                        deviceName = session.deviceName,
                        startTimestamp = session.startTimestamp,
                        title = session.title,
                        description = session.description
                    )

                    val serverId = sessionApiService.createSession("Bearer $token", request).sessionId

                    Timber.i("Sync: server ID generated = $serverId")
                    val sessionFromServer = sessionApiService.getSessionById("Bearer $token", serverId)
                        .toDomainModel()

                    Timber.i("Sync: refetched session = $sessionFromServer")

                    // Save back to Room with updated serverId and pendingUpload = false
                    sessionDao.upsert(
                        sessionFromServer.copy(
                            localId = session.localId, // preserve localId
                            pendingUpload = false
                        ).toEntity()
                    )
                }
                else {
                    val updateRequest = UpdateSessionRequest(
                        description = session.description,
                        device_name = session.deviceName,
                        group_name = session.groupName,
                        class_name = session.className,
                        school_name = session.schoolName,
                        title = session.title,
                        end_timestamp = session.endTimestamp
                    )

                    sessionApiService.updateSession("Bearer $token", session.serverId, updateRequest)
                    Timber.i("Sync: updated session with serverId = ${session.serverId}")

                    // Mark local entity as synced
                    sessionDao.upsert(
                        session.copy(pendingUpload = false).toEntity()
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to sync session with localId=${entity.localId}, exception: $e")
                // Skip and move to the next one
            }
        }
    }

    override suspend fun createSession(
        userId: String,
        groupName: String?,
        className: String,
        schoolName: String,
        deviceName: String?,
        title: String,
        description: String?
    ): Long {
        val now = System.currentTimeMillis() // epoch millis

        val request = CreateSessionRequest(
            userId = userId,
            groupName = groupName,
            className = className,
            schoolName = schoolName,
            deviceName = deviceName,
            startTimestamp = now,
            title = title,
            description = description
        )

        return try {
            val accessToken = authRepository.getAccessToken().getOrThrow()
            val serverId = sessionApiService.createSession("Bearer $accessToken", request).sessionId
            Timber.i("Server Session ID: $serverId")
            val session = sessionApiService.getSessionById("Bearer $accessToken", serverId)
                .toDomainModel()
            Timber.i("Server Session: $session")
            sessionDao.upsert(session.toEntity())
        } catch (e: Exception) {
            // Log the exception or handle gracefully
            Timber.e("Failed to create a remote session, creating a local session: $e, ${e.stackTrace}")
            val localFallbackSession = Session(
                localId = null,
                serverId = null, // not uploaded
                userId = userId,
                groupName = groupName,
                className = className,
                schoolName = schoolName,
                deviceName = deviceName,
                startTimestamp = now,
                endTimestamp = null,
                title = title,
                description = description,
                pendingUpload = true // flag for future sync
            )
            sessionDao.upsert(localFallbackSession.toEntity())
        }
    }
}