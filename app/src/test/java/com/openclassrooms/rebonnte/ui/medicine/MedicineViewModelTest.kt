package com.openclassrooms.rebonnte.ui.medicine

import com.openclassrooms.rebonnte.data.model.AisleDto
import com.openclassrooms.rebonnte.data.model.HistoryAction
import com.openclassrooms.rebonnte.data.repository.impl.InMemoryAisleRepository
import com.openclassrooms.rebonnte.data.repository.impl.InMemoryMedicineRepository
import com.openclassrooms.rebonnte.data.repository.MedicineSort
import com.openclassrooms.rebonnte.fake.FakeUserRepository
import com.openclassrooms.rebonnte.util.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MedicineViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: InMemoryMedicineRepository
    private lateinit var userRepository: FakeUserRepository
    private lateinit var viewModel: MedicineViewModel

    private val aisle = AisleDto(id = "aisle-1", name = "Rayon principal")

    @Before
    fun setUp() {
        // Le depot en memoire sert de double : c'est une implementation reelle
        // du contrat, donc les tests valident le ViewModel contre le vrai
        // comportement attendu.
        repository = InMemoryMedicineRepository()
        userRepository = FakeUserRepository()
        viewModel = MedicineViewModel(repository, userRepository, InMemoryAisleRepository())
    }

    /** `medicines` n'emet que tant qu'un collecteur est actif (WhileSubscribed). */
    private fun kotlinx.coroutines.test.TestScope.collectMedicines() {
        backgroundScope.launch { viewModel.medicines.collect { } }
    }

    /**
     * T-44 : un mouvement de cinquante boites doit produire **une** entree
     * d'historique. Avec des appuis unitaires, le service qualite cherchant
     * « qui a retire 50 boites » trouvait cinquante lignes de « -1 ».
     */
    @Test
    fun `a bulk movement produces a single history entry`() =
        runTest(mainDispatcherRule.dispatcher) {
            collectMedicines()
            repository.addMedicine("Doliprane", 60, aisle.id, "")
            val medicine = viewModel.medicines.value.single()

            viewModel.updateStock(medicine.id, delta = -50)

            val movements = repository.observeHistory(medicine.id).first()
                .filter { it.action == HistoryAction.STOCK_CHANGE }
            assertEquals(1, movements.size)
            assertEquals(60, movements.single().stockBefore)
            assertEquals(10, movements.single().stockAfter)
        }

    /**
     * L'e-mail signe l'historique. Il est lu au moment de l'operation, donc une
     * session ouverte doit se retrouver dans la trace.
     */
    @Test
    fun `a stock change is signed with the signed in operator`() = runTest(mainDispatcherRule.dispatcher) {
        collectMedicines()
        repository.addMedicine("Doliprane", 10, aisle.id, "")
        val medicine = viewModel.medicines.value.single()

        viewModel.updateStock(medicine.id, delta = 5)

        val history = repository.observeHistory(medicine.id).first()
        val stockChange = history.single { it.action == HistoryAction.STOCK_CHANGE }
        assertEquals(FakeUserRepository.SIGNED_IN_USER.email, stockChange.userEmail)
    }

    @Test
    fun `a stock change with no session leaves an empty operator`() = runTest(mainDispatcherRule.dispatcher) {
        userRepository = FakeUserRepository(initialUser = null)
        viewModel = MedicineViewModel(repository, userRepository, InMemoryAisleRepository())
        collectMedicines()
        repository.addMedicine("Doliprane", 10, aisle.id, "")
        val medicine = viewModel.medicines.value.single()

        viewModel.updateStock(medicine.id, delta = 1)

        val stockChange = repository.observeHistory(medicine.id).first()
            .single { it.action == HistoryAction.STOCK_CHANGE }
        assertEquals("", stockChange.userEmail)
    }

    @Test
    fun `deleting removes the medicine from the list`() = runTest(mainDispatcherRule.dispatcher) {
        collectMedicines()
        repository.addMedicine("Doliprane", 10, aisle.id, "")
        val medicine = viewModel.medicines.value.single()

        viewModel.deleteMedicine(medicine.id)

        assertTrue(viewModel.medicines.value.isEmpty())
    }

    // --- Recherche et tri ----------------------------------------------------

    /**
     * Regression : l'ancien filterByName ecrasait la source de verite. Ici le
     * filtre est un parametre de requete, donc l'effacer restaure la liste.
     */
    @Test
    fun `clearing the search restores the full list`() = runTest(mainDispatcherRule.dispatcher) {
        collectMedicines()
        repository.addMedicine("Doliprane", 1, aisle.id, "")
        repository.addMedicine("Ibuprofene", 1, aisle.id, "")

        viewModel.filterByName("dol")
        assertEquals(1, viewModel.medicines.value.size)

        viewModel.filterByName("")
        assertEquals(2, viewModel.medicines.value.size)
    }

    @Test
    fun `sorting by name ascending reorders the exposed list`() =
        runTest(mainDispatcherRule.dispatcher) {
            collectMedicines()
            repository.addMedicine("Zovirax", 5, aisle.id, "")
            repository.addMedicine("Aspirine", 1, aisle.id, "")

            viewModel.sortBy(MedicineSort.NAME_ASC)

            assertEquals(
                listOf("Aspirine", "Zovirax"),
                viewModel.medicines.value.map { it.name }
            )
        }

    @Test
    fun `sorting by name descending reverses the order`() =
        runTest(mainDispatcherRule.dispatcher) {
            collectMedicines()
            repository.addMedicine("Aspirine", 1, aisle.id, "")
            repository.addMedicine("Zovirax", 5, aisle.id, "")

            viewModel.sortBy(MedicineSort.NAME_DESC)

            assertEquals(
                listOf("Zovirax", "Aspirine"),
                viewModel.medicines.value.map { it.name }
            )
        }

    @Test
    fun `sorting by stock ascending reorders the exposed list`() =
        runTest(mainDispatcherRule.dispatcher) {
            collectMedicines()
            repository.addMedicine("Zovirax", 5, aisle.id, "")
            repository.addMedicine("Aspirine", 1, aisle.id, "")

            viewModel.sortBy(MedicineSort.STOCK_ASC)

            assertEquals(listOf(1, 5), viewModel.medicines.value.map { it.stock })
        }

    @Test
    fun `sorting by stock descending reverses the order`() =
        runTest(mainDispatcherRule.dispatcher) {
            collectMedicines()
            repository.addMedicine("Aspirine", 1, aisle.id, "")
            repository.addMedicine("Zovirax", 5, aisle.id, "")

            viewModel.sortBy(MedicineSort.STOCK_DESC)

            assertEquals(listOf(5, 1), viewModel.medicines.value.map { it.stock })
        }

    /** Le menu coche le critere actif : il doit donc etre observable. */
    @Test
    fun `the active sort criterion is exposed`() = runTest(mainDispatcherRule.dispatcher) {
        assertEquals(MedicineSort.NONE, viewModel.currentSort.value)

        viewModel.sortBy(MedicineSort.STOCK_DESC)

        assertEquals(MedicineSort.STOCK_DESC, viewModel.currentSort.value)
    }
}
