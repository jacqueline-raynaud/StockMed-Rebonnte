package com.openclassrooms.rebonnte.ui.medicine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openclassrooms.rebonnte.data.model.HistoryDto
import com.openclassrooms.rebonnte.data.model.MedicineDto
import com.openclassrooms.rebonnte.data.repository.MedicineRepository
import com.openclassrooms.rebonnte.data.repository.MedicineSort
import com.openclassrooms.rebonnte.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Les dependances sont fournies par Hilt. Les tests instancient la classe
 * directement avec leurs propres doubles.
 */
@HiltViewModel
class MedicineViewModel @Inject constructor(
    private val repository: MedicineRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    // Etat de presentation : la recherche et le tri ne touchent jamais la
    // source de verite. L'ancien filterByName ecrasait la liste complete par la
    // liste filtree, ce qui supprimait definitivement les medicaments masques.
    private val query = MutableStateFlow("")
    private val sort = MutableStateFlow(MedicineSort.NONE)

    /** Expose pour que le menu puisse indiquer le critere actif : avec cinq
     *  entrees, ne pas savoir laquelle s'applique est deroutant. */
    val currentSort: StateFlow<MedicineSort> = sort.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val medicines: StateFlow<List<MedicineDto>> =
        combine(query, sort) { query, sort -> query to sort }
            .flatMapLatest { (query, sort) -> repository.observeMedicines(query, sort) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    fun observeMedicine(id: String): Flow<MedicineDto?> = repository.observeMedicine(id)

    fun observeHistory(medicineId: String): Flow<List<HistoryDto>> =
        repository.observeHistory(medicineId)

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
