package com.openclassrooms.rebonnte.data.model

import androidx.annotation.Keep

/**
 * Une entree d'historique : qui a fait quoi, quand, et avec quel effet.
 *
 * Le service qualite doit pouvoir reconstituer l'evolution d'un stock sans
 * interpretation. D'ou [stockBefore] / [stockAfter] plutot qu'un simple
 * libelle, et [userEmail] plutot qu'un identifiant technique illisible.
 *
 * [date] est un horodatage epoch en millisecondes, et non une chaine : une
 * chaine ne se trie pas et depend de la locale de celui qui l'a ecrite.
 *
 * Voir [MedicineDto] pour la raison d'etre de [Keep].
 */
@Keep
data class HistoryDto(
    val id: String = "",
    val medicineId: String = "",
    val medicineName: String = "",
    val userEmail: String = "",
    val date: Long = 0L,
    val action: HistoryAction = HistoryAction.UPDATE,
    val stockBefore: Int = 0,
    val stockAfter: Int = 0,
    val details: String = ""
)
