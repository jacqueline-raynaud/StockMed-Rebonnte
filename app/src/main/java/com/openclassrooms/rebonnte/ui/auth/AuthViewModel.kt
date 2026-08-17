package com.openclassrooms.rebonnte.ui.auth

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openclassrooms.rebonnte.R
import com.openclassrooms.rebonnte.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AuthMode { SIGN_IN, SIGN_UP }

data class AuthUiState(
    val mode: AuthMode = AuthMode.SIGN_IN,
    val email: String = "",
    val password: String = "",
    val displayName: String = "",
    @StringRes val emailError: Int? = null,
    @StringRes val passwordError: Int? = null,
    @StringRes val displayNameError: Int? = null,
    @StringRes val formError: Int? = null,
    val isSubmitting: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) =
        _uiState.update { it.copy(email = value, emailError = null, formError = null) }

    fun onPasswordChange(value: String) =
        _uiState.update { it.copy(password = value, passwordError = null, formError = null) }

    fun onDisplayNameChange(value: String) =
        _uiState.update { it.copy(displayName = value, displayNameError = null, formError = null) }

    fun toggleMode() = _uiState.update {
        AuthUiState(
            mode = if (it.mode == AuthMode.SIGN_IN) AuthMode.SIGN_UP else AuthMode.SIGN_IN,
            email = it.email
        )
    }

    /**
     * Validation before the network call: no need to query
     * Firebase for an empty field.
     */
    fun submit() {
        val state = _uiState.value
        if (state.isSubmitting) return

        val emailError = when {
            state.email.isBlank() -> R.string.auth_error_email_required
            !state.email.contains('@') || !state.email.contains('.') ->
                R.string.auth_error_email_invalid

            else -> null
        }
        val passwordError = when {
            state.password.isEmpty() -> R.string.auth_error_password_required
            // Minimum impose par Firebase Authentication.
            state.password.length < 6 -> R.string.auth_error_password_too_short
            else -> null
        }
        val displayNameError =
            if (state.mode == AuthMode.SIGN_UP && state.displayName.isBlank()) {
                R.string.auth_error_name_required
            } else {
                null
            }

        if (emailError != null || passwordError != null || displayNameError != null) {
            _uiState.update {
                it.copy(
                    emailError = emailError,
                    passwordError = passwordError,
                    displayNameError = displayNameError
                )
            }
            return
        }

        _uiState.update { it.copy(isSubmitting = true, formError = null) }

        viewModelScope.launch {
            val result = when (state.mode) {
                AuthMode.SIGN_IN -> userRepository.signIn(state.email, state.password)
                AuthMode.SIGN_UP ->
                    userRepository.signUp(state.email, state.password, state.displayName)
            }

            // MainViewModel observes the authentication stream, and navigation follows
            _uiState.update {
                it.copy(
                    isSubmitting = false,
                    formError = result.exceptionOrNull()?.let(::messageFor)
                )
            }
        }
    }

    @StringRes
    private fun messageFor(error: Throwable): Int = when {
        error.message?.contains("password is invalid", ignoreCase = true) == true ||
            error.message?.contains("credential is incorrect", ignoreCase = true) == true ->
            R.string.auth_error_bad_credentials

        error.message?.contains("no user record", ignoreCase = true) == true ->
            R.string.auth_error_no_account

        error.message?.contains("already in use", ignoreCase = true) == true ->
            R.string.auth_error_email_in_use

        error.message?.contains("network", ignoreCase = true) == true ->
            R.string.auth_error_network

        else -> R.string.auth_error_generic
    }
}
