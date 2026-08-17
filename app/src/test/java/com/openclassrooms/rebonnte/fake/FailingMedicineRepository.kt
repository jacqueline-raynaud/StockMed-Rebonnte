package com.openclassrooms.rebonnte.fake

import com.openclassrooms.rebonnte.data.model.HistoryDto
import com.openclassrooms.rebonnte.data.model.MedicineDto
import com.openclassrooms.rebonnte.data.repository.MedicineRepository
import com.openclassrooms.rebonnte.data.repository.MedicineSort
import com.openclassrooms.rebonnte.data.repository.StockErrorReason
import com.openclassrooms.rebonnte.data.repository.StockException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Un depot qui echoue toujours, avec la raison demandee.
 *
 * Les doubles en memoire ne tombent jamais en panne : c'est leur interet, et
 * c'est aussi pourquoi ils n'ont jamais revele que l'application mourait sur
 * une erreur Firestore. Celui-ci existe pour verifier le chemin d'echec.
 */
class FailingMedicineRepository(
    private val reason: StockErrorReason = StockErrorReason.NETWORK
) : MedicineRepository {

    private fun failure(): Nothing = throw StockException(reason)

    override fun observeMedicines(query: String, sort: MedicineSort): Flow<List<MedicineDto>> =
        flow { failure() }

    override fun observeMedicine(id: String): Flow<MedicineDto?> = flow { failure() }

    override fun observeHistory(medicineId: String): Flow<List<HistoryDto>> = flow { failure() }

    override suspend fun addMedicine(
        name: String,
        stock: Int,
        aisleId: String,
        userEmail: String
    ): MedicineDto = failure()

    override suspend fun updateMedicine(
        id: String,
        name: String,
        aisleId: String,
        aisleName: String,
        userEmail: String
    ) = failure()

    override suspend fun updateStock(id: String, delta: Int, userEmail: String) = failure()

    override suspend fun deleteMedicine(id: String, userEmail: String) = failure()
}
