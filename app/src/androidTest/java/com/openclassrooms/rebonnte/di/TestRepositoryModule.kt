package com.openclassrooms.rebonnte.di

import com.openclassrooms.rebonnte.data.repository.AisleRepository
import com.openclassrooms.rebonnte.data.repository.InMemoryAisleRepository
import com.openclassrooms.rebonnte.data.repository.InMemoryMedicineRepository
import com.openclassrooms.rebonnte.data.repository.MedicineRepository
import com.openclassrooms.rebonnte.data.repository.UserRepository
import com.openclassrooms.rebonnte.fake.FakeUserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

/**
 * Remplace [RepositoryModule] pendant les tests instrumentes.
 *
 * Aucun test ne doit atteindre Firebase : ce serait lent, dependant du reseau,
 * et ca creerait de vrais comptes.
 */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [RepositoryModule::class]
)
abstract class TestRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: FakeUserRepository): UserRepository

    @Binds
    @Singleton
    abstract fun bindMedicineRepository(impl: InMemoryMedicineRepository): MedicineRepository

    @Binds
    @Singleton
    abstract fun bindAisleRepository(impl: InMemoryAisleRepository): AisleRepository
}
