package com.openclassrooms.rebonnte.data.repository.impl

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.openclassrooms.rebonnte.data.model.AisleDto
import com.openclassrooms.rebonnte.data.model.StorageLocations
import com.openclassrooms.rebonnte.data.repository.AisleRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

@Singleton
class AisleRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : AisleRepository {

    private val aisles get() = firestore.collection(COLLECTION_AISLES)

    /**
     * Observes the collection of aisles .
     * Povides a real-time stream of the current data.
     * The resulting list is ordered alphabetically by the aisle name.
     *
     * @return A [Flow] emitting a list of [AisleDto] objects whenever the remote data changes.
     */
    override fun observeAisles(): Flow<List<AisleDto>> = callbackFlow {
        val registration = aisles.orderBy(FIELD_NAME).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error.toStockException())
                return@addSnapshotListener
            }
            trySend(
                snapshot?.documents.orEmpty().mapNotNull { document ->
                    document.toObject(AisleDto::class.java)?.copy(id = document.id)
                }
            )
        }
        awaitClose { registration.remove() }
    }

    /**
     * Observes a specific aisle
     * Provides a real-time stream of its data.
     *
     * @param id The ID of the aisle to observe.
     * @return A [Flow] emitting the [AisleDto] object for the specified aisle, or `null` if not found.
     */
    override fun observeAisle(id: String): Flow<AisleDto?> = callbackFlow {
        val registration = aisles.document(id).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error.toStockException())
                return@addSnapshotListener
            }
            trySend(snapshot?.toObject(AisleDto::class.java)?.copy(id = snapshot.id))
        }
        awaitClose { registration.remove() }
    }

    override suspend fun addAisle(name: String): AisleDto {
        val document = aisles.document()
        val aisle = AisleDto(id = document.id, name = name.trim())
        firestoreWrite { document.set(mapOf(FIELD_NAME to aisle.name)).await() }
        return aisle
    }

    /**
     * prevents non-existent locations from appearing when the application starts
     * and avoids duplicate entries created by two users
     */
    override suspend fun ensureDefaultStorageLocations() {
        val batch = firestore.batch()
        StorageLocations.DEFAULTS.forEach { location ->
            batch.set(
                aisles.document(location.id),
                mapOf(FIELD_NAME to location.name),
                SetOptions.merge()
            )
        }
        firestoreWrite { batch.commit().await() }
    }

    private companion object {
        const val COLLECTION_AISLES = "aisles"
        const val FIELD_NAME = "name"
    }
}
