package com.openclassrooms.rebonnte.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val welcomeAcknowledged: Boolean = false
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val aisleRepository: AisleRepository
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

    val uiState: StateFlow<MainUiState> =
        combine(currentUser, _welcomeAcknowledged) { user, acknowledged ->
            MainUiState(user = user, welcomeAcknowledged = acknowledged)
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

    fun acknowledgeWelcome() {
        _welcomeAcknowledged.value = true
    }

    fun signOut() {
        _welcomeAcknowledged.value = false
        userRepository.signOut()
    }
}
