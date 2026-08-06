package com.openclassrooms.rebonnte.data.repository

import com.openclassrooms.rebonnte.data.model.History
import com.openclassrooms.rebonnte.data.model.Medicine
import kotlinx.coroutines.flow.Flow

/**
 * Acces aux medicaments du stock.
 *
 * Toutes les operations d'ecriture tracent elles-memes l'historique. C'est
 * volontaire : tant qu'un appelant devait penser a journaliser son action, il
 * finissait par l'oublier, d'ou les manques signales par le service qualite.
 * Ici l'oubli est impossible, la trace fait partie de l'operation.
 *
 * L'historique est expose separement de [Medicine] pour deux raisons : une
 * suppression doit rester tracee alors que le medicament disparait, et un
 * historique embarque dans le document grossirait sans limite.
 */
interface MedicineRepository {

    fun observeMedicines(
        query: String = "",
        sort: MedicineSort = MedicineSort.NONE
    ): Flow<List<Medicine>>

    fun observeMedicine(id: String): Flow<Medicine?>

    fun observeHistory(medicineId: String): Flow<List<History>>

    suspend fun addMedicine(
        name: String,
        stock: Int,
        aisleId: String,
        userEmail: String
    ): Medicine

    /** Applique [delta] au stock, borne a zero, et trace l'operation. */
    suspend fun updateStock(id: String, delta: Int, userEmail: String)

    suspend fun deleteMedicine(id: String, userEmail: String)
}
