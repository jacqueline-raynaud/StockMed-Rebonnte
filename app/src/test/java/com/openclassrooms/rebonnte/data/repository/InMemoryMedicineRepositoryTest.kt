package com.openclassrooms.rebonnte.data.repository

import com.openclassrooms.rebonnte.data.model.HistoryAction
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Ces tests verrouillent les trois defauts remontes par le Product Owner et le
 * service qualite. Chacun echouerait sur le code d'origine.
 */
class InMemoryMedicineRepositoryTest {

    private lateinit var repository: MedicineRepository

    @Before
    fun setUp() {
        repository = InMemoryMedicineRepository()
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

    @Test
    fun `updateStock never lets the stock fall below zero`() = runTest {
        val medicine = repository.addMedicine("Doliprane", stock = 1, aisleId = AISLE, userEmail = USER)

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

        val stockChange = repository.observeHistory(medicine.id).first()
            .first { it.action == HistoryAction.STOCK_CHANGE }

        assertEquals(10, stockChange.stockBefore)
        assertEquals(7, stockChange.stockAfter)
        assertEquals(USER, stockChange.userEmail)
        assertEquals(medicine.id, stockChange.medicineId)
    }

    @Test
    fun `creating a medicine records a history entry`() = runTest {
        val medicine = repository.addMedicine("Doliprane", stock = 10, aisleId = AISLE, userEmail = USER)

        val history = repository.observeHistory(medicine.id).first()

        assertEquals(1, history.size)
        assertEquals(HistoryAction.CREATE, history.single().action)
    }

    @Test
    fun `deleting a medicine keeps its history`() = runTest {
        val medicine = repository.addMedicine("Doliprane", stock = 10, aisleId = AISLE, userEmail = USER)

        repository.deleteMedicine(medicine.id, userEmail = USER)

        assertNull(repository.observeMedicine(medicine.id).first())
        val history = repository.observeHistory(medicine.id).first()
        assertNotNull(history.firstOrNull { it.action == HistoryAction.DELETE })
    }

    @Test
    fun `a stock change that has no effect is not recorded`() = runTest {
        val medicine = repository.addMedicine("Doliprane", stock = 0, aisleId = AISLE, userEmail = USER)

        repository.updateStock(medicine.id, delta = -1, userEmail = USER)

        val history = repository.observeHistory(medicine.id).first()
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

        val sortedByName = repository.observeMedicines(sort = MedicineSort.NAME).first()
        assertEquals(listOf("Aspirine", "Zovirax"), sortedByName.map { it.name })

        val unsorted = repository.observeMedicines().first()
        assertEquals(listOf("Zovirax", "Aspirine"), unsorted.map { it.name })
    }

    @Test
    fun `sorting by stock orders ascending`() = runTest {
        repository.addMedicine("Zovirax", stock = 5, aisleId = AISLE, userEmail = USER)
        repository.addMedicine("Aspirine", stock = 1, aisleId = AISLE, userEmail = USER)

        val sorted = repository.observeMedicines(sort = MedicineSort.STOCK).first()

        assertEquals(listOf(1, 5), sorted.map { it.stock })
    }

    private companion object {
        const val USER = "operateur@rebonnte.fr"
        const val AISLE = "aisle-1"
    }
}
