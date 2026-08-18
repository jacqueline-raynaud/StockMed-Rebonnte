package com.openclassrooms.rebonnte.fake

import com.openclassrooms.rebonnte.data.model.HistoryAction
import com.openclassrooms.rebonnte.data.repository.MedicineRepository
import com.openclassrooms.rebonnte.data.repository.MedicineSort
import com.openclassrooms.rebonnte.data.repository.StockErrorReason
import com.openclassrooms.rebonnte.data.repository.StockException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Ces tests verrouillent les trois defauts remontes par le Product Owner et le
 * service qualite. Chacun echouerait sur le code d'origine.
 */
class FakeMedicineRepositoryTest {

    private lateinit var repository: MedicineRepository

    @Before
    fun setUp() {
        repository = FakeMedicineRepository()
    }

    // --- Gestion du stock ----------------------------------------------------
    @Test
    fun `updateStock updates the requested medicine and leaves the others untouched`() = runTest {
        val target = repository.addMedicine("Doliprane", stock = 10, aisleId = AISLE, userEmail = USER)
        val last = repository.addMedicine("Ibuprofene", stock = 10, aisleId = AISLE, userEmail = USER)

        repository.updateStock(target.id, delta = 1, userEmail = USER)

        assertEquals(11, repository.observeMedicine(target.id).first()!!.stock)
        assertEquals(10, repository.observeMedicine(last.id).first()!!.stock)
    }

    /**
     * T-21 : le retrait est **refuse**, pas rabote a zero.
     *
     * L'ancienne version ramenait le stock a zero en silence : l'operateur qui
     * demandait cinq unites sur une disponible repartait en croyant en avoir
     * sorti cinq, et l'historique enregistrait « de 1 a 0 » sans mentionner
     * l'ecart.
     */
    @Test
    fun `a removal larger than the stock is refused`() = runTest {
        val medicine = repository.addMedicine("Doliprane", stock = 1, aisleId = AISLE, userEmail = USER)

        val failure = assertThrows(StockException::class.java) {
            runBlocking { repository.updateStock(medicine.id, delta = -5, userEmail = USER) }
        }

        assertEquals(StockErrorReason.INSUFFICIENT_STOCK, failure.reason)
        // Le stock disponible accompagne le refus : l'ecran peut le dire.
        assertEquals(1, failure.available)
    }

    /** Le stock reste intact, et rien n'est journalise : l'operation n'a pas eu lieu. */
    @Test
    fun `a refused removal leaves the stock and the history untouched`() = runTest {
        val medicine = repository.addMedicine("Doliprane", stock = 1, aisleId = AISLE, userEmail = USER)

        runCatching { repository.updateStock(medicine.id, delta = -5, userEmail = USER) }

        assertEquals(1, repository.observeMedicine(medicine.id).first()!!.stock)
        val history = repository.observeHistory(medicine.id, limit = ANY_LIMIT).first()
        assertTrue(history.none { it.action == HistoryAction.STOCK_CHANGE })
    }

    /** Le retrait qui vide exactement le stock reste autorise. */
    @Test
    fun `a removal down to exactly zero is allowed`() = runTest {
        val medicine = repository.addMedicine("Doliprane", stock = 5, aisleId = AISLE, userEmail = USER)

        repository.updateStock(medicine.id, delta = -5, userEmail = USER)

        assertEquals(0, repository.observeMedicine(medicine.id).first()!!.stock)
    }

    @Test
    fun `updateStock on an unknown id changes nothing`() = runTest {
        repository.addMedicine("Doliprane", stock = 10, aisleId = AISLE, userEmail = USER)

        repository.updateStock("id-inexistant", delta = 1, userEmail = USER)

        assertEquals(1, repository.observeMedicines().first().size)
    }


    @Test
    fun `updateStock records the operation with its before and after values`() = runTest {
        val medicine = repository.addMedicine("Doliprane", stock = 10, aisleId = AISLE, userEmail = USER)

        repository.updateStock(medicine.id, delta = -3, userEmail = USER)

        val stockChange = repository.observeHistory(medicine.id, limit = ANY_LIMIT).first()
            .first { it.action == HistoryAction.STOCK_CHANGE }

        assertEquals(10, stockChange.stockBefore)
        assertEquals(7, stockChange.stockAfter)
        assertEquals(USER, stockChange.userEmail)
        assertEquals(medicine.id, stockChange.medicineId)
    }

    @Test
    fun `creating a medicine records a history entry`() = runTest {
        val medicine = repository.addMedicine("Doliprane", stock = 10, aisleId = AISLE, userEmail = USER)

        val history = repository.observeHistory(medicine.id, limit = ANY_LIMIT).first()

        assertEquals(1, history.size)
        assertEquals(HistoryAction.CREATE, history.single().action)
    }

    @Test
    fun `deleting a medicine keeps its history`() = runTest {
        val medicine = repository.addMedicine("Doliprane", stock = 10, aisleId = AISLE, userEmail = USER)

        repository.deleteMedicine(medicine.id, userEmail = USER)

        assertNull(repository.observeMedicine(medicine.id).first())
        val history = repository.observeHistory(medicine.id, limit = ANY_LIMIT).first()
        assertNotNull(history.firstOrNull { it.action == HistoryAction.DELETE })
    }

    @Test
    fun `a stock change that has no effect is not recorded`() = runTest {
        val medicine = repository.addMedicine("Doliprane", stock = 0, aisleId = AISLE, userEmail = USER)

        repository.updateStock(medicine.id, delta = 0, userEmail = USER)

        val history = repository.observeHistory(medicine.id, limit = ANY_LIMIT).first()
        assertTrue(history.none { it.action == HistoryAction.STOCK_CHANGE })
    }

    // --- Recherche et tri ----------------------------------------------------

    @Test
    fun `filtering narrows the result without amputating the source`() = runTest {
        repository.addMedicine("Doliprane", stock = 1, aisleId = AISLE, userEmail = USER)
        repository.addMedicine("Ibuprofene", stock = 1, aisleId = AISLE, userEmail = USER)

        assertEquals(1, repository.observeMedicines(query = "dol").first().size)
        assertEquals(2, repository.observeMedicines().first().size)
    }

    @Test
    fun `filtering ignores case`() = runTest {
        repository.addMedicine("Doliprane", stock = 1, aisleId = AISLE, userEmail = USER)

        assertEquals(1, repository.observeMedicines(query = "DOLI").first().size)
    }

    @Test
    fun `sorting does not reorder the underlying source`() = runTest {
        repository.addMedicine("Zovirax", stock = 5, aisleId = AISLE, userEmail = USER)
        repository.addMedicine("Aspirine", stock = 1, aisleId = AISLE, userEmail = USER)

        val sortedByName = repository.observeMedicines(sort = MedicineSort.NAME_ASC).first()
        assertEquals(listOf("Aspirine", "Zovirax"), sortedByName.map { it.name })

        val unsorted = repository.observeMedicines().first()
        assertEquals(listOf("Zovirax", "Aspirine"), unsorted.map { it.name })
    }

    /** L'ordre alphabetique attendu ignore la casse : un tri lexicographique
     *  brut placerait « Zovirax » avant « aspirine ». */
    @Test
    fun `sorting by name ignores case`() = runTest {
        repository.addMedicine("aspirine", stock = 1, aisleId = AISLE, userEmail = USER)
        repository.addMedicine("Zovirax", stock = 5, aisleId = AISLE, userEmail = USER)

        val sorted = repository.observeMedicines(sort = MedicineSort.NAME_ASC).first()

        assertEquals(listOf("aspirine", "Zovirax"), sorted.map { it.name })
    }

    @Test
    fun `sorting by stock orders ascending`() = runTest {
        repository.addMedicine("Zovirax", stock = 5, aisleId = AISLE, userEmail = USER)
        repository.addMedicine("Aspirine", stock = 1, aisleId = AISLE, userEmail = USER)

        val sorted = repository.observeMedicines(sort = MedicineSort.STOCK_ASC).first()

        assertEquals(listOf(1, 5), sorted.map { it.stock })
    }

    private companion object {
        const val USER = "operateur@rebonnte.fr"
        const val AISLE = "aisle-1"

        /** Assez large pour que le plafond ne joue pas dans ces tests. */
        const val ANY_LIMIT = 100
    }

    // --- Chargement paresseux ------------------------------------------------

    @Test
    fun `observeMedicinesInAisle keeps only that aisle, in alphabetical order`() = runTest {
        repository.addMedicine("Insuline", stock = 5, aisleId = "cold", userEmail = USER)
        repository.addMedicine("Zovirax", stock = 5, aisleId = AISLE, userEmail = USER)
        repository.addMedicine("aspirine", stock = 5, aisleId = AISLE, userEmail = USER)

        val medicines = repository.observeMedicinesInAisle(AISLE).first()

        assertEquals(listOf("aspirine", "Zovirax"), medicines.map { it.name })
    }

    /** Le plafond garde les entrees les plus recentes, pas les premieres. */
    @Test
    fun `observeHistory returns at most the requested number of recent entries`() = runTest {
        val medicine = repository.addMedicine("Doliprane", stock = 0, aisleId = AISLE, userEmail = USER)
        repeat(5) { repository.updateStock(medicine.id, delta = 1, userEmail = USER) }

        val history = repository.observeHistory(medicine.id, limit = 2).first()

        assertEquals(2, history.size)
        assertEquals(listOf(5, 4), history.map { it.stockAfter })
    }

    // --- Correction d'une fiche ----------------------------------------------

    /** Une faute d'orthographe se corrige, et la correction laisse une trace. */
    @Test
    fun `renaming a medicine records an update in the history`() = runTest {
        val medicine = repository.addMedicine("Dolipran", stock = 10, aisleId = AISLE, userEmail = USER)

        repository.updateMedicine(
            id = medicine.id,
            name = "Doliprane",
            aisleId = AISLE,
            aisleName = "Stockage standard",
            userEmail = USER
        )

        assertEquals("Doliprane", repository.observeMedicine(medicine.id).first()!!.name)
        val update = repository.observeHistory(medicine.id, limit = ANY_LIMIT).first()
            .single { it.action == HistoryAction.UPDATE }
        assertTrue(update.details.contains("Dolipran"))
        assertTrue(update.details.contains("Doliprane"))
        assertEquals(USER, update.userEmail)
    }

    /** Un changement d'emplacement nomme la destination dans la trace. */
    @Test
    fun `moving a medicine records the destination`() = runTest {
        val medicine = repository.addMedicine("Insuline", stock = 4, aisleId = AISLE, userEmail = USER)

        repository.updateMedicine(
            id = medicine.id,
            name = "Insuline",
            aisleId = "cold",
            aisleName = "Stockage froid",
            userEmail = USER
        )

        assertEquals("cold", repository.observeMedicine(medicine.id).first()!!.aisleId)
        val update = repository.observeHistory(medicine.id, limit = ANY_LIMIT).first()
            .single { it.action == HistoryAction.UPDATE }
        assertTrue(update.details.contains("Stockage froid"))
    }

    /**
     * Le stock ne bouge pas : une correction de fiche n'est pas un mouvement.
     * Le confondre ferait apparaitre des variations de stock imaginaires dans
     * le journal.
     */
    @Test
    fun `an update leaves the stock untouched`() = runTest {
        val medicine = repository.addMedicine("Doliprane", stock = 7, aisleId = AISLE, userEmail = USER)

        repository.updateMedicine(medicine.id, "Doliprane 1000", AISLE, "Stockage standard", USER)

        assertEquals(7, repository.observeMedicine(medicine.id).first()!!.stock)
        val update = repository.observeHistory(medicine.id, limit = ANY_LIMIT).first()
            .single { it.action == HistoryAction.UPDATE }
        assertEquals(7, update.stockBefore)
        assertEquals(7, update.stockAfter)
    }

    /** Reenregistrer sans rien changer ne doit pas polluer l'historique. */
    @Test
    fun `saving without any change records nothing`() = runTest {
        val medicine = repository.addMedicine("Doliprane", stock = 3, aisleId = AISLE, userEmail = USER)

        repository.updateMedicine(medicine.id, "Doliprane", AISLE, "Stockage standard", USER)

        val history = repository.observeHistory(medicine.id, limit = ANY_LIMIT).first()
        assertTrue(history.none { it.action == HistoryAction.UPDATE })
    }

    /** Un nom vide ne remplace pas un nom valide. */
    @Test
    fun `an update with a blank name is ignored`() = runTest {
        val medicine = repository.addMedicine("Doliprane", stock = 3, aisleId = AISLE, userEmail = USER)

        repository.updateMedicine(medicine.id, "   ", AISLE, "Stockage standard", USER)

        assertEquals("Doliprane", repository.observeMedicine(medicine.id).first()!!.name)
    }
}