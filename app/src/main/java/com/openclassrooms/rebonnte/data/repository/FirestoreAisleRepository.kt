package com.openclassrooms.rebonnte.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.openclassrooms.rebonnte.data.model.Aisle
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
        val aisle = Aisle(id = document.id, name = name)
        document.set(mapOf(FIELD_NAME to name)).await()
        return aisle
    }

    private companion object {
        const val COLLECTION_AISLES = "aisles"
        const val FIELD_NAME = "name"
    }
}
