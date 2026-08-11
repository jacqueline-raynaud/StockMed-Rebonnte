package com.openclassrooms.rebonnte.data.repository

import com.openclassrooms.rebonnte.data.model.History
import com.openclassrooms.rebonnte.data.model.HistoryAction
import com.openclassrooms.rebonnte.data.model.Medicine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

/**
 * Implementation en memoire, utilisee tant que Firestore n'est pas branche et
 * comme double dans les tests unitaires.
 *
 * Elle definit le comportement attendu du contrat : c'est cette implementation
 * qui sert de reference quand l'implementation Firestore arrivera.
 */
class InMemoryMedicineRepository @Inject constructor() : MedicineRepository {

    private val medicines = MutableStateFlow<List<Medicine>>(emptyList())
    private val histories = MutableStateFlow<List<History>>(emptyList())

    override fun observeMedicines(query: String, sort: MedicineSort): Flow<List<Medicine>> =
        medicines.map { list ->
            list.filterByName(query).sortedBy(sort)
        }

    override fun observeMedicine(id: String): Flow<Medicine?> =
        medicines.map { list -> list.firstOrNull { it.id == id } }

    override fun observeHistory(medicineId: String): Flow<List<History>> =
        histories.map { list ->
            list.filter { it.medicineId == medicineId }.sortedByDescending { it.date }
        }

    override suspend fun addMedicine(
        name: String,
        stock: Int,
        aisleId: String,
        userEmail: String
    ): Medicine {
        val medicine = Medicine(
            id = UUID.randomUUID().toString(),
            name = name,
            stock = stock,
            aisleId = aisleId
        )
        medicines.value = medicines.value + medicine
        record(medicine, HistoryAction.CREATE, 0, stock, userEmail, "Medicament cree")
        return medicine
    }

    override suspend fun updateStock(id: String, delta: Int, userEmail: String) {
        val medicine = medicines.value.firstOrNull { it.id == id } ?: return

        val stockAfter = (medicine.stock + delta).coerceAtLeast(0)
        if (stockAfter == medicine.stock) return

        medicines.value = medicines.value.map {
            if (it.id == id) it.copy(stock = stockAfter) else it
        }
        record(
            medicine,
            HistoryAction.STOCK_CHANGE,
            medicine.stock,
            stockAfter,
            userEmail,
            "Stock modifie de ${medicine.stock} a $stockAfter"
        )
    }

    override suspend fun deleteMedicine(id: String, userEmail: String) {
        val medicine = medicines.value.firstOrNull { it.id == id } ?: return

        medicines.value = medicines.value.filterNot { it.id == id }
        // La trace survit au medicament supprime.
        record(medicine, HistoryAction.DELETE, medicine.stock, 0, userEmail, "Medicament supprime")
    }

    private fun record(
        medicine: Medicine,
        action: HistoryAction,
        stockBefore: Int,
        stockAfter: Int,
        userEmail: String,
        details: String
    ) {
        histories.value = histories.value + History(
            id = UUID.randomUUID().toString(),
            medicineId = medicine.id,
            medicineName = medicine.name,
            userEmail = userEmail,
            date = System.currentTimeMillis(),
            action = action,
            stockBefore = stockBefore,
            stockAfter = stockAfter,
            details = details
        )
    }
}

private fun List<Medicine>.filterByName(query: String): List<Medicine> {
    if (query.isBlank()) return this
    val needle = query.lowercase(Locale.getDefault())
    return filter { it.name.lowercase(Locale.getDefault()).contains(needle) }
}

private fun List<Medicine>.sortedBy(sort: MedicineSort): List<Medicine> = when (sort) {
    MedicineSort.NONE -> this
    // lowercase() : meme ordre alphabetique que l'implementation Firestore, qui
    // trie sur le champ en minuscules.
    MedicineSort.NAME_ASC -> sortedBy { it.name.lowercase(Locale.getDefault()) }
    MedicineSort.NAME_DESC -> sortedByDescending { it.name.lowercase(Locale.getDefault()) }
    MedicineSort.STOCK_ASC -> sortedBy { it.stock }
    MedicineSort.STOCK_DESC -> sortedByDescending { it.stock }
}
