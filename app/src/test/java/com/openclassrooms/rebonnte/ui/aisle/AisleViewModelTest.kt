package com.openclassrooms.rebonnte.ui.aisle

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

    @Test
    fun `the aisle list starts with the default aisle`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = AisleViewModel(InMemoryAisleRepository())
        backgroundScope.launch { viewModel.aisles.collect { } }

        assertEquals(listOf("Main Aisle"), viewModel.aisles.value.map { it.name })
    }

    @Test
    fun `adding an aisle appends it with a sequential name`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = AisleViewModel(InMemoryAisleRepository())
        backgroundScope.launch { viewModel.aisles.collect { } }

        viewModel.addRandomAisle()

        assertEquals(listOf("Main Aisle", "Aisle 2"), viewModel.aisles.value.map { it.name })
    }

    @Test
    fun `each aisle receives a distinct identifier`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = AisleViewModel(InMemoryAisleRepository())
        backgroundScope.launch { viewModel.aisles.collect { } }

        viewModel.addRandomAisle()
        viewModel.addRandomAisle()

        val ids = viewModel.aisles.value.map { it.id }
        assertEquals(3, ids.size)
        assertTrue(ids.toSet().size == ids.size)
    }
}
