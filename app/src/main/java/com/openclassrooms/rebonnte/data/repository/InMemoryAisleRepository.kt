package com.openclassrooms.rebonnte.data.repository

import com.openclassrooms.rebonnte.data.model.Aisle
import com.openclassrooms.rebonnte.data.model.StorageLocations
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Pendant de [InMemoryMedicineRepository] pour les emplacements de stockage. */
@Singleton
class InMemoryAisleRepository @Inject constructor() : AisleRepository {

    private val aisles = MutableStateFlow(StorageLocations.DEFAULTS)

    override fun observeAisles(): Flow<List<Aisle>> = aisles

    override fun observeAisle(id: String): Flow<Aisle?> =
        aisles.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun addAisle(name: String): Aisle {
        val aisle = Aisle(id = UUID.randomUUID().toString(), name = name.trim())
        aisles.value = aisles.value + aisle
        return aisle
    }

    override suspend fun ensureDefaultStorageLocations() {
        // Les emplacements standards sont deja presents a la construction ;
        // on ne recree que ceux qu'un test aurait retires.
        val existing = aisles.value.map { it.id }.toSet()
        aisles.value = aisles.value + StorageLocations.DEFAULTS.filterNot { it.id in existing }
    }
}
