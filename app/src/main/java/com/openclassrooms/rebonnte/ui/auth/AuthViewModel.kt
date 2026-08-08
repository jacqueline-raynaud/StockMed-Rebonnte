package com.openclassrooms.rebonnte.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val emailError: String? = null,
    val passwordError: String? = null,
    val displayNameError: String? = null,
    val formError: String? = null,
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
     * La validation se fait avant tout appel reseau : inutile de solliciter
     * Firebase pour un champ vide.
     */
    fun submit() {
        val state = _uiState.value
        if (state.isSubmitting) return

        val emailError = when {
            state.email.isBlank() -> "L'adresse e-mail est obligatoire"
            !state.email.contains('@') || !state.email.contains('.') ->
                "Adresse e-mail invalide"

            else -> null
        }
        val passwordError = when {
            state.password.isEmpty() -> "Le mot de passe est obligatoire"
            // Minimum impose par Firebase Authentication.
            state.password.length < 6 -> "Au moins 6 caracteres"
            else -> null
        }
        val displayNameError =
            if (state.mode == AuthMode.SIGN_UP && state.displayName.isBlank()) {
                "Le nom est obligatoire"
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

            // En cas de succes, rien a faire ici : MainViewModel observe le flux
            // d'authentification et la navigation suit.
            _uiState.update {
                it.copy(
                    isSubmitting = false,
                    formError = result.exceptionOrNull()?.let(::messageFor)
                )
            }
        }
    }

    private fun messageFor(error: Throwable): String = when {
        error.message?.contains("password is invalid", ignoreCase = true) == true ||
            error.message?.contains("credential is incorrect", ignoreCase = true) == true ->
            "E-mail ou mot de passe incorrect"

        error.message?.contains("no user record", ignoreCase = true) == true ->
            "Aucun compte ne correspond a cette adresse"

        error.message?.contains("already in use", ignoreCase = true) == true ->
            "Un compte existe deja avec cette adresse"

        error.message?.contains("network", ignoreCase = true) == true ->
            "Connexion impossible : verifiez votre reseau"

        else -> "La connexion a echoue. Reessayez."
    }
}
