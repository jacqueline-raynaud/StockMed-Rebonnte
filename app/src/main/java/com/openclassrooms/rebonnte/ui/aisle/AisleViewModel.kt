package com.openclassrooms.rebonnte.ui.aisle

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
     * Remplace l'ancien addRandomAisle, qui fabriquait des « Aisle 2 »,
     * « Aisle 3 » sans signification. Un emplacement de stockage porte un nom
     * choisi par l'operateur.
     */
    fun addAisle(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            runCatching { repository.addAisle(name) }
                .onFailure { _actionError.value = it.toUiMessage() }
        }
    }

    fun actionErrorShown() {
        _actionError.value = null
    }
}
