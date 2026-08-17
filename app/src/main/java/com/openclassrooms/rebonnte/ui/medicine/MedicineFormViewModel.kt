package com.openclassrooms.rebonnte.ui.medicine

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openclassrooms.rebonnte.R
import com.openclassrooms.rebonnte.data.repository.AisleRepository
import com.openclassrooms.rebonnte.data.repository.MedicineRepository
import com.openclassrooms.rebonnte.data.repository.UserRepository
import com.openclassrooms.rebonnte.ui.navigation.Destinations
import com.openclassrooms.rebonnte.ui.toMessageRes
import com.openclassrooms.rebonnte.ui.whileSignedIn
import com.openclassrooms.rebonnte.ui.model.AisleUi
import com.openclassrooms.rebonnte.ui.model.toUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
    /** Echec de l'enregistrement lui-meme, par opposition aux erreurs de saisie. */
    @StringRes val submitError: Int? = null,
    val isSubmitting: Boolean = false,
    val isSaved: Boolean = false,
    /**
     * Correction d'une fiche existante plutot que creation.
     *
     * L'ecran s'en sert pour masquer la quantite initiale et changer le libelle
     * du bouton : **le stock ne se corrige pas par ce formulaire**, il ne bouge
     * que par un mouvement trace.
     */
    val isEditing: Boolean = false
)

/**
 * Creation **et** correction d'un medicament.
 *
 * Un seul formulaire pour les deux : les champs sont les memes, les regles de
 * validation aussi. Les separer aurait duplique la liste deroulante des
 * emplacements et ses controles, avec le risque de les voir diverger.
 *
 * Le mode se deduit de la route : `medicine/new` ne porte pas d'identifiant,
 * `medicine/{id}/edit` si.
 *
 * Remplace l'ancien bouton « + » qui ajoutait un medicament au nom et au stock
 * aleatoires, dans un rayon tire au hasard.
 */
@HiltViewModel
class MedicineFormViewModel @Inject constructor(
    private val medicineRepository: MedicineRepository,
    private val userRepository: UserRepository,
    aisleRepository: AisleRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val medicineId: String? = savedStateHandle[Destinations.MEDICINE_ID_ARG]

    /** Alimente la liste deroulante : on choisit parmi ce qui existe. */
    val aisles: StateFlow<List<AisleUi>> = aisleRepository.observeAisles()
        .map { aisles -> aisles.map { it.toUi() } }
        .whileSignedIn(userRepository, emptyList())
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _uiState = MutableStateFlow(MedicineFormUiState(isEditing = medicineId != null))
    val uiState: StateFlow<MedicineFormUiState> = _uiState.asStateFlow()

    init {
        // Prechargement de la fiche a corriger. `first()` et non une
        // observation continue : le formulaire ne doit pas ecraser la saisie en
        // cours si un collegue modifie la meme fiche pendant l'edition.
        medicineId?.let { id ->
            viewModelScope.launch {
                val medicine = runCatching { medicineRepository.observeMedicine(id).first() }
                    .getOrNull()

                medicine?.let { loaded ->
                    _uiState.update {
                        it.copy(
                            name = loaded.name,
                            stock = loaded.stock.toString(),
                            aisleId = loaded.aisleId
                        )
                    }
                }
            }
        }
    }

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
        // La quantite n'est pas saisissable en correction : elle n'a pas a etre
        // validee, et un stock devenu illisible ne doit pas bloquer un simple
        // changement de nom.
        val stockError = when {
            state.isEditing -> null
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

        _uiState.update { it.copy(isSubmitting = true, submitError = null) }

        viewModelScope.launch {
            val userEmail = userRepository.currentUserOrNull()?.email.orEmpty()
            // Le libelle accompagne la correction pour rendre l'entree
            // d'historique lisible : voir MedicineRepository.updateMedicine.
            val aisleName = aisles.value.firstOrNull { it.id == state.aisleId }?.name.orEmpty()

            runCatching {
                if (medicineId != null) {
                    medicineRepository.updateMedicine(
                        id = medicineId,
                        name = state.name.trim(),
                        aisleId = state.aisleId,
                        aisleName = aisleName,
                        userEmail = userEmail
                    )
                } else {
                    medicineRepository.addMedicine(
                        name = state.name.trim(),
                        stock = requireNotNull(stock),
                        aisleId = state.aisleId,
                        userEmail = userEmail
                    )
                }
            }.fold(
                // L'ecran observe isSaved pour revenir a la liste.
                onSuccess = { _uiState.update { it.copy(isSubmitting = false, isSaved = true) } },
                // On reste sur le formulaire : la saisie est conservee, le geste
                // peut etre repete sans tout retaper.
                onFailure = { error ->
                    _uiState.update {
                        it.copy(isSubmitting = false, submitError = error.toMessageRes())
                    }
                }
            )
        }
    }
}
