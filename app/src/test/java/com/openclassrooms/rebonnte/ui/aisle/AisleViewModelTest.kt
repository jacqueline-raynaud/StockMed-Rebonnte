package com.openclassrooms.rebonnte.ui.aisle

import com.openclassrooms.rebonnte.data.model.StorageLocations
import com.openclassrooms.rebonnte.data.repository.impl.InMemoryAisleRepository
import com.openclassrooms.rebonnte.fake.FakeUserRepository
import com.openclassrooms.rebonnte.util.MainDispatcherRule
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AisleViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    /**
     * Sans emplacement, aucun medicament ne peut etre range : la liste ne doit
     * jamais etre vide au premier lancement.
     */
    @Test
    fun `the list starts with the standard storage locations`() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = AisleViewModel(InMemoryAisleRepository(), FakeUserRepository())
            backgroundScope.launch { viewModel.uiState.collect { } }

            assertEquals(
                StorageLocations.DEFAULTS.map { it.name },
                viewModel.uiState.value.aisles.map { it.name }
            )
        }

    @Test
    fun `adding a location appends it with the chosen name`() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = AisleViewModel(InMemoryAisleRepository(), FakeUserRepository())
            backgroundScope.launch { viewModel.uiState.collect { } }

            viewModel.addAisle("Stupéfiants")

            assertEquals("Stupéfiants", viewModel.uiState.value.aisles.last().name)
        }

    /** Un nom vide ne doit pas creer d'emplacement fantome. */
    @Test
    fun `a blank name creates nothing`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = AisleViewModel(InMemoryAisleRepository(), FakeUserRepository())
        backgroundScope.launch { viewModel.uiState.collect { } }
        val before = viewModel.uiState.value.aisles.size

        viewModel.addAisle("   ")

        assertEquals(before, viewModel.uiState.value.aisles.size)
    }

    @Test
    fun `each location receives a distinct identifier`() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = AisleViewModel(InMemoryAisleRepository(), FakeUserRepository())
            backgroundScope.launch { viewModel.uiState.collect { } }

            viewModel.addAisle("Froid négatif")
            viewModel.addAisle("Quarantaine")

            val ids = viewModel.uiState.value.aisles.map { it.id }
            assertTrue(ids.toSet().size == ids.size)
        }

    /** L'amorcage est appele a chaque session : il ne doit pas creer de doublon. */
    @Test
    fun `seeding twice does not duplicate the standard locations`() =
        runTest(mainDispatcherRule.dispatcher) {
            val repository = InMemoryAisleRepository()
            val viewModel = AisleViewModel(repository, FakeUserRepository())
            backgroundScope.launch { viewModel.uiState.collect { } }

            repository.ensureDefaultStorageLocations()
            repository.ensureDefaultStorageLocations()

            assertEquals(StorageLocations.DEFAULTS.size, viewModel.uiState.value.aisles.size)
        }
}
