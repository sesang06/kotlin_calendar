package com.kotlincalendar.client.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlincalendar.client.domain.model.User
import com.kotlincalendar.client.domain.repository.AuthRepository
import com.kotlincalendar.client.presentation.event.detail.formatKoreanDate
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** 저장된 사용자 정보를 먼저 보여주고, 서버에서 최신 정보를 받아 갱신한다. 로그아웃 후 화면 이동은 App이 authState로 처리한다. */
class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : ViewModel() {
    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    private val _effects = Channel<ProfileEffect>(Channel.BUFFERED)
    val effects: Flow<ProfileEffect> = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            authRepository.getSavedUser()?.let(::showUser)
            refresh()
        }
    }

    fun onAction(action: ProfileAction) {
        when (action) {
            ProfileAction.Back -> viewModelScope.launch { _effects.send(ProfileEffect.NavigateBack) }
            ProfileAction.Retry -> viewModelScope.launch { refresh() }
            ProfileAction.Logout -> logout()
        }
    }

    private suspend fun refresh() {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        authRepository
            .refreshCurrentUser()
            .onSuccess { user ->
                showUser(user)
                _state.update { it.copy(isLoading = false) }
            }.onFailure { error ->
                _state.update {
                    it.copy(isLoading = false, errorMessage = error.message ?: "내 정보를 불러오지 못했습니다.")
                }
            }
    }

    private fun showUser(user: User) {
        _state.update {
            it.copy(
                displayName = user.displayName,
                email = user.email,
                joinedAt = formatKoreanDate(user.createdAt.toLocalDateTime(timeZone).date),
            )
        }
    }

    private fun logout() {
        if (_state.value.isLoggingOut) return
        _state.update { it.copy(isLoggingOut = true) }
        viewModelScope.launch {
            authRepository.logout()
            _state.update { it.copy(isLoggingOut = false) }
        }
    }
}
