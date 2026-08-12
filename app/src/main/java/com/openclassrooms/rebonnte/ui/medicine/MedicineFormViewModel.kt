package com.openclassrooms.rebonnte.ui.medicine

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openclassrooms.rebonnte.R
import com.openclassrooms.rebonnte.data.model.AisleDto
import com.openclassrooms.rebonnte.data.repository.AisleRepository
import com.openclassrooms.rebonnte.data.repository.MedicineRepository
import com.openclassrooms.rebonnte.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MedicineFormUiState(
    val name: String = "",
    val stock: String = "0",
    val aisleId: String = "",
    // Identifiants de ressource : voir AuthUiState pour la raison.
    @StringRes val nameError: Int? = null,
    @StringRes val stockError: Int? = null,
    @StringRes val aisleError: Int? = null,
    val isSubmitting: Boolean = false,
    val isSaved: Boolean = false
)

/**
 * Creation d'un medicament.
 *
 * Remplace l'ancien bouton « + » qui ajoutait un medicament au nom et au stock
 * aleatoires, dans un rayon tire au hasard.
 */
@HiltViewModel
class MedicineFormViewModel @Inject constructor(
    private val medicineRepository: MedicineRepository,
    private val userRepository: UserRepository,
    aisleRepository: AisleRepository
) : ViewModel() {

    /** Alimente la liste deroulante : on choisit parmi ce qui existe. */
    val aisles: StateFlow<List<AisleDto>> = aisleRepository.observeAisles()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _uiState = MutableStateFlow(MedicineFormUiState())
    val uiState: StateFlow<MedicineFormUiState> = _uiState.asStateFlow()

    fun onNameChange(value: String) =
        _uiState.update { it.copy(name = value, nameError = null) }

    fun onStockChange(value: String) =
        _uiState.update { it.copy(stock = value.filter(Char::isDigit), stockError = null) }

    fun onAisleChange(aisleId: String) =
        _uiState.update { it.copy(aisleId = aisleId, aisleError = null) }

    fun submit() {
        val state = _uiState.value
        if (state.isSubmitting) return

        val nameError = if (state.name.isBlank()) R.string.form_error_name_required else null
        val stock = state.stock.toIntOrNull()
        val stockError = when {
            stock == null -> R.string.form_error_quantity_invalid
            stock < 0 -> R.string.form_error_quantity_negative
            else -> null
        }
        val aisleError =
            if (state.aisleId.isBlank()) R.string.form_error_aisle_required else null

        if (nameError != null || stockError != null || aisleError != null) {
            _uiState.update {
                it.copy(nameError = nameError, stockError = stockError, aisleError = aisleError)
            }
            return
        }

        _uiState.update { it.copy(isSubmitting = true) }

        viewModelScope.launch {
            medicineRepository.addMedicine(
                name = state.name.trim(),
                stock = requireNotNull(stock),
                aisleId = state.aisleId,
                userEmail = userRepository.currentUserOrNull()?.email.orEmpty()
            )
            // L'ecran observe isSaved pour revenir a la liste.
            _uiState.update { it.copy(isSubmitting = false, isSaved = true) }
        }
    }
}
