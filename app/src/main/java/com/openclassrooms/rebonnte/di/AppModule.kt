package com.openclassrooms.rebonnte.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.openclassrooms.rebonnte.data.network.ConnectivityNetworkMonitor
import com.openclassrooms.rebonnte.data.network.NetworkMonitor
import com.openclassrooms.rebonnte.data.preferences.SharedPreferencesThemeRepository
import com.openclassrooms.rebonnte.data.preferences.ThemeRepository
import com.openclassrooms.rebonnte.data.repository.AisleRepository
import com.openclassrooms.rebonnte.data.repository.impl.UserRepositoryImpl
import com.openclassrooms.rebonnte.data.repository.impl.AisleRepositoryImpl
import com.openclassrooms.rebonnte.data.repository.impl.MedicineRepositoryImpl
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
 * interface-implementation link
 * The InMemory* classes serve as doubles in tests.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindNetworkMonitor(impl: ConnectivityNetworkMonitor): NetworkMonitor

    @Binds
    @Singleton
    abstract fun bindThemeRepository(impl: SharedPreferencesThemeRepository): ThemeRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    @Singleton
    abstract fun bindMedicineRepository(impl: MedicineRepositoryImpl): MedicineRepository

    @Binds
    @Singleton
    abstract fun bindAisleRepository(impl: AisleRepositoryImpl): AisleRepository
}
