package com.openclassrooms.rebonnte.ui

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openclassrooms.rebonnte.R
import com.openclassrooms.rebonnte.data.network.NetworkMonitor
import com.openclassrooms.rebonnte.data.preferences.ThemeMode
import com.openclassrooms.rebonnte.data.preferences.ThemeRepository
import com.openclassrooms.rebonnte.data.repository.AisleRepository
import com.openclassrooms.rebonnte.data.repository.UserRepository
import com.openclassrooms.rebonnte.ui.model.UserUi
import com.openclassrooms.rebonnte.ui.model.toUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Internal state, combined into [MainUiState] : the screen has no use for a
 * deletion split in two, and the two values must never be exposed apart.
 */
private data class AccountDeletion(
    val inProgress: Boolean = false,
    @StringRes val error: Int? = null
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val aisleRepository: AisleRepository,
    private val themeRepository: ThemeRepository,
    networkMonitor: NetworkMonitor
) : ViewModel() {

    private val currentUser: StateFlow<UserUi?> = userRepository.currentUser
        .map { it?.toUi() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = userRepository.currentUserOrNull()?.toUi()
        )

    private val _welcomeAcknowledged = MutableStateFlow(false)
    private val _accountDeletion = MutableStateFlow(AccountDeletion())

    val uiState: StateFlow<MainUiState> =
        combine(
            currentUser,
            _welcomeAcknowledged,
            networkMonitor.isOnline,
            themeRepository.themeMode,
            _accountDeletion
        ) { user, acknowledged, isOnline, themeMode, deletion ->
            MainUiState(
                user = user,
                welcomeAcknowledged = acknowledged,
                appState = if (isOnline) AppState.READY else AppState.OFFLINE,
                themeMode = themeMode,
                isDeletingAccount = deletion.inProgress,
                deleteAccountError = deletion.error
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = MainUiState(user = currentUser.value)
        )

    init {
        viewModelScope.launch {
            currentUser.filterNotNull().first()
             runCatching { aisleRepository.ensureDefaultStorageLocations() }
        }
    }

    fun deleteAccount(password: String) {
        if (_accountDeletion.value.inProgress) return

        _accountDeletion.value = AccountDeletion(inProgress = true)
        viewModelScope.launch {
            val result = userRepository.deleteAccount(password)
            _accountDeletion.value = AccountDeletion(
                inProgress = false,
                error = result.exceptionOrNull()?.let(::deletionMessageFor)
            )
        }
    }

    fun deleteAccountErrorShown() {
        _accountDeletion.value = _accountDeletion.value.copy(error = null)
    }

    @StringRes
    private fun deletionMessageFor(error: Throwable): Int = when {
        error.message?.contains("password is invalid", ignoreCase = true) == true ||
            error.message?.contains("credential is incorrect", ignoreCase = true) == true ->
            R.string.auth_error_bad_credentials

        error.message?.contains("network", ignoreCase = true) == true ->
            R.string.error_network

        else -> R.string.error_generic
    }

    fun setThemeMode(mode: ThemeMode) {
        themeRepository.setThemeMode(mode)
    }

    fun acknowledgeWelcome() {
        _welcomeAcknowledged.value = true
    }

    fun signOut() {
        _welcomeAcknowledged.value = false
        userRepository.signOut()
    }
}
