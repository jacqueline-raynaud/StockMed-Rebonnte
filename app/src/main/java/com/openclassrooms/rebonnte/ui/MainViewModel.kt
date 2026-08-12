package com.openclassrooms.rebonnte.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openclassrooms.rebonnte.data.model.UserDto
import com.openclassrooms.rebonnte.data.repository.AisleRepository
import com.openclassrooms.rebonnte.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
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
@HiltViewModel
class MainViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val aisleRepository: AisleRepository
) : ViewModel() {

    val currentUser: StateFlow<UserDto?> = userRepository.currentUser
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            // Lecture synchrone : sans elle, l'ecran de connexion apparaitrait
            // brievement au demarrage meme pour une session deja ouverte.
            initialValue = userRepository.currentUserOrNull()
        )

    private val _welcomeAcknowledged = MutableStateFlow(false)
    val welcomeAcknowledged: StateFlow<Boolean> = _welcomeAcknowledged.asStateFlow()

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
