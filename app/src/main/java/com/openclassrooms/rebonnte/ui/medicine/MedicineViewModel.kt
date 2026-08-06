package com.openclassrooms.rebonnte.ui.medicine

import androidx.lifecycle.ViewModel
import com.openclassrooms.rebonnte.data.model.Aisle
import com.openclassrooms.rebonnte.data.model.History
import com.openclassrooms.rebonnte.data.model.HistoryAction
import com.openclassrooms.rebonnte.data.model.Medicine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

class MedicineViewModel : ViewModel() {
    private val _medicines = MutableStateFlow<List<Medicine>>(emptyList())
    val medicines: StateFlow<List<Medicine>> = _medicines.asStateFlow()

    fun addRandomMedicine(aisles: List<Aisle>) {
        // Sans rayon, l'ancien code levait une exception sur nextInt(0).
        if (aisles.isEmpty()) return

        val current = _medicines.value
        _medicines.value = current + Medicine(
            id = UUID.randomUUID().toString(),
            name = "Medicine ${current.size + 1}",
            stock = (0..99).random(),
            aisleId = aisles.random().id
        )
    }

    /**
     * Applique une variation de stock au medicament designe et trace
     * l'operation dans son historique, dans la meme mise a jour d'etat.
     *
     * L'ancien code ecrivait dans `medicines[medicines.size]` (hors bornes,
     * donc plantage systematique) puis ajoutait l'entree a une copie jetee par
     * `toMutableList()`, si bien qu'aucun historique n'etait jamais conserve.
     */
    fun updateStock(medicineId: String, delta: Int, userEmail: String) {
        _medicines.value = _medicines.value.map { medicine ->
            if (medicine.id != medicineId) return@map medicine

            val stockAfter = (medicine.stock + delta).coerceAtLeast(0)
            if (stockAfter == medicine.stock) return@map medicine

            medicine.copy(
                stock = stockAfter,
                histories = medicine.histories + History(
                    id = UUID.randomUUID().toString(),
                    medicineId = medicine.id,
                    medicineName = medicine.name,
                    userEmail = userEmail,
                    date = System.currentTimeMillis(),
                    action = HistoryAction.STOCK_CHANGE,
                    stockBefore = medicine.stock,
                    stockAfter = stockAfter,
                    details = "Stock modifie de ${medicine.stock} a $stockAfter"
                )
            )
        }
    }

    fun filterByName(name: String) {
        val currentMedicines: List<Medicine> = medicines.value
        val filteredMedicines: MutableList<Medicine> = ArrayList()
        for (medicine in currentMedicines) {
            if (medicine.name.lowercase(Locale.getDefault())
                    .contains(name.lowercase(Locale.getDefault()))
            ) {
                filteredMedicines.add(medicine)
            }
        }
        _medicines.value = filteredMedicines
    }

    fun sortByNone() {
        _medicines.value = _medicines.value // Pas de tri
    }

    fun sortByName() {
        _medicines.value = _medicines.value.sortedBy { it.name }
    }

    fun sortByStock() {
        _medicines.value = _medicines.value.sortedBy { it.stock }
    }
}
