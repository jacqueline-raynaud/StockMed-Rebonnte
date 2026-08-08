package com.openclassrooms.rebonnte.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.openclassrooms.rebonnte.data.repository.AisleRepository
import com.openclassrooms.rebonnte.data.repository.FirebaseUserRepository
import com.openclassrooms.rebonnte.data.repository.FirestoreAisleRepository
import com.openclassrooms.rebonnte.data.repository.FirestoreMedicineRepository
import com.openclassrooms.rebonnte.data.repository.MedicineRepository
import com.openclassrooms.rebonnte.data.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()
}

/**
 * Les implementations sont liees a leurs interfaces ici, et nulle part ailleurs.
 *
 * Le passage des implementations InMemory* aux implementations Firestore n'a
 * demande que la modification de ces trois lignes : ni les ViewModel, ni les
 * ecrans, ni les tests unitaires n'ont bouge. C'est ce que les interfaces
 * achetent.
 *
 * Les InMemory* restent en place et servent de doubles dans les tests.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: FirebaseUserRepository): UserRepository

    @Binds
    @Singleton
    abstract fun bindMedicineRepository(impl: FirestoreMedicineRepository): MedicineRepository

    @Binds
    @Singleton
    abstract fun bindAisleRepository(impl: FirestoreAisleRepository): AisleRepository
}
