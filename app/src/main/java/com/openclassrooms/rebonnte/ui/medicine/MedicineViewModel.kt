package com.openclassrooms.rebonnte.ui.medicine

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openclassrooms.rebonnte.data.repository.AisleRepository
import com.openclassrooms.rebonnte.data.repository.MedicineRepository
import com.openclassrooms.rebonnte.data.repository.MedicineSort
import com.openclassrooms.rebonnte.data.repository.UserRepository
import com.openclassrooms.rebonnte.ui.toMessageRes
import com.openclassrooms.rebonnte.ui.whileSignedIn
import com.openclassrooms.rebonnte.ui.model.HistoryUi
import com.openclassrooms.rebonnte.ui.model.MedicineUi
import com.openclassrooms.rebonnte.ui.model.toUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Ce que l'ecran de la liste affiche, en un seul objet.
 *
 * Les trois valeurs etaient exposees separement — la liste, le critere de tri,
 * la recherche. Trois flux se lisent independamment et peuvent donc etre
 * observes dans des etats incoherents : une liste deja triee alors que le menu
 * montre encore l'ancien critere. Un etat unique ne peut pas se contredire.
 *
 * [query] vit ici et non dans la composable : la saisie etait dupliquee entre
 * un `rememberSaveable` de MainActivity et le flux du ViewModel, soit deux
 * sources de verite pour la meme donnee.
 */
@Immutable
data class MedicineUiState(
    val medicines: List<MedicineUi> = emptyList(),
    val sort: MedicineSort = MedicineSort.NONE,
    val query: String = "",
    val isLoading: Boolean = true,
    @StringRes val errorMessage: Int? = null
)

/** L'etat de la fiche detail : le medicament, sa trace, et l'etat du chargement. */
@Immutable
data class MedicineDetailUiState(
    val medicine: MedicineUi? = null,
    val histories: List<HistoryUi> = emptyList(),
    val isLoading: Boolean = true,
    @StringRes val errorMessage: Int? = null
)

/** Etat interne du flux de lecture, avant d'etre combine au tri et a la recherche. */
private data class MedicinesLoad(
    val medicines: List<MedicineUi> = emptyList(),
    val isLoading: Boolean = true,
    @StringRes val error: Int? = null
)

/**
 * Les dependances sont fournies par Hilt. Les tests instancient la classe
 * directement avec leurs propres doubles.
 *
 * Le ViewModel recoit des `*Dto` et n'expose que des `*Ui` : la conversion est
 * son travail. Un ecran qui recevrait un Dto dependrait de la forme de la base
 * de donnees, et un changement de schema remonterait jusqu'a l'affichage.
 *
 * [AisleRepository] est ici pour cela : il fournit les libelles d'emplacement
 * que le medicament ne porte pas.
 */
@HiltViewModel
class MedicineViewModel @Inject constructor(
    private val repository: MedicineRepository,
    private val userRepository: UserRepository,
    aisleRepository: AisleRepository
) : ViewModel() {

    private val aisleNames = aisleRepository.observeAisles()
        .map { aisles -> aisles.associate { it.id to it.name } }

    // Etat de presentation : la recherche et le tri ne touchent jamais la
    // source de verite. L'ancien filterByName ecrasait la liste complete par la
    // liste filtree, ce qui supprimait definitivement les medicaments masques.
    private val query = MutableStateFlow("")
    private val sort = MutableStateFlow(MedicineSort.NONE)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val medicines: Flow<List<MedicineUi>> =
        combine(query, sort) { query, sort -> query to sort }
            .flatMapLatest { (query, sort) -> repository.observeMedicines(query, sort) }
            .combine(aisleNames) { medicines, names ->
                medicines.map { it.toUi(names[it.aisleId]) }
            }

    /**
     * `onStart` fournit l'etat de chargement, `catch` l'etat d'erreur.
     *
     * Sans eux, une liste vide voulait dire trois choses a la fois : le stock
     * est vide, les donnees arrivent, ou la lecture a echoue. L'operateur ne
     * pouvait pas les distinguer — et une lecture qui echouait faisait planter
     * l'application faute d'etre rattrapee.
     */
    private val medicinesLoad: Flow<MedicinesLoad> =
        medicines.whileSignedIn(userRepository, emptyList())
            .map { MedicinesLoad(medicines = it, isLoading = false) }
            .onStart { emit(MedicinesLoad()) }
            .catch { emit(MedicinesLoad(isLoading = false, error = it.toMessageRes())) }

    /**
     * Les echecs d'ecriture sont exposes a part.
     *
     * Ils ne decrivent pas l'etat de l'ecran mais un geste qui n'a pas abouti,
     * et ils doivent pouvoir s'afficher depuis la fiche detail comme depuis la
     * liste. Ce flux ne touche pas Firestore : l'observer en permanence n'ouvre
     * aucun ecouteur.
     */
    private val _actionError = MutableStateFlow<Int?>(null)
    val actionError: StateFlow<Int?> = _actionError.asStateFlow()

    val uiState: StateFlow<MedicineUiState> =
        combine(medicinesLoad, query, sort) { load, query, sort ->
            MedicineUiState(
                medicines = load.medicines,
                sort = sort,
                query = query,
                isLoading = load.isLoading,
                errorMessage = load.error
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MedicineUiState()
        )

    fun observeDetail(medicineId: String): Flow<MedicineDetailUiState> =
        combine(
            repository.observeMedicine(medicineId)
                .combine(aisleNames) { medicine, names -> medicine?.toUi(names[medicine.aisleId]) },
            repository.observeHistory(medicineId)
                .map { entries -> entries.map { it.toUi() } }
        ) { medicine, histories ->
            MedicineDetailUiState(medicine = medicine, histories = histories, isLoading = false)
        }
            .whileSignedIn(userRepository, MedicineDetailUiState(isLoading = false))
            .onStart { emit(MedicineDetailUiState()) }
            .catch {
                emit(MedicineDetailUiState(isLoading = false, errorMessage = it.toMessageRes()))
            }

    /** Appele une fois le message affiche, pour qu'il ne revienne pas. */
    fun actionErrorShown() {
        _actionError.value = null
    }

    /**
     * [delta] peut valoir plus de un : un mouvement de cinquante boites produit
     * **une** entree d'historique et non cinquante. Sans cela, le service
     * qualite cherchant « qui a retire 50 boites » trouverait cinquante lignes
     * de « -1 », ce qui annulerait une partie du benefice de la journalisation.
     */
    fun updateStock(medicineId: String, delta: Int) {
        viewModelScope.launch {
            // Sans runCatching, l'exception remonte au scope et tue le
            // processus : un mouvement de stock hors reseau fermait
            // l'application.
            runCatching { repository.updateStock(medicineId, delta, currentUserEmail()) }
                .onFailure { _actionError.value = it.toMessageRes() }
        }
    }

    fun deleteMedicine(medicineId: String) {
        viewModelScope.launch {
            runCatching { repository.deleteMedicine(medicineId, currentUserEmail()) }
                .onFailure { _actionError.value = it.toMessageRes() }
        }
    }

    fun filterByName(name: String) {
        query.value = name
    }

    /** Une seule entree plutot qu'une methode par critere : le menu passe la
     *  valeur, le ViewModel n'a pas a connaitre les libelles. */
    fun sortBy(criterion: MedicineSort) {
        sort.value = criterion
    }

    /**
     * L'operateur qui signera l'entree d'historique. Lu au moment de
     * l'operation et non conserve : la session peut changer pendant la vie du
     * ViewModel.
     */
    private fun currentUserEmail(): String =
        userRepository.currentUserOrNull()?.email.orEmpty()
}
