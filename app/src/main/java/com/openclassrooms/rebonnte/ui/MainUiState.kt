package com.openclassrooms.rebonnte.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.openclassrooms.rebonnte.data.preferences.ThemeMode
import com.openclassrooms.rebonnte.ui.model.UserUi

/** Whether the application can be trusted to show real data. */
enum class AppState { READY, OFFLINE }

/**
 * What the frame around every screen needs: who is signed in, whether the
 * welcome screen has been acknowledged, the network, the theme, and the
 * account deletion in progress.
 */
@Immutable
data class MainUiState(
    val user: UserUi? = null,
    val welcomeAcknowledged: Boolean = false,
    val appState: AppState = AppState.READY,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isDeletingAccount: Boolean = false,
    @StringRes val deleteAccountError: Int? = null
)
