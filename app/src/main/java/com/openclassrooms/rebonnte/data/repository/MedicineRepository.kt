package com.openclassrooms.rebonnte.data.repository

import com.openclassrooms.rebonnte.data.model.HistoryDto
import com.openclassrooms.rebonnte.data.model.MedicineDto
import kotlinx.coroutines.flow.Flow

interface MedicineRepository {

    fun observeMedicines(
        query: String = "",
        sort: MedicineSort = MedicineSort.NONE
    ): Flow<List<MedicineDto>>

    /**
     * Les medicaments d'un seul emplacement, filtres **par la base**.
     *
     * L'ecran d'un emplacement telechargeait auparavant tout le stock pour n'en
     * afficher qu'une partie. Sur deux cents references dont douze au froid,
     * cent quatre-vingt-huit documents descendaient pour rien — du reseau, de la
     * batterie, et des lectures facturees.
     *
     * Aucun `orderBy` n'accompagne le filtre : ce serait un index composite a
     * declarer. Le tri se fait en memoire, sur un ensemble par construction
     * restreint.
     */
    fun observeMedicinesInAisle(aisleId: String): Flow<List<MedicineDto>>

    fun observeMedicine(id: String): Flow<MedicineDto?>

    /**
     * Les [limit] entrees les plus recentes.
     *
     * L'historique d'un medicament tres manipule compte des centaines de
     * lignes ; l'operateur en lit trois. Sans borne, ouvrir une fiche les
     * telechargeait toutes.
     */
    fun observeHistory(medicineId: String, limit: Int): Flow<List<HistoryDto>>

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
