package com.openclassrooms.rebonnte.data.model

/**
 * This involves data rather than a simple list.
 * The identifiers are fixed, preventing duplicates when opening the application simultaneously
 * on an empty database.
 */
object StorageLocations {

    val DEFAULTS = listOf(
        AisleDto(id = "standard", name = "Stockage standard"),
        AisleDto(id = "cold", name = "Stockage froid"),
        AisleDto(id = "secured", name = "Stockage securisé")
    )
}
