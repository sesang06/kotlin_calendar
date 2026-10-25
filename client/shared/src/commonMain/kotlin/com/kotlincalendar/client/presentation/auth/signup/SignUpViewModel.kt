package com.kotlincalendar.client.presentation.auth.signup

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

class SignUpViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(SignUpUiState())
    val state: StateFlow<SignUpUiState> = _state.asStateFlow()

    private val _effects = Channel<SignUpEffect>(Channel.BUFFERED)
    val effects: Flow<SignUpEffect> = _effects.receiveAsFlow()

    fun onAction(action: SignUpAction) {
        when (action) {
            is SignUpAction.ChangeEmail -> _state.update { it.copy(email = action.email, errorMessage = null) }
            is SignUpAction.ChangePassword -> _state.update { it.copy(password = action.password, errorMessage = null) }
            is SignUpAction.ChangeDisplayName -> _state.update { it.copy(displayName = action.displayName, errorMessage = null) }
            SignUpAction.Submit -> signUp()
            SignUpAction.NavigateToLogin -> {
                viewModelScope.launch {
                    _effects.send(SignUpEffect.NavigateToLogin)
                }
            }
            SignUpAction.DismissError -> _state.update { it.copy(errorMessage = null) }
        }
    }

    private fun signUp() {
        val email = _state.value.email.trim()
        val password = _state.value.password
        val displayName = _state.value.displayName.trim()

        if (email.isBlank() || password.isBlank() || displayName.isBlank()) {
            _state.update { it.copy(errorMessage = "모든 항목을 입력해주세요.") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            authRepository
                .signUp(email = email, password = password, displayName = displayName)
                .onSuccess {
                    _state.update { it.copy(isLoading = false) }
                    _effects.send(SignUpEffect.NavigateToHome)
                }.onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "회원가입에 실패했습니다.",
                        )
                    }
                }
        }
    }
}
