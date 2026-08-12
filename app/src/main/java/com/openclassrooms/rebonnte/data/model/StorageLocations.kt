package com.openclassrooms.rebonnte.data.model

/**
 * Emplacements de stockage presents dans toute pharmacie.
 *
 * Ce sont des **donnees** et non une enumeration : un etablissement peut avoir
 * besoin d'un stockage stupefiants distinct du securise, ou d'un froid negatif.
 * Ajouter un emplacement ne doit pas demander de recompiler.
 *
 * Les identifiants sont fixes et non generes. C'est ce qui rend l'amorcage
 * idempotent : deux operateurs qui ouvrent l'application en meme temps sur une
 * base vide n'obtiennent pas six emplacements en double.
 */
object StorageLocations {

    val DEFAULTS = listOf(
        AisleDto(id = "standard", name = "Stockage standard"),
        AisleDto(id = "cold", name = "Stockage froid"),
        AisleDto(id = "secured", name = "Stockage securise")
    )
}
