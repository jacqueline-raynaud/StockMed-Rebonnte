package com.openclassrooms.rebonnte.data.repository

import com.openclassrooms.rebonnte.data.model.AisleDto
import kotlinx.coroutines.flow.Flow

interface AisleRepository {

    fun observeAisles(): Flow<List<AisleDto>>

    fun observeAisle(id: String): Flow<AisleDto?>

    suspend fun addAisle(name: String): AisleDto

    suspend fun ensureDefaultStorageLocations()
}
