package com.openclassrooms.rebonnte.data.repository.impl

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.openclassrooms.rebonnte.data.model.HistoryAction
import com.openclassrooms.rebonnte.data.model.HistoryDto
import com.openclassrooms.rebonnte.data.model.MedicineDto
import com.openclassrooms.rebonnte.data.repository.MedicineRepository
import com.openclassrooms.rebonnte.data.repository.MedicineSort
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

/**
 * Implementation Firestore de [MedicineRepository].
 *
 * L'historique vit dans une collection racine et non dans une sous-collection
 * des medicaments : la trace d'une suppression doit survivre au document
 * supprime, et le service qualite doit pouvoir lire le journal complet sans
 * parcourir chaque medicament.
 */
@Singleton
class MedicineRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : MedicineRepository {

    private val medicines get() = firestore.collection(COLLECTION_MEDICINES)
    private val history get() = firestore.collection(COLLECTION_HISTORY)

    override fun observeMedicines(query: String, sort: MedicineSort): Flow<List<MedicineDto>> {
        val needle = query.trim().lowercase()

        val request: Query = when {
            // Firestore ne sait pas faire un « contient » : seul un prefixe est
            // possible, via un intervalle sur le champ en minuscules.
            // Un vrai moteur de recherche demanderait un service dedie.
            needle.isNotEmpty() -> medicines
                .orderBy(FIELD_NAME_LOWERCASE)
                .startAt(needle)
                .endAt(needle + PREFIX_UPPER_BOUND)

            // Le tri par nom porte sur le champ en minuscules : sur le champ
            // brut, Firestore placerait « Zovirax » avant « aspirine ».
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
                    close(error)
                    return@addSnapshotListener
                }
                val result = snapshot?.documents.orEmpty().mapNotNull { it.toMedicine() }
                // Firestore impose que le premier orderBy porte sur le champ de
                // l'intervalle : quand une recherche est active, le tri demande
                // s'applique donc sur le resultat deja restreint.
                trySend(if (needle.isEmpty()) result else result.sortedBy(sort))
            }
            awaitClose { registration.remove() }
        }
    }

    override fun observeMedicine(id: String): Flow<MedicineDto?> = callbackFlow {
        val registration = medicines.document(id).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
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
                    close(error)
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

        // Le medicament et sa trace de creation partent dans le meme lot :
        // aucun des deux ne peut exister sans l'autre.
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
        }.commit().await()

        return medicine
    }

    /**
     * Transaction et non deux ecritures successives, pour deux raisons.
     *
     * D'abord l'atomicite : un stock modifie sans trace serait exactement
     * l'incoherence signalee par le service qualite.
     *
     * Ensuite la concurrence : sur des telephones partages, deux operateurs
     * peuvent retirer une boite au meme instant. Lire puis ecrire sans
     * transaction en perdrait une ; Firestore relit et reessaie.
     */
    override suspend fun updateStock(id: String, delta: Int, userEmail: String) {
        firestore.runTransaction { transaction ->
            val reference = medicines.document(id)
            // Toutes les lectures avant toutes les ecritures : Firestore l'impose.
            val medicine = transaction.get(reference).toMedicine()
                ?: return@runTransaction null

            val stockAfter = (medicine.stock + delta).coerceAtLeast(0)
            if (stockAfter == medicine.stock) return@runTransaction null

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
            null
        }.await()
    }

    override suspend fun deleteMedicine(id: String, userEmail: String) {
        firestore.runTransaction { transaction ->
            val reference = medicines.document(id)
            val medicine = transaction.get(reference).toMedicine() ?: return@runTransaction null

            transaction.delete(reference)
            // La trace est ecrite dans la meme transaction que la suppression,
            // et survit au document efface.
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
            null
        }.await()
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
        const val COLLECTION_MEDICINES = "medicines"
        const val COLLECTION_HISTORY = "history"

        const val FIELD_NAME = "name"
        const val FIELD_NAME_LOWERCASE = "nameLowercase"
        const val FIELD_STOCK = "stock"
        const val FIELD_MEDICINE_ID = "medicineId"
        const val FIELD_DATE = "date"

        /** Dernier point de code utilisable : borne haute d'une recherche par prefixe. */
        const val PREFIX_UPPER_BOUND = '\uf8ff'
    }
}

/**
 * `nameLowercase` n'appartient pas au modele : c'est un champ technique, ajoute
 * uniquement pour rendre la recherche par prefixe insensible a la casse.
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
