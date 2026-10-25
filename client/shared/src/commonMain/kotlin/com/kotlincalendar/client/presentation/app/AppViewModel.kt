package com.kotlincalendar.client.presentation.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlincalendar.client.domain.model.AuthState
import com.kotlincalendar.client.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 앱 시작 시 저장된 로그인 세션을 복원해 첫 화면을 정하고, 이후 로그인 상태 변화를 전달한다. */
class AppViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(AppUiState())
    val state: StateFlow<AppUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val user = authRepository.restoreSession()
            _state.value =
                AppUiState(
                    isRestoringSession = false,
                    startLoggedIn = user != null,
                    isLoggedIn = user != null,
                )
            authRepository.authState.collect { authState ->
                _state.update { it.copy(isLoggedIn = authState is AuthState.Authenticated) }
            }
        }
    }

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }
}
