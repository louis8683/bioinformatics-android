package com.louislu.pennbioinformatics.screen.history

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
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
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class EntryHistoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dataEntryRepository: DataEntryRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val id: Long? = savedStateHandle["id"]
    private val isServerId: Boolean? = savedStateHandle["isServerId"]

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

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

    fun exportCsvCompat(context: Context, filename: String = "session.csv") {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val csv = generateCsv(_entries.value)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    saveCsvWithMediaStore(context, filename, csv)
                } else {
                    saveCsvLegacy(context, filename, csv)
                }
            }
        }
    }

    private fun generateCsv(entries: List<DataEntry>): String {
        val builder = StringBuilder()
        builder.append(
            listOf(
                "Time",
                "Latitude",
                "Longitude",
                "CO (ppm)",
                "PM2.5 (µg/m³)",
                "Temperature (°C)",
                "Humidity (%)",
                "User ID",
                "Remote Session ID",
            ).joinToString(",")
        ).append("\n")

        entries.forEach { entry ->
            builder.append(
                listOf(
                    formatEpochMillisToDateTime(entry.timestamp),
                    entry.latitude ?: "",
                    entry.longitude ?: "",
                    entry.coLevel ?: "",
                    entry.pm25level ?: "",
                    entry.temperature ?: "",
                    entry.humidity ?: "",
                    entry.userId,
                    entry.remoteSessionId ?: ""
                ).joinToString(",")
            ).append("\n")
        }

        return builder.toString()
    }

    private fun formatEpochMillisToDateTime(epochMillis: Long): String {
        val instant = Instant.ofEpochMilli(epochMillis)
        val zonedDateTime = instant.atZone(ZoneId.systemDefault())
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        return formatter.format(zonedDateTime)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveCsvWithMediaStore(context: Context, filename: String, csvContent: String) {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, filename)
            put(MediaStore.Downloads.MIME_TYPE, "text/csv")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val item = resolver.insert(collection, contentValues)

        item?.let { uri ->
            resolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(csvContent.toByteArray())
            }
            contentValues.clear()
            contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
        }
    }

    @Suppress("DEPRECATION")
    private fun saveCsvLegacy(context: Context, filename: String, csv: String) {
        _isSaving.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()

            val file = File(downloadsDir, filename)
            try {
                FileOutputStream(file).use { output ->
                    output.write(csv.toByteArray())
                }
                Timber.i("Saved to downloads (legacy)")
            } catch (e: IOException) {
                e.printStackTrace()
                Timber.e("Error saving file (legacy)")
            } finally {
                _isSaving.value = false
            }
        }
    }
}
