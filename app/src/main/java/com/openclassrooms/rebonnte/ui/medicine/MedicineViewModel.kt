package com.openclassrooms.rebonnte.ui.medicine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openclassrooms.rebonnte.data.model.Aisle
import com.openclassrooms.rebonnte.data.model.History
import com.openclassrooms.rebonnte.data.model.Medicine
import com.openclassrooms.rebonnte.data.repository.MedicineRepository
import com.openclassrooms.rebonnte.data.repository.MedicineSort
import com.openclassrooms.rebonnte.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
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

    @OptIn(ExperimentalCoroutinesApi::class)
    val medicines: StateFlow<List<Medicine>> =
        combine(query, sort) { query, sort -> query to sort }
            .flatMapLatest { (query, sort) -> repository.observeMedicines(query, sort) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    fun observeMedicine(id: String): Flow<Medicine?> = repository.observeMedicine(id)

    fun observeHistory(medicineId: String): Flow<List<History>> =
        repository.observeHistory(medicineId)

    fun addRandomMedicine(aisles: List<Aisle>) {
        // Sans rayon, l'ancien code levait une exception sur nextInt(0).
        if (aisles.isEmpty()) return

        viewModelScope.launch {
            repository.addMedicine(
                name = "Medicine ${medicines.value.size + 1}",
                stock = (0..99).random(),
                aisleId = aisles.random().id,
                userEmail = currentUserEmail()
            )
        }
    }

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

    fun sortByNone() {
        sort.value = MedicineSort.NONE
    }

    fun sortByName() {
        sort.value = MedicineSort.NAME
    }

    fun sortByStock() {
        sort.value = MedicineSort.STOCK
    }

    /**
     * L'operateur qui signera l'entree d'historique. Lu au moment de
     * l'operation et non conserve : la session peut changer pendant la vie du
     * ViewModel.
     */
    private fun currentUserEmail(): String =
        userRepository.currentUserOrNull()?.email.orEmpty()
}
