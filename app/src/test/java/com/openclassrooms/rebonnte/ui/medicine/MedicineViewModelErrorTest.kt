package com.openclassrooms.rebonnte.ui.medicine

import com.openclassrooms.rebonnte.R
import com.openclassrooms.rebonnte.data.repository.StockErrorReason
import com.openclassrooms.rebonnte.data.repository.fake.InMemoryAisleRepository
import com.openclassrooms.rebonnte.data.repository.fake.InMemoryMedicineRepository
import com.openclassrooms.rebonnte.fake.FailingMedicineRepository
import com.openclassrooms.rebonnte.fake.FakeUserRepository
import com.openclassrooms.rebonnte.util.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

/**
 * T-24 : une panne ne doit plus tuer l'application.
 *
 * Avant, l'exception remontait du `callbackFlow` jusqu'au collecteur et le
 * processus mourait. Ces tests echoueraient sur cette version — ils ne
 * verifieraient meme pas une assertion, ils planteraient.
 */
class MedicineViewModelErrorTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel(reason: StockErrorReason) = MedicineViewModel(
        FailingMedicineRepository(reason),
        FakeUserRepository(),
        InMemoryAisleRepository()
    )

    @Test
    fun `a failed read exposes a message instead of crashing`() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = viewModel(StockErrorReason.NETWORK)
            backgroundScope.launch { viewModel.uiState.collect { } }

            val state = viewModel.uiState.first { !it.isLoading }

            assertEquals(R.string.error_network, state.errorMessage)
            assertFalse(state.isLoading)
        }

    /** Chaque raison a son libelle : le message doit dire quoi faire. */
    @Test
    fun `a permission failure is reported as such`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel(StockErrorReason.PERMISSION)
        backgroundScope.launch { viewModel.uiState.collect { } }

        val state = viewModel.uiState.first { !it.isLoading }

        assertEquals(R.string.error_permission, state.errorMessage)
    }

    @Test
    fun `the detail screen reports a failed read`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel(StockErrorReason.UNAVAILABLE)

        val state = viewModel.observeDetail("any-id").first { !it.isLoading }

        assertEquals(R.string.error_unavailable, state.errorMessage)
        assertNull(state.medicine)
    }

    /**
     * Le cas le plus grave : un mouvement de stock hors reseau. L'exception
     * partait dans le scope du ViewModel, ou personne ne l'attrapait.
     */
    @Test
    fun `a failed stock movement is reported without crashing`() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = viewModel(StockErrorReason.NETWORK)

            viewModel.updateStock("any-id", delta = -5)

            assertEquals(R.string.error_network, viewModel.actionError.value?.res)
        }

    @Test
    fun `a failed deletion is reported without crashing`() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = viewModel(StockErrorReason.PERMISSION)

            viewModel.deleteMedicine("any-id")

            assertEquals(R.string.error_permission, viewModel.actionError.value?.res)
        }

    /** Sans cet acquittement, le meme message reviendrait a chaque recomposition. */
    @Test
    fun `acknowledging the message clears it`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel(StockErrorReason.NETWORK)
        viewModel.updateStock("any-id", delta = -5)

        viewModel.actionErrorShown()

        assertNull(viewModel.actionError.value?.res)
    }

    /** Le chemin nominal ne doit evidemment rien signaler. */
    @Test
    fun `a successful read reports no error`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = MedicineViewModel(
            InMemoryMedicineRepository(),
            FakeUserRepository(),
            InMemoryAisleRepository()
        )
        backgroundScope.launch { viewModel.uiState.collect { } }

        val state = viewModel.uiState.first { !it.isLoading }

        assertNull(state.errorMessage)
        assertNull(viewModel.actionError.value?.res)
    }

    /**
     * T-21 : le refus remonte jusqu'a l'ecran, avec la quantite disponible.
     *
     * C'est le chiffre qui manquait a l'operateur : sans lui, le message dirait
     * seulement que le retrait a echoue, sans dire de combien il s'est trompe.
     */
    @Test
    fun `a removal larger than the stock is reported with the remaining quantity`() =
        runTest(mainDispatcherRule.dispatcher) {
            val repository = InMemoryMedicineRepository()
            val viewModel = MedicineViewModel(
                repository,
                FakeUserRepository(),
                InMemoryAisleRepository()
            )
            val medicine = repository.addMedicine("Doliprane", 10, "aisle-1", "")

            viewModel.updateStock(medicine.id, delta = -50)

            assertEquals(R.string.error_insufficient_stock, viewModel.actionError.value?.res)
            assertEquals(listOf(10), viewModel.actionError.value?.args)
            // Le stock n'a pas bouge : l'operation a bien ete refusee.
            assertEquals(10, repository.observeMedicine(medicine.id).first()!!.stock)
        }

    /** Un mouvement enregistre produit une confirmation, et une seule. */
    @Test
    fun `a successful movement is confirmed with its quantity`() =
        runTest(mainDispatcherRule.dispatcher) {
            val repository = InMemoryMedicineRepository()
            val viewModel = MedicineViewModel(
                repository,
                FakeUserRepository(),
                InMemoryAisleRepository()
            )
            val medicine = repository.addMedicine("Doliprane", 60, "aisle-1", "")

            viewModel.updateStock(medicine.id, delta = -50)

            assertEquals(R.string.detail_units_removed, viewModel.movementConfirmed.value?.res)
            assertEquals(listOf(50), viewModel.movementConfirmed.value?.args)
            assertNull(viewModel.actionError.value)
        }

    /**
     * Le point de depart de la correction : un retrait refuse ne doit produire
     * **aucune** confirmation. L'ecran en affichait une avant meme de savoir si
     * l'operation avait abouti.
     */
    @Test
    fun `a refused movement produces no confirmation`() =
        runTest(mainDispatcherRule.dispatcher) {
            val repository = InMemoryMedicineRepository()
            val viewModel = MedicineViewModel(
                repository,
                FakeUserRepository(),
                InMemoryAisleRepository()
            )
            val medicine = repository.addMedicine("Doliprane", 10, "aisle-1", "")

            viewModel.updateStock(medicine.id, delta = -50)

            assertNull(viewModel.movementConfirmed.value)
            assertEquals(R.string.error_insufficient_stock, viewModel.actionError.value?.res)
        }
}