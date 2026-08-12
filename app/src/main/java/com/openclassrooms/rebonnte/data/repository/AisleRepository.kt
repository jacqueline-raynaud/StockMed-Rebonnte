package com.openclassrooms.rebonnte.data.repository

import com.openclassrooms.rebonnte.data.model.AisleDto
import kotlinx.coroutines.flow.Flow

/** Acces aux emplacements de stockage. */
interface AisleRepository {

    fun observeAisles(): Flow<List<AisleDto>>

    fun observeAisle(id: String): Flow<AisleDto?>

    suspend fun addAisle(name: String): AisleDto

    /**
     * Cree les emplacements standards s'ils n'existent pas encore.
     *
     * Sans amorcage, la collection est vide au premier lancement et aucun
     * medicament ne peut etre cree : il n'y a nulle part ou le ranger.
     *
     * L'operation doit etre idempotente — elle est appelee a chaque ouverture
     * de session.
     */
    suspend fun ensureDefaultStorageLocations()
}
