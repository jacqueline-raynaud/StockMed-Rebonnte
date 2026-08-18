package com.openclassrooms.rebonnte.ui.medicine

import com.openclassrooms.rebonnte.data.model.AisleDto
import com.openclassrooms.rebonnte.data.model.HistoryAction
import com.openclassrooms.rebonnte.data.repository.fake.InMemoryAisleRepository
import com.openclassrooms.rebonnte.data.repository.fake.InMemoryMedicineRepository
import com.openclassrooms.rebonnte.data.repository.MedicineSort
import com.openclassrooms.rebonnte.fake.FakeUserRepository
import com.openclassrooms.rebonnte.util.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        backgroundScope.launch { viewModel.uiState.collect { } }
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
            val medicine = viewModel.uiState.value.medicines.single()

            viewModel.updateStock(medicine.id, delta = -50)

            val movements = repository.observeHistory(medicine.id, limit = HISTORY_PAGE_SIZE).first()
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
        val medicine = viewModel.uiState.value.medicines.single()

        viewModel.updateStock(medicine.id, delta = 5)

        val history = repository.observeHistory(medicine.id, limit = HISTORY_PAGE_SIZE).first()
        val stockChange = history.single { it.action == HistoryAction.STOCK_CHANGE }
        assertEquals(FakeUserRepository.SIGNED_IN_USER.email, stockChange.userEmail)
    }

    /**
     * L'identifiant est lu dans le depot et non dans l'etat : sans session,
     * l'etat est vide par construction — les flux sont geles tant que personne
     * n'est connecte, pour ne pas heurter les regles de securite Firestore.
     */
    @Test
    fun `a stock change with no session leaves an empty operator`() = runTest(mainDispatcherRule.dispatcher) {
        userRepository = FakeUserRepository(initialUser = null)
        viewModel = MedicineViewModel(repository, userRepository, InMemoryAisleRepository())
        collectMedicines()
        repository.addMedicine("Doliprane", 10, aisle.id, "")
        val medicine = repository.observeMedicines("", MedicineSort.NONE).first().single()

        viewModel.updateStock(medicine.id, delta = 1)

        val stockChange = repository.observeHistory(medicine.id, limit = HISTORY_PAGE_SIZE).first()
            .single { it.action == HistoryAction.STOCK_CHANGE }
        assertEquals("", stockChange.userEmail)
    }

    @Test
    fun `deleting removes the medicine from the list`() = runTest(mainDispatcherRule.dispatcher) {
        collectMedicines()
        repository.addMedicine("Doliprane", 10, aisle.id, "")
        val medicine = viewModel.uiState.value.medicines.single()

        viewModel.deleteMedicine(medicine.id)

        assertTrue(viewModel.uiState.value.medicines.isEmpty())
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
        assertEquals(1, viewModel.uiState.value.medicines.size)

        viewModel.filterByName("")
        assertEquals(2, viewModel.uiState.value.medicines.size)
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
                viewModel.uiState.value.medicines.map { it.name }
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
                viewModel.uiState.value.medicines.map { it.name }
            )
        }

    @Test
    fun `sorting by stock ascending reorders the exposed list`() =
        runTest(mainDispatcherRule.dispatcher) {
            collectMedicines()
            repository.addMedicine("Zovirax", 5, aisle.id, "")
            repository.addMedicine("Aspirine", 1, aisle.id, "")

            viewModel.sortBy(MedicineSort.STOCK_ASC)

            assertEquals(listOf(1, 5), viewModel.uiState.value.medicines.map { it.stock })
        }

    @Test
    fun `sorting by stock descending reverses the order`() =
        runTest(mainDispatcherRule.dispatcher) {
            collectMedicines()
            repository.addMedicine("Aspirine", 1, aisle.id, "")
            repository.addMedicine("Zovirax", 5, aisle.id, "")

            viewModel.sortBy(MedicineSort.STOCK_DESC)

            assertEquals(listOf(5, 1), viewModel.uiState.value.medicines.map { it.stock })
        }

    // --- Chargement paresseux (T-23) -----------------------------------------

    /**
     * L'ecran d'un emplacement lisait la liste complete et gardait les
     * medicaments portant ce rayon : tout le stock descendait pour afficher
     * quelques lignes. Le filtre appartient desormais a la requete.
     */
    @Test
    fun `an aisle only exposes its own medicines`() = runTest(mainDispatcherRule.dispatcher) {
        repository.addMedicine("Insuline", 5, "cold", "")
        repository.addMedicine("Doliprane", 10, aisle.id, "")
        repository.addMedicine("Aspirine", 10, aisle.id, "")

        var state = MedicineListUiState()
        backgroundScope.launch { viewModel.observeMedicinesInAisle(aisle.id).collect { state = it } }

        assertEquals(listOf("Aspirine", "Doliprane"), state.medicines.map { it.name })
    }

    /**
     * Ouvrir une fiche ne doit pas telecharger des centaines d'entrees : une
     * page suffit, et l'ecran sait qu'il en reste.
     */
    @Test
    fun `opening a card reads a single page of history`() = runTest(mainDispatcherRule.dispatcher) {
        val medicine = repository.addMedicine("Doliprane", 0, aisle.id, "")
        repeat(HISTORY_PAGE_SIZE) { repository.updateStock(medicine.id, 1, "") }

        var state = MedicineDetailUiState()
        backgroundScope.launch { viewModel.observeDetail(medicine.id).collect { state = it } }

        // HISTORY_PAGE_SIZE mouvements plus la creation : une entree de trop,
        // donc une page pleine et un reste.
        assertEquals(HISTORY_PAGE_SIZE, state.histories.size)
        assertTrue(state.hasMoreHistory)
    }

    @Test
    fun `asking for more history widens the window`() = runTest(mainDispatcherRule.dispatcher) {
        val medicine = repository.addMedicine("Doliprane", 0, aisle.id, "")
        repeat(HISTORY_PAGE_SIZE) { repository.updateStock(medicine.id, 1, "") }
        var state = MedicineDetailUiState()
        backgroundScope.launch { viewModel.observeDetail(medicine.id).collect { state = it } }

        viewModel.showMoreHistory()

        assertEquals(HISTORY_PAGE_SIZE + 1, state.histories.size)
        assertFalse(state.hasMoreHistory)
    }

    /**
     * Sans remise a zero, ouvrir une fiche apres avoir deroule l'historique
     * d'une autre relirait d'emblee autant d'entrees.
     */
    @Test
    fun `the history window resets on the next card`() = runTest(mainDispatcherRule.dispatcher) {
        val medicine = repository.addMedicine("Doliprane", 0, aisle.id, "")
        repeat(HISTORY_PAGE_SIZE) { repository.updateStock(medicine.id, 1, "") }
        val first = backgroundScope.launch { viewModel.observeDetail(medicine.id).collect { } }
        viewModel.showMoreHistory()
        first.cancel()

        var state = MedicineDetailUiState()
        backgroundScope.launch { viewModel.observeDetail(medicine.id).collect { state = it } }

        assertEquals(HISTORY_PAGE_SIZE, state.histories.size)
    }

    /**
     * Le menu coche le critere actif : il doit donc etre observable.
     *
     * Le collecteur est indispensable depuis que le critere vit dans l'etat
     * unique : celui-ci est partage en `WhileSubscribed`, donc sans abonne il
     * reste fige sur sa valeur initiale. A l'ecran, c'est la composable qui
     * joue ce role.
     */
    @Test
    fun `the active sort criterion is exposed`() = runTest(mainDispatcherRule.dispatcher) {
        collectMedicines()
        assertEquals(MedicineSort.NONE, viewModel.uiState.value.sort)

        viewModel.sortBy(MedicineSort.STOCK_DESC)

        assertEquals(MedicineSort.STOCK_DESC, viewModel.uiState.value.sort)
    }
}
