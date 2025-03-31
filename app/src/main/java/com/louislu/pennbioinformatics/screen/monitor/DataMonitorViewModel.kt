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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.roundToInt

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

    private var bioInfoEntry: BioinfoEntry? = null

    private val _navigateToSessionEnded = MutableSharedFlow<Unit>()
    val navigateToSessionEnded: SharedFlow<Unit> = _navigateToSessionEnded

    private var isMonitoring = true

    private val _isUpdating = MutableStateFlow(false)
    val isUpdating: StateFlow<Boolean> = _isUpdating

    init {
        viewModelScope.launch {
            bleRepository.getBioinfoData().collect {
                Timber.d("Bioinfo Data Received: $it")
                bioInfoEntry = it
            }
        }
    }

    fun updateSession(title: String? = null, description: String? = null) {
        if (title == null && description == null) return
        viewModelScope.launch {
            _isUpdating.value = true
            _currentSession.value?.let {
                if (title != null && description != null) {
                    sessionRepository.upsert(session = it.copy(title = title, description = description))
                }
                else if (title != null) {
                    sessionRepository.upsert(session = it.copy(title = title))
                }
                else if (description != null) {
                    sessionRepository.upsert(session = it.copy(description = description))
                }
            }
            _isUpdating.value = false
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

    fun stopSession() {
        isMonitoring = false
    }

    fun startSession() {
        isMonitoring = true
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                while (isMonitoring) {
                    collectAndStoreData()
                    delay(DATA_COLLECTION_INTERVAL_MS)
                }
            }
        }
    }

    private suspend fun collectAndStoreData() {
        val location = locationRepository.getLastLocation()
        val timestamp = System.currentTimeMillis()
        val userId = authRepository.getUserInfo()?.userId ?: "" // TODO: make sure this doesn't happen

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
                coLevel = bioInfoEntry?.let {
                    if (it.coConcentration == Float.NEGATIVE_INFINITY) null
                    else it.coConcentration
                },
                pm25level = bioInfoEntry?.let{
                    if (it.pm25.roundToInt() == -1) null
                    else it.pm25
                },
                temperature = bioInfoEntry?.let{
                    if (it.temperature == Float.NEGATIVE_INFINITY) null
                    else it.temperature
                },
                humidity = bioInfoEntry?.let{
                    if (it.humidity == Float.NEGATIVE_INFINITY) null
                    else it.humidity
                },
                pendingUpload = true
            )

            dataEntryRepository.upsert(dataEntry)
        } ?: Timber.e("Session is null")
    }

}