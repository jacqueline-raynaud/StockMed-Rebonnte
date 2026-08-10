package com.openclassrooms.rebonnte.ui.aisle

import com.openclassrooms.rebonnte.data.model.StorageLocations
import com.openclassrooms.rebonnte.data.repository.InMemoryAisleRepository
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
            val viewModel = AisleViewModel(InMemoryAisleRepository())
            backgroundScope.launch { viewModel.aisles.collect { } }

            assertEquals(
                StorageLocations.DEFAULTS.map { it.name },
                viewModel.aisles.value.map { it.name }
            )
        }

    @Test
    fun `adding a location appends it with the chosen name`() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = AisleViewModel(InMemoryAisleRepository())
            backgroundScope.launch { viewModel.aisles.collect { } }

            viewModel.addAisle("Stupéfiants")

            assertEquals("Stupéfiants", viewModel.aisles.value.last().name)
        }

    /** Un nom vide ne doit pas creer d'emplacement fantome. */
    @Test
    fun `a blank name creates nothing`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = AisleViewModel(InMemoryAisleRepository())
        backgroundScope.launch { viewModel.aisles.collect { } }
        val before = viewModel.aisles.value.size

        viewModel.addAisle("   ")

        assertEquals(before, viewModel.aisles.value.size)
    }

    @Test
    fun `each location receives a distinct identifier`() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = AisleViewModel(InMemoryAisleRepository())
            backgroundScope.launch { viewModel.aisles.collect { } }

            viewModel.addAisle("Froid négatif")
            viewModel.addAisle("Quarantaine")

            val ids = viewModel.aisles.value.map { it.id }
            assertTrue(ids.toSet().size == ids.size)
        }

    /** L'amorcage est appele a chaque session : il ne doit pas creer de doublon. */
    @Test
    fun `seeding twice does not duplicate the standard locations`() =
        runTest(mainDispatcherRule.dispatcher) {
            val repository = InMemoryAisleRepository()
            val viewModel = AisleViewModel(repository)
            backgroundScope.launch { viewModel.aisles.collect { } }

            repository.ensureDefaultStorageLocations()
            repository.ensureDefaultStorageLocations()

            assertEquals(StorageLocations.DEFAULTS.size, viewModel.aisles.value.size)
        }
}
