package com.openclassrooms.rebonnte.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
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
 * Etat applicatif : qui est connecte, et l'ecran d'accueil a-t-il ete valide.
 *
 * [welcomeAcknowledged] vit dans un ViewModel et non dans un `remember` : il
 * doit survivre a une rotation d'ecran mais **pas** au relancement de
 * l'application. Sur un telephone partage entre operateurs, chaque demarrage
 * doit repasser par « Bonjour X, si ce n'est pas vous, deconnectez-vous ».
 */
/**
 * L'etat general de l'application, celui qui vaut pour tous les ecrans.
 *
 * [OFFLINE] n'est pas une erreur : l'application fonctionne, Firestore sert son
 * cache et met les ecritures en attente. C'est justement pourquoi il faut le
 * dire — sans indication, un stock vide faute de cache se lit comme un stock
 * reellement vide.
 */
enum class AppState { READY, OFFLINE }

/** Etat interne de la suppression de compte, avant d'etre fondu dans l'etat public. */
private data class AccountDeletion(
    val inProgress: Boolean = false,
    @StringRes val error: Int? = null
)

/**
 * L'etat qui commande la navigation : qui est connecte, et l'accueil a-t-il
 * ete valide.
 *
 * Les deux valeurs sont lues ensemble a chaque decision de navigation. Separees,
 * elles pouvaient etre observees dans un ordre different de celui ou elles
 * changent — une session fermee avec un accueil encore valide, par exemple.
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
            // Lecture synchrone : sans elle, l'ecran de connexion apparaitrait
            // brievement au demarrage meme pour une session deja ouverte.
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
            // L'amorcage attend une session : les regles Firestore refusent
            // toute ecriture a un utilisateur non authentifie.
            currentUser.filterNotNull().first()
            // Idempotent : les emplacements ont des identifiants de document
            // fixes, l'appeler a chaque session ne cree pas de doublon.
            runCatching { aisleRepository.ensureDefaultStorageLocations() }
        }
    }

    /**
     * Supprime le compte de l'operateur connecte.
     *
     * En cas de succes, rien a faire ici : la session se ferme, [currentUser]
     * passe a null et la navigation renvoie sur l'ecran de connexion.
     *
     * L'historique n'est pas touche. Voir [UserRepository.deleteAccount].
     */
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

    /**
     * Le motif d'echec le plus frequent est un mot de passe faux : Firebase le
     * signale comme un identifiant invalide lors de la re-authentification.
     */
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
