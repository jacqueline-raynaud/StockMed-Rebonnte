package com.openclassrooms.rebonnte.fake

import com.openclassrooms.rebonnte.data.model.AisleDto
import com.openclassrooms.rebonnte.data.model.StorageLocations
import com.openclassrooms.rebonnte.data.repository.AisleRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * A working, in-memory implementation of [AisleRepository], seeded with the
 * standard storage locations.
 *
 * `@Singleton` matters: without it every ViewModel would receive its own
 * instance, and a location created on one screen would be invisible on the
 * next. Duplicated in `androidTest`, like [FakeMedicineRepository].
 */
@Singleton
class FakeAisleRepository @Inject constructor() : AisleRepository {

    private val aisles = MutableStateFlow(StorageLocations.DEFAULTS)

    override fun observeAisles(): Flow<List<AisleDto>> = aisles

    override fun observeAisle(id: String): Flow<AisleDto?> =
        aisles.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun addAisle(name: String): AisleDto {
        val aisle = AisleDto(id = UUID.randomUUID().toString(), name = name.trim())
        aisles.value = aisles.value + aisle
        return aisle
    }

    override suspend fun ensureDefaultStorageLocations() {
        val existing = aisles.value.map { it.id }.toSet()
        aisles.value = aisles.value + StorageLocations.DEFAULTS.filterNot { it.id in existing }
    }
}
