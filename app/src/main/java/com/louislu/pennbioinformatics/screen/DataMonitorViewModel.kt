package com.louislu.pennbioinformatics.screen

import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.louislu.pennbioinformatics.auth.AuthRepository
import com.louislu.pennbioinformatics.ble.BleRepository
import com.louislu.pennbioinformatics.ble.model.BioinfoEntry
import com.louislu.pennbioinformatics.domain.model.DataEntry
import com.louislu.pennbioinformatics.domain.model.Session
import com.louislu.pennbioinformatics.domain.repository.DataEntryRepository
import com.louislu.pennbioinformatics.domain.repository.LocationRepository
import com.louislu.pennbioinformatics.domain.repository.SessionRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

class DataMonitorViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val bleRepository: BleRepository,
    private val sessionRepository: SessionRepository,
    private val dataEntryRepository: DataEntryRepository,
    private val locationRepository: LocationRepository
): ViewModel() {

    companion object {
        private const val DATA_COLLECTION_INTERVAL_MS = 5000L
    }

    private val _currentSession = MutableStateFlow<Session?>(null)
    val currentSession: StateFlow<Session?> = _currentSession

    private val bioInfoEntry: StateFlow<BioinfoEntry?> = bleRepository.getBioinfoData()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    private var sessionId: Long? = null // Store session ID to associate data

    init {

    }

    private fun startNewSession() {
        viewModelScope.launch {
//            val userId = authRepository.getUserId() ?: return@launch
            // TODO: get user ID
            val userId = "test-id"
            val groupId = "test-group-id"

            val newSession = Session(
                localId = null, // Will be auto-generated
                serverId = null,
                userId = userId,
                groupId = groupId,
                deviceMac = "not-implemented", // Retrieve dynamically if needed
                startTimestamp = System.currentTimeMillis(),
                endTimestamp = null,
                description = "",
                pendingUpload = true
            )

            sessionId = sessionRepository.upsert(newSession) // Insert session & get ID
            _currentSession.value = sessionRepository.getById(sessionId!!).firstOrNull()

            startDataMonitoring() // Start collecting BLE & location data every 5s
        }
    }

    private fun startDataMonitoring() {
        viewModelScope.launch {
            while (true) {
                collectAndStoreData()
                delay(DATA_COLLECTION_INTERVAL_MS)
            }
        }
    }

    private suspend fun collectAndStoreData() {
        val location = locationRepository.getLastLocation()
        val timestamp = System.currentTimeMillis()

        // TODO: implement this
        val userId = "test-id"

        val dataEntry = DataEntry(
            localId = null,
            serverId = null,
            userId = userId ?: return,
            localSessionId = sessionId,
            remoteSessionId = null,
            timestamp = timestamp,
            latitude = location?.latitude,
            longitude = location?.longitude,
            coLevel = bioInfoEntry.value?.coConcentration,
            pm25level = bioInfoEntry.value?.pm25,
            temperature = bioInfoEntry.value?.temperature,
            humidity = bioInfoEntry.value?.humidity,
//            ongoing = true,
            pendingUpload = true
        )

        dataEntryRepository.upsert(dataEntry) // Store in Room
//        _latestDataEntry.value = dataEntryRepository.getOngoing().firstOrNull() // ✅ Get latest entry
    }

}