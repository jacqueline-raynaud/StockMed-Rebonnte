package com.openclassrooms.rebonnte.data.repository

import com.openclassrooms.rebonnte.data.model.HistoryDto
import com.openclassrooms.rebonnte.data.model.MedicineDto
import kotlinx.coroutines.flow.Flow

/**
 * Acces aux medicaments du stock.
 *
 * Toutes les operations d'ecriture tracent elles-memes l'historique. C'est
 * volontaire : tant qu'un appelant devait penser a journaliser son action, il
 * finissait par l'oublier, d'ou les manques signales par le service qualite.
 * Ici l'oubli est impossible, la trace fait partie de l'operation.
 *
 * L'historique est expose separement de [MedicineDto] pour deux raisons : une
 * suppression doit rester tracee alors que le medicament disparait, et un
 * historique embarque dans le document grossirait sans limite.
 */
interface MedicineRepository {

    fun observeMedicines(
        query: String = "",
        sort: MedicineSort = MedicineSort.NONE
    ): Flow<List<MedicineDto>>

    fun observeMedicine(id: String): Flow<MedicineDto?>

    fun observeHistory(medicineId: String): Flow<List<HistoryDto>>

    suspend fun addMedicine(
        name: String,
        stock: Int,
        aisleId: String,
        userEmail: String
    ): MedicineDto

    /**
     * Corrige le nom et l'emplacement d'un medicament, et trace la correction.
     *
     * **Le stock n'est pas modifiable ici.** Il ne bouge que par [updateStock],
     * qui enregistre un mouvement : corriger un stock par ce chemin
     * contournerait la tracabilite, et le journal ne dirait plus d'ou vient
     * l'ecart.
     *
     * [aisleName] ne sert qu'a rendre l'entree d'historique lisible : « de
     * Stockage froid a Stockage securise » plutot qu'un identifiant de
     * document. Le depot ne connait que les medicaments, c'est donc l'appelant
     * — qui affiche deja la liste — qui resout le libelle.
     *
     * Sans changement reel, rien n'est ecrit : pas de document touche, pas
     * d'entree d'historique.
     */
    suspend fun updateMedicine(
        id: String,
        name: String,
        aisleId: String,
        aisleName: String,
        userEmail: String
    )

    /** Applique [delta] au stock, borne a zero, et trace l'operation. */
    suspend fun updateStock(id: String, delta: Int, userEmail: String)

    suspend fun deleteMedicine(id: String, userEmail: String)
}
