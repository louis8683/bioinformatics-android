package com.louislu.pennbioinformatics.screen.monitor

import androidx.lifecycle.SavedStateHandle
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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class DataMonitorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val bleRepository: BleRepository,
    private val sessionRepository: SessionRepository,
    private val dataEntryRepository: DataEntryRepository,
    private val locationRepository: LocationRepository
): ViewModel() {

    // TODO: the data collection is not paused when left the data monitor screen

    companion object {
        private const val DATA_COLLECTION_INTERVAL_MS = 5000L
    }

    private val sessionId: Long = checkNotNull(savedStateHandle["sessionId"])

    private val _currentSession = sessionRepository
        .getById(sessionId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )
    val currentSession: StateFlow<Session?> = _currentSession

    private val _latestDataEntry = dataEntryRepository.getLatestBySession(sessionId).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )
    val latestDataEntry: StateFlow<DataEntry?> = _latestDataEntry

//    private val bioInfoEntry: StateFlow<BioinfoEntry?> = bleRepository.getBioinfoData()
//        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    private var bioInfoEntry: BioinfoEntry? = null


    private val _navigateToSessionEnded = MutableSharedFlow<Unit>()
    val navigateToSessionEnded: SharedFlow<Unit> = _navigateToSessionEnded

    init {
        startNewSession()

        viewModelScope.launch {
            bleRepository.getBioinfoData().collect {
                Timber.d("Bioinfo Data Received: $it")
//                Timber.d("bioInfoEntry value: ${bioInfoEntry.value}")
                bioInfoEntry = it
            }
        }
    }

    fun updateDescription(content: String) {
        viewModelScope.launch {
            _currentSession.value?.let {
                sessionRepository.upsert(session = it.copy(description = content)) }
        }
    }


    fun syncPendingAndNavigate() {
        viewModelScope.launch {
            try {
                sessionRepository.syncPendingUploads()
                dataEntryRepository.syncPendingUploads()
                _navigateToSessionEnded.emit(Unit)

                // TODO: it's better to sync a session -> its data entries, and repeat
            } catch (e: Exception) {
                Timber.e(e, "Sync failed before navigating")
                // Optionally show error message
            }
        }
    }

    private fun startNewSession() {
        viewModelScope.launch {
            startDataMonitoring()
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
        val userId = authRepository.getUserInfo()?.userId ?: throw IllegalStateException() // TODO: make sure this doesn't happen

        Timber.i("location: $location")
        Timber.i("bioInfoEntry: $bioInfoEntry")

        if (location == null) {
            Timber.e("Location is null")
        }

        currentSession.value?.let { session ->
            val dataEntry = DataEntry(
                localId = null,
                serverId = null,
                userId = userId,
                localSessionId = session.localId,
                remoteSessionId = session.serverId,
                timestamp = timestamp,
                latitude = location?.latitude,
                longitude = location?.longitude,
                coLevel = bioInfoEntry?.coConcentration,
                pm25level = bioInfoEntry?.pm25,
                temperature = bioInfoEntry?.temperature,
                humidity = bioInfoEntry?.humidity,
                pendingUpload = true
            )

            dataEntryRepository.upsert(dataEntry)
        } ?: Timber.e("Session is null")
    }

}