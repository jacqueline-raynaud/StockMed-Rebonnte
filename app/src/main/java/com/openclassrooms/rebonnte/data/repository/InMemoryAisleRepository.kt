package com.openclassrooms.rebonnte.data.repository

import com.openclassrooms.rebonnte.data.model.Aisle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.util.UUID

/** Pendant de [InMemoryMedicineRepository] pour les rayons. */
class InMemoryAisleRepository : AisleRepository {

    private val aisles = MutableStateFlow(
        listOf(Aisle(id = UUID.randomUUID().toString(), name = "Main Aisle"))
    )

    override fun observeAisles(): Flow<List<Aisle>> = aisles

    override fun observeAisle(id: String): Flow<Aisle?> =
        aisles.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun addAisle(name: String): Aisle {
        val aisle = Aisle(id = UUID.randomUUID().toString(), name = name)
        aisles.value = aisles.value + aisle
        return aisle
    }
}
