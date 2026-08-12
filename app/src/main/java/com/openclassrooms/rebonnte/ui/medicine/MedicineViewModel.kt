package com.openclassrooms.rebonnte.ui.medicine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openclassrooms.rebonnte.data.repository.AisleRepository
import com.openclassrooms.rebonnte.data.repository.MedicineRepository
import com.openclassrooms.rebonnte.data.repository.MedicineSort
import com.openclassrooms.rebonnte.data.repository.UserRepository
import com.openclassrooms.rebonnte.ui.model.HistoryUi
import com.openclassrooms.rebonnte.ui.model.MedicineUi
import com.openclassrooms.rebonnte.ui.model.toUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

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

    /** Expose pour que le menu puisse indiquer le critere actif : avec cinq
     *  entrees, ne pas savoir laquelle s'applique est deroutant. */
    val currentSort: StateFlow<MedicineSort> = sort.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val medicines: StateFlow<List<MedicineUi>> =
        combine(query, sort) { query, sort -> query to sort }
            .flatMapLatest { (query, sort) -> repository.observeMedicines(query, sort) }
            .combine(aisleNames) { medicines, names ->
                medicines.map { it.toUi(names[it.aisleId]) }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    fun observeMedicine(id: String): Flow<MedicineUi?> =
        repository.observeMedicine(id).combine(aisleNames) { medicine, names ->
            medicine?.toUi(names[medicine.aisleId])
        }

    fun observeHistory(medicineId: String): Flow<List<HistoryUi>> =
        repository.observeHistory(medicineId).map { entries -> entries.map { it.toUi() } }

    /**
     * [delta] peut valoir plus de un : un mouvement de cinquante boites produit
     * **une** entree d'historique et non cinquante. Sans cela, le service
     * qualite cherchant « qui a retire 50 boites » trouverait cinquante lignes
     * de « -1 », ce qui annulerait une partie du benefice de la journalisation.
     */
    fun updateStock(medicineId: String, delta: Int) {
        viewModelScope.launch {
            repository.updateStock(medicineId, delta, currentUserEmail())
        }
    }

    fun deleteMedicine(medicineId: String) {
        viewModelScope.launch {
            repository.deleteMedicine(medicineId, currentUserEmail())
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
