package com.openclassrooms.rebonnte.ui.aisle

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openclassrooms.rebonnte.data.repository.AisleRepository
import com.openclassrooms.rebonnte.data.repository.UserRepository
import com.openclassrooms.rebonnte.ui.whileSignedIn
import com.openclassrooms.rebonnte.ui.model.AisleUi
import com.openclassrooms.rebonnte.ui.model.toUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Un seul champ aujourd'hui. Les indicateurs de chargement et les erreurs
 * reseau viendront s'y ajouter avec T-24 ; les inventer maintenant reviendrait
 * a exposer un `isLoading` qui vaudrait toujours `false`.
 */
@Immutable
data class AisleUiState(
    val aisles: List<AisleUi> = emptyList()
)

@HiltViewModel
class AisleViewModel @Inject constructor(
    private val repository: AisleRepository,
    userRepository: UserRepository
) : ViewModel() {

    val uiState: StateFlow<AisleUiState> = repository.observeAisles()
        .map { aisles -> AisleUiState(aisles.map { it.toUi() }) }
        .whileSignedIn(userRepository, AisleUiState())
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AisleUiState()
        )

    /**
     * Remplace l'ancien addRandomAisle, qui fabriquait des « Aisle 2 »,
     * « Aisle 3 » sans signification. Un emplacement de stockage porte un nom
     * choisi par l'operateur.
     */
    fun addAisle(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.addAisle(name)
        }
    }
}
