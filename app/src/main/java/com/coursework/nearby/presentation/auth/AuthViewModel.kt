package com.coursework.nearby.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coursework.nearby.domain.error.AppError
import com.coursework.nearby.domain.error.AppException
import com.coursework.nearby.domain.usecase.AuthResult
import com.coursework.nearby.domain.usecase.SignInUseCase
import com.coursework.nearby.domain.usecase.SignUpUseCase
import com.coursework.nearby.domain.validation.CredentialsFieldError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val isSignUp: Boolean = false,
    val isLoading: Boolean = false,
    val fieldErrors: Set<CredentialsFieldError> = emptySet(),
    val error: AppError? = null,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val signIn: SignInUseCase,
    private val signUp: SignUpUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun onEmailChange(value: String) = _state.update {
        it.copy(email = value, fieldErrors = it.fieldErrors - CredentialsFieldError.EMAIL_INVALID, error = null)
    }

    fun onPasswordChange(value: String) = _state.update {
        it.copy(password = value, fieldErrors = it.fieldErrors - CredentialsFieldError.PASSWORD_TOO_SHORT, error = null)
    }

    fun onToggleMode() = _state.update { it.copy(isSignUp = !it.isSignUp, fieldErrors = emptySet(), error = null) }

    /** После успеха экран сменится сам: состояние приложения следит за сессией. */
    fun onSubmit() {
        val current = _state.value
        if (current.isLoading) return
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val result = if (current.isSignUp) signUp(current.email, current.password) else signIn(current.email, current.password)
                if (result is AuthResult.Invalid) _state.update { it.copy(fieldErrors = result.errors) }
            } catch (e: AppException) {
                _state.update { it.copy(error = e.error) }
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }
}
