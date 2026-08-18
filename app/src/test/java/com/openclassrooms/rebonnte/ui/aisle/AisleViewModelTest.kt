package com.openclassrooms.rebonnte.ui.aisle

import com.openclassrooms.rebonnte.R
import com.openclassrooms.rebonnte.data.model.StorageLocations
import com.openclassrooms.rebonnte.data.repository.fake.InMemoryAisleRepository
import com.openclassrooms.rebonnte.fake.FakeUserRepository
import com.openclassrooms.rebonnte.util.MainDispatcherRule
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

    // --- Refus de creation ----------------------------------------------------

    /**
     * Deux « Stockage froid » seraient indiscernables dans la liste deroulante
     * du formulaire de medicament : l'operateur ne saurait pas lequel il choisit.
     */
    @Test
    fun `a duplicate name is refused`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = AisleViewModel(InMemoryAisleRepository(), FakeUserRepository())
        backgroundScope.launch { viewModel.uiState.collect { } }
        val before = viewModel.uiState.value.aisles.size

        viewModel.addAisle("Stockage froid")

        assertEquals(R.string.aisle_error_duplicate, viewModel.newAisleError.value)
        assertEquals(before, viewModel.uiState.value.aisles.size)
    }

    /** La casse et les espaces autour ne font pas un nom different. */
    @Test
    fun `a duplicate is detected regardless of case and spacing`() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = AisleViewModel(InMemoryAisleRepository(), FakeUserRepository())
            backgroundScope.launch { viewModel.uiState.collect { } }

            viewModel.addAisle("   sToCkAgE FrOiD  ")

            assertEquals(R.string.aisle_error_duplicate, viewModel.newAisleError.value)
        }

    /** Un nom fait d'espaces ne cree pas d'emplacement fantome. */
    @Test
    fun `a whitespace only name is refused with a message`() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = AisleViewModel(InMemoryAisleRepository(), FakeUserRepository())
            backgroundScope.launch { viewModel.uiState.collect { } }
            val before = viewModel.uiState.value.aisles.size

            viewModel.addAisle("     ")

            assertEquals(R.string.form_error_name_required, viewModel.newAisleError.value)
            assertEquals(before, viewModel.uiState.value.aisles.size)
        }

    /** Le chemin nominal : creation, pas d'erreur, et la fenetre peut se fermer. */
    @Test
    fun `a new name is accepted and signals the creation`() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = AisleViewModel(InMemoryAisleRepository(), FakeUserRepository())
            backgroundScope.launch { viewModel.uiState.collect { } }

            viewModel.addAisle("  Quarantaine  ")

            assertNull(viewModel.newAisleError.value)
            assertTrue(viewModel.aisleCreated.value)
            // Le nom est enregistre sans les espaces qui l'entouraient.
            assertEquals("Quarantaine", viewModel.uiState.value.aisles.last().name)
        }

    /** L'erreur ne doit pas survivre a la correction de la saisie. */
    @Test
    fun `typing again clears the error`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = AisleViewModel(InMemoryAisleRepository(), FakeUserRepository())
        backgroundScope.launch { viewModel.uiState.collect { } }
        viewModel.addAisle("Stockage froid")

        viewModel.clearNewAisleError()

        assertNull(viewModel.newAisleError.value)
    }
}