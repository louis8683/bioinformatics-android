package com.louislu.pennbioinformatics.screen.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.louislu.pennbioinformatics.auth.AuthRepository
import com.louislu.pennbioinformatics.domain.model.UserInfo
import com.louislu.pennbioinformatics.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import net.openid.appauth.AuthorizationException
import timber.log.Timber
import javax.inject.Inject


@HiltViewModel
class MenuViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _userInfo = MutableStateFlow<UserInfo?>(null)
    val userInfo: StateFlow<UserInfo?> = _userInfo

    private val _newSessionCreated = MutableSharedFlow<Long>()
    val newSessionCreated: SharedFlow<Long> = _newSessionCreated.asSharedFlow()

    init {
        viewModelScope.launch {
            try {
                val user = authRepository.getUserInfo()
                _userInfo.value = user
            } catch (e: Exception) {
                Timber.i("Exception getting user info: $e")
            }
        }
    }

    fun createNewSession(
        title: String,
        description: String?,
        deviceName: String? = null
    ) {
        viewModelScope.launch {
            val user = userInfo.value
                ?: throw IllegalStateException("User info not loaded")

            val className = user.className
                ?: throw IllegalStateException("User info is missing required field: className")

            val schoolName = user.schoolName
                ?: throw IllegalStateException("User info is missing required field: schoolName")

            val sessionId = sessionRepository.createSession(
                userId = user.userId,
                groupName = user.groupName,
                className = className,
                schoolName = schoolName,
                deviceName = deviceName,
                title = title,
                description = description
            )

            _newSessionCreated.emit(sessionId)
        }
    }
}