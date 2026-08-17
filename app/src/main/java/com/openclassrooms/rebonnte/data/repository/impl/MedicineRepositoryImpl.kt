package com.openclassrooms.rebonnte.data.repository.impl

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.openclassrooms.rebonnte.data.model.HistoryAction
import com.openclassrooms.rebonnte.data.model.HistoryDto
import com.openclassrooms.rebonnte.data.model.MedicineDto
import com.openclassrooms.rebonnte.data.repository.MedicineRepository
import com.openclassrooms.rebonnte.data.repository.MedicineSort
import com.openclassrooms.rebonnte.data.repository.StockErrorReason
import com.openclassrooms.rebonnte.data.repository.StockException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await


@Singleton
class MedicineRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : MedicineRepository {

    private val medicines get() = firestore.collection(COLLECTION_MEDICINES)
    private val history get() = firestore.collection(COLLECTION_HISTORY)

    override fun observeMedicines(query: String, sort: MedicineSort): Flow<List<MedicineDto>> {
        val needle = query.trim().lowercase()

        val request: Query = when {
            // Search by text prefix. Firestore does not have a "content" operator..
            needle.isNotEmpty() -> medicines
                .orderBy(FIELD_NAME_LOWERCASE)
                .startAt(needle)
                .endAt(needle + PREFIX_UPPER_BOUND)

            // Firefox places uppercase letters before lowercase letters; the sorting is based on the lowercase content.
            sort == MedicineSort.NAME_ASC ->
                medicines.orderBy(FIELD_NAME_LOWERCASE, Query.Direction.ASCENDING)

            sort == MedicineSort.NAME_DESC ->
                medicines.orderBy(FIELD_NAME_LOWERCASE, Query.Direction.DESCENDING)

            sort == MedicineSort.STOCK_ASC ->
                medicines.orderBy(FIELD_STOCK, Query.Direction.ASCENDING)

            sort == MedicineSort.STOCK_DESC ->
                medicines.orderBy(FIELD_STOCK, Query.Direction.DESCENDING)

            else -> medicines
        }

        return callbackFlow {
            val registration = request.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error.toStockException())
                    return@addSnapshotListener
                }
                val result = snapshot?.documents.orEmpty().mapNotNull { it.toMedicine() }
                trySend(if (needle.isEmpty()) result else result.sortedBy(sort))
            }
            awaitClose { registration.remove() }
        }
    }

    override fun observeMedicine(id: String): Flow<MedicineDto?> = callbackFlow {
        val registration = medicines.document(id).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error.toStockException())
                return@addSnapshotListener
            }
            trySend(snapshot?.toMedicine())
        }
        awaitClose { registration.remove() }
    }

    override fun observeHistory(medicineId: String): Flow<List<HistoryDto>> = callbackFlow {
        val registration = history
            .whereEqualTo(FIELD_MEDICINE_ID, medicineId)
            .orderBy(FIELD_DATE, Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error.toStockException())
                    return@addSnapshotListener
                }
                trySend(
                    snapshot?.documents.orEmpty().mapNotNull { document ->
                        document.toObject(HistoryDto::class.java)?.copy(id = document.id)
                    }
                )
            }
        awaitClose { registration.remove() }
    }

    override suspend fun addMedicine(
        name: String,
        stock: Int,
        aisleId: String,
        userEmail: String
    ): MedicineDto {
        val document = medicines.document()
        val medicine = MedicineDto(id = document.id, name = name, stock = stock, aisleId = aisleId)

        // medicine and history
        firestore.batch().apply {
            set(document, medicine.toDocument())
            set(
                history.document(),
                historyDocument(
                    medicine = medicine,
                    action = HistoryAction.CREATE,
                    stockBefore = 0,
                    stockAfter = stock,
                    userEmail = userEmail,
                    details = "Medicament cree"
                )
            )
        }.let { batch -> firestoreWrite { batch.commit().await() } }

        return medicine
    }

    /**
     * Correction d'une fiche : le nom, l'emplacement, jamais le stock.
     *
     * En transaction pour la meme raison qu'un mouvement : la fiche et sa trace
     * doivent changer ensemble ou pas du tout. Et la relecture protege d'une
     * correction concurrente — deux operateurs qui renomment le meme medicament
     * au meme instant ne doivent pas produire une trace qui ne correspond a
     * aucun etat reel.
     *
     * `nameLowercase` est mis a jour en meme temps que `name` : c'est lui qui
     * porte le tri et la recherche. Oublier ce champ ferait disparaitre le
     * medicament renomme des resultats, sans erreur visible.
     */
    override suspend fun updateMedicine(
        id: String,
        name: String,
        aisleId: String,
        aisleName: String,
        userEmail: String
    ) {
        val newName = name.trim()
        if (newName.isEmpty()) return

        firestore.runTransaction { transaction ->
            val reference = medicines.document(id)
            val medicine = transaction.get(reference).toMedicine()
                ?: return@runTransaction UPDATED

            val changes = describeChanges(medicine, newName, aisleId, aisleName)
                ?: return@runTransaction UPDATED

            transaction.update(
                reference,
                mapOf(
                    FIELD_NAME to newName,
                    FIELD_NAME_LOWERCASE to newName.lowercase(),
                    FIELD_AISLE_ID to aisleId
                )
            )
            transaction.set(
                history.document(),
                historyDocument(
                    medicine = medicine.copy(name = newName),
                    action = HistoryAction.UPDATE,
                    // Le stock ne bouge pas : avant et apres portent la meme
                    // valeur, et l'entree se distingue par son action.
                    stockBefore = medicine.stock,
                    stockAfter = medicine.stock,
                    userEmail = userEmail,
                    details = changes
                )
            )
            UPDATED
        }.let { task -> firestoreTransaction { task.await() } }
    }

    /**
     * The update is performed via a transaction (read and write)
     * to avoid a conflict if two users operate on the same product.
     */
    override suspend fun updateStock(id: String, delta: Int, userEmail: String) {
        val outcome = firestore.runTransaction { transaction ->
            val reference = medicines.document(id)
            // Toutes les lectures avant toutes les ecritures : Firestore l'impose.
            val medicine = transaction.get(reference).toMedicine()
                ?: return@runTransaction StockChange.Applied

            val stockAfter = medicine.stock + delta

            // checking the actual stock quantity against the quantity requested by the user
            if (stockAfter < 0) {
                return@runTransaction StockChange.Insufficient(medicine.stock)
            }
            if (stockAfter == medicine.stock) return@runTransaction StockChange.Applied

            transaction.update(reference, FIELD_STOCK, stockAfter)
            transaction.set(
                history.document(),
                historyDocument(
                    medicine = medicine,
                    action = HistoryAction.STOCK_CHANGE,
                    stockBefore = medicine.stock,
                    stockAfter = stockAfter,
                    userEmail = userEmail,
                    details = "Stock modifie de ${medicine.stock} a $stockAfter"
                )
            )
            StockChange.Applied
        }.let { task -> firestoreTransaction { task.await() } }

        if (outcome is StockChange.Insufficient) {
            throw StockException(
                reason = StockErrorReason.INSUFFICIENT_STOCK,
                available = outcome.available
            )
        }
    }

    /** Result transaction of update stock. */
    private sealed interface StockChange {
        data object Applied : StockChange
        data class Insufficient(val available: Int) : StockChange
    }


    override suspend fun deleteMedicine(id: String, userEmail: String) {
        firestore.runTransaction { transaction ->
            val reference = medicines.document(id)
            val medicine = transaction.get(reference).toMedicine()
                ?: return@runTransaction DELETED

            transaction.delete(reference)
            transaction.set(
                history.document(),
                historyDocument(
                    medicine = medicine,
                    action = HistoryAction.DELETE,
                    stockBefore = medicine.stock,
                    stockAfter = 0,
                    userEmail = userEmail,
                    details = "Medicament supprime"
                )
            )
            DELETED
        }.let { task -> firestoreTransaction { task.await() } }
    }

    private fun historyDocument(
        medicine: MedicineDto,
        action: HistoryAction,
        stockBefore: Int,
        stockAfter: Int,
        userEmail: String,
        details: String
    ): Map<String, Any> = mapOf(
        FIELD_MEDICINE_ID to medicine.id,
        "medicineName" to medicine.name,
        "userEmail" to userEmail,
        FIELD_DATE to System.currentTimeMillis(),
        "action" to action.name,
        "stockBefore" to stockBefore,
        "stockAfter" to stockAfter,
        "details" to details
    )

    private companion object {
        //transaction return value
        const val DELETED = true

        /** Meme raison que [DELETED] : la transaction ne doit pas rendre null. */
        const val UPDATED = true

        const val COLLECTION_MEDICINES = "medicines"
        const val COLLECTION_HISTORY = "history"

        const val FIELD_NAME = "name"
        const val FIELD_NAME_LOWERCASE = "nameLowercase"
        const val FIELD_AISLE_ID = "aisleId"
        const val FIELD_STOCK = "stock"
        const val FIELD_MEDICINE_ID = "medicineId"
        const val FIELD_DATE = "date"

        const val PREFIX_UPPER_BOUND = '\uf8ff'
    }
}

/**
 * name Lowercase` is not part of the model;
 * it is a technical field added solely to make prefix searches case-insensitive.
 */
private fun MedicineDto.toDocument(): Map<String, Any> = mapOf(
    "name" to name,
    "nameLowercase" to name.lowercase(),
    "stock" to stock,
    "aisleId" to aisleId
)

private fun DocumentSnapshot.toMedicine(): MedicineDto? =
    toObject(MedicineDto::class.java)?.copy(id = id)

private fun List<MedicineDto>.sortedBy(sort: MedicineSort): List<MedicineDto> = when (sort) {
    MedicineSort.NONE -> this
    MedicineSort.NAME_ASC -> sortedBy { it.name.lowercase() }
    MedicineSort.NAME_DESC -> sortedByDescending { it.name.lowercase() }
    MedicineSort.STOCK_ASC -> sortedBy { it.stock }
    MedicineSort.STOCK_DESC -> sortedByDescending { it.stock }
}
