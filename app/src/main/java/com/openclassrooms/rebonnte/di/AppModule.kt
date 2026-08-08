package com.openclassrooms.rebonnte.di

import com.google.firebase.auth.FirebaseAuth
import com.openclassrooms.rebonnte.data.repository.AisleRepository
import com.openclassrooms.rebonnte.data.repository.FirebaseUserRepository
import com.openclassrooms.rebonnte.data.repository.InMemoryAisleRepository
import com.openclassrooms.rebonnte.data.repository.InMemoryMedicineRepository
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
}

/**
 * Les implementations sont liees a leurs interfaces ici, et nulle part ailleurs.
 *
 * Le @Singleton est indispensable sur les depots en memoire : sans lui, chaque
 * ViewModel recevrait sa propre instance et l'ecran des rayons ne verrait pas
 * les medicaments ajoutes depuis l'autre onglet.
 *
 * Les implementations InMemory* seront remplacees par leurs equivalents
 * Firestore sans toucher aux ViewModel : c'est tout l'interet des interfaces.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: FirebaseUserRepository): UserRepository

    @Binds
    @Singleton
    abstract fun bindMedicineRepository(impl: InMemoryMedicineRepository): MedicineRepository

    @Binds
    @Singleton
    abstract fun bindAisleRepository(impl: InMemoryAisleRepository): AisleRepository
}
