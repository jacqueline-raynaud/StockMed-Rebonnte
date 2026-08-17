package com.openclassrooms.rebonnte.ui.aisle

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openclassrooms.rebonnte.R
import com.openclassrooms.rebonnte.data.repository.AisleRepository
import com.openclassrooms.rebonnte.data.repository.UserRepository
import com.openclassrooms.rebonnte.ui.UiMessage
import com.openclassrooms.rebonnte.ui.toMessageRes
import com.openclassrooms.rebonnte.ui.toUiMessage
import com.openclassrooms.rebonnte.ui.whileSignedIn
import com.openclassrooms.rebonnte.ui.model.AisleUi
import com.openclassrooms.rebonnte.ui.model.toUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class AisleUiState(
    val aisles: List<AisleUi> = emptyList(),
    val isLoading: Boolean = true,
    @StringRes val errorMessage: Int? = null
)

@HiltViewModel
class AisleViewModel @Inject constructor(
    private val repository: AisleRepository,
    userRepository: UserRepository
) : ViewModel() {

    private val _actionError = MutableStateFlow<UiMessage?>(null)
    val actionError: StateFlow<UiMessage?> = _actionError.asStateFlow()

    val uiState: StateFlow<AisleUiState> = repository.observeAisles()
        .map { aisles -> AisleUiState(aisles.map { it.toUi() }, isLoading = false) }
        .whileSignedIn(userRepository, AisleUiState(isLoading = false))
        .onStart { emit(AisleUiState()) }
        .catch { emit(AisleUiState(isLoading = false, errorMessage = it.toMessageRes())) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AisleUiState()
        )

    /**
     * Erreur de saisie de la fenetre de creation, exposee a part de
     * [actionError] : elle s'affiche **sous le champ**, et non dans une fenetre
     * par-dessus une autre fenetre.
     */
    private val _newAisleError = MutableStateFlow<Int?>(null)
    val newAisleError: StateFlow<Int?> = _newAisleError.asStateFlow()

    /** Passe a vrai quand la creation a abouti : la fenetre peut se fermer. */
    private val _aisleCreated = MutableStateFlow(false)
    val aisleCreated: StateFlow<Boolean> = _aisleCreated.asStateFlow()

    /**
     * Remplace l'ancien addRandomAisle, qui fabriquait des « Aisle 2 »,
     * « Aisle 3 » sans signification. Un emplacement de stockage porte un nom
     * choisi par l'operateur.
     *
     * Deux refus possibles :
     *
     * - **un nom vide ou fait d'espaces** — le bouton est deja desactive dans ce
     *   cas, mais la regle est verifiee ici aussi : elle ne doit pas dependre
     *   d'un detail d'affichage ;
     * - **un nom deja pris**, a la casse et aux espaces pres. Deux « Stockage
     *   froid » seraient indiscernables dans la liste deroulante du formulaire
     *   de medicament, et l'operateur ne saurait pas lequel il choisit.
     */
    fun addAisle(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            _newAisleError.value = R.string.form_error_name_required
            return
        }

        viewModelScope.launch {
            // Lu a la source plutot que dans l'etat de l'ecran : celui-ci est
            // partage en WhileSubscribed et pourrait etre vide si la liste
            // n'etait pas observee.
            val existing = runCatching { repository.observeAisles().first() }
                .getOrDefault(emptyList())

            if (existing.any { it.name.trim().equals(trimmed, ignoreCase = true) }) {
                _newAisleError.value = R.string.aisle_error_duplicate
                return@launch
            }

            runCatching { repository.addAisle(trimmed) }
                .onSuccess {
                    _newAisleError.value = null
                    _aisleCreated.value = true
                }
                .onFailure { _actionError.value = it.toUiMessage() }
        }
    }

    /** Appele a chaque frappe : l'erreur ne doit pas survivre a la correction. */
    fun clearNewAisleError() {
        _newAisleError.value = null
    }

    fun aisleCreatedShown() {
        _aisleCreated.value = false
    }

    fun actionErrorShown() {
        _actionError.value = null
    }
}
