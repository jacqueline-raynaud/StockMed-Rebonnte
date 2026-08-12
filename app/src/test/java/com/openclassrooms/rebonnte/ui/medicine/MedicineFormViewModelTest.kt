package com.openclassrooms.rebonnte.ui.medicine

import com.openclassrooms.rebonnte.data.model.HistoryAction
import com.openclassrooms.rebonnte.data.model.StorageLocations
import com.openclassrooms.rebonnte.data.repository.impl.InMemoryAisleRepository
import com.openclassrooms.rebonnte.data.repository.impl.InMemoryMedicineRepository
import com.openclassrooms.rebonnte.fake.FakeUserRepository
import com.openclassrooms.rebonnte.util.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * T-15 : la creation remplace l'ancien bouton qui ajoutait un medicament au nom
 * et au stock aleatoires, dans un emplacement tire au hasard.
 */
class MedicineFormViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var medicineRepository: InMemoryMedicineRepository
    private lateinit var viewModel: MedicineFormViewModel

    private val standard = StorageLocations.DEFAULTS.first()

    @Before
    fun setUp() {
        medicineRepository = InMemoryMedicineRepository()
        viewModel = MedicineFormViewModel(
            medicineRepository = medicineRepository,
            userRepository = FakeUserRepository(),
            aisleRepository = InMemoryAisleRepository()
        )
    }

    // --- Validation ----------------------------------------------------------

    @Test
    fun `an empty form creates nothing`() = runTest(mainDispatcherRule.dispatcher) {
        viewModel.submit()

        assertNotNull(viewModel.uiState.value.nameError)
        assertNotNull(viewModel.uiState.value.aisleError)
        assertTrue(medicineRepository.observeMedicines().first().isEmpty())
    }

    @Test
    fun `a name without a storage location is rejected`() =
        runTest(mainDispatcherRule.dispatcher) {
            viewModel.onNameChange("Doliprane")

            viewModel.submit()

            assertNotNull(viewModel.uiState.value.aisleError)
            assertTrue(medicineRepository.observeMedicines().first().isEmpty())
        }

    /** Le champ n'accepte que des chiffres : un stock negatif est impossible. */
    @Test
    fun `the quantity field discards anything but digits`() =
        runTest(mainDispatcherRule.dispatcher) {
            viewModel.onStockChange("-12a3")

            assertEquals("123", viewModel.uiState.value.stock)
        }

    @Test
    fun `editing a field clears its error`() = runTest(mainDispatcherRule.dispatcher) {
        viewModel.submit()
        assertNotNull(viewModel.uiState.value.nameError)

        viewModel.onNameChange("Doliprane")

        assertEquals(null, viewModel.uiState.value.nameError)
    }

    // --- Creation ------------------------------------------------------------

    @Test
    fun `a valid form creates the medicine in the chosen location`() =
        runTest(mainDispatcherRule.dispatcher) {
            viewModel.onNameChange("  Doliprane  ")
            viewModel.onAisleChange(standard.id)
            viewModel.onStockChange("25")

            viewModel.submit()

            val created = medicineRepository.observeMedicines().first().single()
            // Le nom est nettoye : une espace de frappe ne doit pas creer deux
            // medicaments differents.
            assertEquals("Doliprane", created.name)
            assertEquals(standard.id, created.aisleId)
            assertEquals(25, created.stock)
        }

    @Test
    fun `creation is traced with the signed in operator`() =
        runTest(mainDispatcherRule.dispatcher) {
            viewModel.onNameChange("Doliprane")
            viewModel.onAisleChange(standard.id)
            viewModel.onStockChange("10")

            viewModel.submit()

            val created = medicineRepository.observeMedicines().first().single()
            val entry = medicineRepository.observeHistory(created.id).first().single()
            assertEquals(HistoryAction.CREATE, entry.action)
            assertEquals(FakeUserRepository.SIGNED_IN_USER.email, entry.userEmail)
        }

    /** L'ecran observe isSaved pour revenir a la liste. */
    @Test
    fun `a successful creation signals the screen`() = runTest(mainDispatcherRule.dispatcher) {
        assertFalse(viewModel.uiState.value.isSaved)

        viewModel.onNameChange("Doliprane")
        viewModel.onAisleChange(standard.id)
        viewModel.submit()

        assertTrue(viewModel.uiState.value.isSaved)
    }

    @Test
    fun `the storage locations are offered as choices`() =
        runTest(mainDispatcherRule.dispatcher) {
            assertEquals(
                StorageLocations.DEFAULTS.map { it.name },
                viewModel.aisles.first().map { it.name }
            )
        }
}
