package com.openclassrooms.rebonnte.ui.auth

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable

/** The same screen serves both: only the Name field and the labels differ. */
enum class AuthMode { SIGN_IN, SIGN_UP }

/**
 * The form as it stands.
 *
 * Each field carries its own error: a single message would not say which entry
 * is at fault.
 */
@Immutable
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
