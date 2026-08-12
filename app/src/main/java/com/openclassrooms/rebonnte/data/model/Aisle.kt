package com.openclassrooms.rebonnte.data.model

import androidx.annotation.Keep

/**
 * Un rayon du stock.
 *
 * Voir [Medicine] pour la raison d'etre de l'identifiant, et pour celle de
 * [Keep].
 */
@Keep
data class Aisle(
    val id: String = "",
    val name: String = ""
)
