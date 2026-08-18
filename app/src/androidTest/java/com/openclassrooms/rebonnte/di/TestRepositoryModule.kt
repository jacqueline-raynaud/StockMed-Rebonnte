package com.openclassrooms.rebonnte.di

import com.openclassrooms.rebonnte.data.network.NetworkMonitor
import com.openclassrooms.rebonnte.data.preferences.ThemeRepository
import com.openclassrooms.rebonnte.data.repository.AisleRepository
import com.openclassrooms.rebonnte.data.repository.MedicineRepository
import com.openclassrooms.rebonnte.data.repository.UserRepository
import com.openclassrooms.rebonnte.fake.FakeAisleRepository
import com.openclassrooms.rebonnte.fake.FakeMedicineRepository
import com.openclassrooms.rebonnte.fake.FakeNetworkMonitor
import com.openclassrooms.rebonnte.fake.FakeThemeRepository
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
interface TestRepositoryModule {

    @Binds
    @Singleton
    fun bindNetworkMonitor(impl: FakeNetworkMonitor): NetworkMonitor

    @Binds
    @Singleton
    fun bindThemeRepository(impl: FakeThemeRepository): ThemeRepository

    @Binds
    @Singleton
    fun bindUserRepository(impl: FakeUserRepository): UserRepository

    @Binds
    @Singleton
    fun bindMedicineRepository(impl: FakeMedicineRepository): MedicineRepository

    @Binds
    @Singleton
    fun bindAisleRepository(impl: FakeAisleRepository): AisleRepository
}
