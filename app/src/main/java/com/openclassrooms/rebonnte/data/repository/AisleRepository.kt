package com.openclassrooms.rebonnte.data.repository

import com.openclassrooms.rebonnte.data.model.Aisle
import kotlinx.coroutines.flow.Flow

/** Acces aux rayons du stock. */
interface AisleRepository {

    fun observeAisles(): Flow<List<Aisle>>

    fun observeAisle(id: String): Flow<Aisle?>

    suspend fun addAisle(name: String): Aisle
}
