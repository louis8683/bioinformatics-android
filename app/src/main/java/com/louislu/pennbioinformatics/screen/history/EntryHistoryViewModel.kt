package com.louislu.pennbioinformatics.screen.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.louislu.pennbioinformatics.domain.model.DataEntry
import com.louislu.pennbioinformatics.domain.model.Session
import com.louislu.pennbioinformatics.domain.repository.DataEntryRepository
import com.louislu.pennbioinformatics.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class EntryHistoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dataEntryRepository: DataEntryRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val sessionServerId: Long = checkNotNull(savedStateHandle["sessionServerId"])

    private val _entries = MutableStateFlow<List<DataEntry>>(emptyList())
    val entries: StateFlow<List<DataEntry>> = _entries

    private val _session = sessionRepository.getById(serverId = sessionServerId)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    val session: StateFlow<Session?> = _session

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading


    init {
        viewModelScope.launch {
            syncAndLoadEntries()
        }
    }

    private suspend fun syncAndLoadEntries() {
        _isLoading.value = true
        try {
            // Step 1: Upload pending entries/sessions
            dataEntryRepository.syncPendingUploads()

            // Step 2: Pull all entries (you can filter by session ID if needed)
            viewModelScope.launch {
                dataEntryRepository.getAllBySession(remoteSessionId = sessionServerId) // defaults to all entries
                    .collect { _entries.value = it }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to sync or load entries")
            // Optionally expose error state
        } finally {
            _isLoading.value = false
        }
    }
}
