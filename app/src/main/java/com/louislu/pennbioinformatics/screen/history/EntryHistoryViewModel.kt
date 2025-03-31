package com.louislu.pennbioinformatics.screen.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.louislu.pennbioinformatics.domain.model.DataEntry
import com.louislu.pennbioinformatics.domain.model.Session
import com.louislu.pennbioinformatics.domain.repository.DataEntryRepository
import com.louislu.pennbioinformatics.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class EntryHistoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dataEntryRepository: DataEntryRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val id: Long? = savedStateHandle["id"]
    private val isServerId: Boolean? = savedStateHandle["isServerId"]

    // TODO: deal with the both null (which shouldn't happen)

    private val _entries = MutableStateFlow<List<DataEntry>>(emptyList())
    val entries: StateFlow<List<DataEntry>> = _entries

    private val _session: StateFlow<Session?> = run {
        if (isServerId != null && id != null) {
            val flow = if (isServerId) sessionRepository.getById(serverId = id)
            else sessionRepository.getById(localId = id)

            flow.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                initialValue = null
            )
        } else {
            MutableStateFlow<Session?>(null)
        }
    }

    val session: StateFlow<Session?> = _session

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                dataEntryRepository.syncPendingUploads()

                session.collect {
                    session.value?.let {
                        dataEntryRepository.getAllBySession(localSessionId = it.localId) // defaults to all entries
                            .collect { _entries.value = it }
                    }
                }
            }
        }
        viewModelScope.launch {
            _entries.first{ it.isNotEmpty() } // wait for entry to arrive
            _isLoading.value = false
        }
    }
}
