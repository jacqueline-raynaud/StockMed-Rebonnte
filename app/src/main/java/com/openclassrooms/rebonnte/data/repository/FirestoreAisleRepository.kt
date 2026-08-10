package com.openclassrooms.rebonnte.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.openclassrooms.rebonnte.data.model.Aisle
import com.openclassrooms.rebonnte.data.model.StorageLocations
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreAisleRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : AisleRepository {

    private val aisles get() = firestore.collection(COLLECTION_AISLES)

    override fun observeAisles(): Flow<List<Aisle>> = callbackFlow {
        val registration = aisles.orderBy(FIELD_NAME).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(
                snapshot?.documents.orEmpty().mapNotNull { document ->
                    document.toObject(Aisle::class.java)?.copy(id = document.id)
                }
            )
        }
        awaitClose { registration.remove() }
    }

    override fun observeAisle(id: String): Flow<Aisle?> = callbackFlow {
        val registration = aisles.document(id).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snapshot?.toObject(Aisle::class.java)?.copy(id = snapshot.id))
        }
        awaitClose { registration.remove() }
    }

    override suspend fun addAisle(name: String): Aisle {
        val document = aisles.document()
        val aisle = Aisle(id = document.id, name = name.trim())
        document.set(mapOf(FIELD_NAME to aisle.name)).await()
        return aisle
    }

    /**
     * Identifiants de document fixes plutot que generes : `set` devient
     * idempotent. Deux appareils qui amorcent la base au meme instant
     * aboutissent au meme resultat, la ou des identifiants aleatoires auraient
     * cree des doublons.
     *
     * `merge` preserve un libelle qui aurait ete personnalise depuis la console.
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
        batch.commit().await()
    }

    private companion object {
        const val COLLECTION_AISLES = "aisles"
        const val FIELD_NAME = "name"
    }
}
