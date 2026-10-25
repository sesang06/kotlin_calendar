package com.kotlincalendar.client.presentation.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlincalendar.client.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 앱 시작 시 저장된 로그인 세션을 복원해 첫 화면을 정한다. */
class AppViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(AppUiState())
    val state: StateFlow<AppUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val user = authRepository.restoreSession()
            _state.value = AppUiState(isRestoringSession = false, isLoggedIn = user != null)
        }
    }
}
