package com.kotlincalendar.client.presentation.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlincalendar.client.domain.repository.AuthRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    private val _effects = Channel<LoginEffect>(Channel.BUFFERED)
    val effects: Flow<LoginEffect> = _effects.receiveAsFlow()

    fun onAction(action: LoginAction) {
        when (action) {
            is LoginAction.ChangeEmail -> _state.update { it.copy(email = action.email, errorMessage = null) }
            is LoginAction.ChangePassword -> _state.update { it.copy(password = action.password, errorMessage = null) }
            LoginAction.Submit -> login()
            LoginAction.NavigateToSignUp -> {
                viewModelScope.launch {
                    _effects.send(LoginEffect.NavigateToSignUp)
                }
            }
            LoginAction.DismissError -> _state.update { it.copy(errorMessage = null) }
        }
    }

    private fun login() {
        val email = _state.value.email.trim()
        val password = _state.value.password

        if (email.isBlank() || password.isBlank()) {
            _state.update { it.copy(errorMessage = "이메일과 비밀번호를 입력해주세요.") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            authRepository
                .login(email = email, password = password)
                .onSuccess {
                    _state.update { it.copy(isLoading = false) }
                    _effects.send(LoginEffect.NavigateToHome)
                }.onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "로그인에 실패했습니다.",
                        )
                    }
                }
        }
    }
}
