package com.louislu.pennbioinformatics.screen.history

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.louislu.pennbioinformatics.domain.model.Session
import com.louislu.pennbioinformatics.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class SessionHistoryViewModel @Inject constructor(
    private val sessionRepository: SessionRepository
): ViewModel() {

    private val _sessions = MutableStateFlow<List<Session>>(emptyList())
    val sessions: StateFlow<List<Session>> = _sessions

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        viewModelScope.launch {
            syncAndLoadSessions()
        }
    }

    private suspend fun syncAndLoadSessions() {
        try {
            _isLoading.value = true
            sessionRepository.syncPendingUploads()
            sessionRepository.getAll().collect {
                _sessions.value = it
                _isLoading.value = false
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to sync/load")
            _isLoading.value = false
        }
    }
}