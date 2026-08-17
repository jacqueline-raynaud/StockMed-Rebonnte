package com.openclassrooms.rebonnte.data.repository.impl

import com.openclassrooms.rebonnte.data.model.HistoryAction
import com.openclassrooms.rebonnte.data.model.HistoryDto
import com.openclassrooms.rebonnte.data.model.MedicineDto
import com.openclassrooms.rebonnte.data.repository.MedicineRepository
import com.openclassrooms.rebonnte.data.repository.MedicineSort
import com.openclassrooms.rebonnte.data.repository.StockErrorReason
import com.openclassrooms.rebonnte.data.repository.StockException
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Implementation en memoire, utilisee tant que Firestore n'est pas branche et
 * comme double dans les tests unitaires.
 *
 * Elle definit le comportement attendu du contrat : c'est cette implementation
 * qui sert de reference quand l'implementation Firestore arrivera.
 */
class InMemoryMedicineRepository @Inject constructor() : MedicineRepository {

    private val medicines = MutableStateFlow<List<MedicineDto>>(emptyList())
    private val histories = MutableStateFlow<List<HistoryDto>>(emptyList())

    override fun observeMedicines(query: String, sort: MedicineSort): Flow<List<MedicineDto>> =
        medicines.map { list ->
            list.filterByName(query).sortedBy(sort)
        }

    override fun observeMedicine(id: String): Flow<MedicineDto?> =
        medicines.map { list -> list.firstOrNull { it.id == id } }

    override fun observeHistory(medicineId: String): Flow<List<HistoryDto>> =
        histories.map { list ->
            list.filter { it.medicineId == medicineId }.sortedByDescending { it.date }
        }

    override suspend fun addMedicine(
        name: String,
        stock: Int,
        aisleId: String,
        userEmail: String
    ): MedicineDto {
        val medicine = MedicineDto(
            id = UUID.randomUUID().toString(),
            name = name,
            stock = stock,
            aisleId = aisleId
        )
        medicines.value = medicines.value + medicine
        record(medicine, HistoryAction.CREATE, 0, stock, userEmail, "Medicament cree")
        return medicine
    }

    override suspend fun updateMedicine(
        id: String,
        name: String,
        aisleId: String,
        aisleName: String,
        userEmail: String
    ) {
        val medicine = medicines.value.firstOrNull { it.id == id } ?: return

        val newName = name.trim()
        if (newName.isEmpty()) return

        val changes = describeChanges(medicine, newName, aisleId, aisleName)
        if (changes == null) return

        medicines.value = medicines.value.map {
            if (it.id == id) it.copy(name = newName, aisleId = aisleId) else it
        }
        // Le stock ne bouge pas : avant et apres portent la meme valeur.
        record(
            medicine.copy(name = newName),
            HistoryAction.UPDATE,
            medicine.stock,
            medicine.stock,
            userEmail,
            changes
        )
    }

    override suspend fun updateStock(id: String, delta: Int, userEmail: String) {
        val medicine = medicines.value.firstOrNull { it.id == id } ?: return

        // Meme regle que l'implementation Firestore : un retrait superieur au
        // stock est refuse, pas rabote. Voir MedicineRepositoryImpl.
        val stockAfter = medicine.stock + delta
        if (stockAfter < 0) {
            throw StockException(
                reason = StockErrorReason.INSUFFICIENT_STOCK,
                available = medicine.stock
            )
        }
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
        medicine: MedicineDto,
        action: HistoryAction,
        stockBefore: Int,
        stockAfter: Int,
        userEmail: String,
        details: String
    ) {
        histories.value = histories.value + HistoryDto(
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

private fun List<MedicineDto>.filterByName(query: String): List<MedicineDto> {
    if (query.isBlank()) return this
    val needle = query.lowercase(Locale.getDefault())
    return filter { it.name.lowercase(Locale.getDefault()).contains(needle) }
}

private fun List<MedicineDto>.sortedBy(sort: MedicineSort): List<MedicineDto> = when (sort) {
    MedicineSort.NONE -> this
    // lowercase() : meme ordre alphabetique que l'implementation Firestore, qui
    // trie sur le champ en minuscules.
    MedicineSort.NAME_ASC -> sortedBy { it.name.lowercase(Locale.getDefault()) }
    MedicineSort.NAME_DESC -> sortedByDescending { it.name.lowercase(Locale.getDefault()) }
    MedicineSort.STOCK_ASC -> sortedBy { it.stock }
    MedicineSort.STOCK_DESC -> sortedByDescending { it.stock }
}
