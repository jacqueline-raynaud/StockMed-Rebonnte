package com.openclassrooms.rebonnte.ui.aisle

import androidx.lifecycle.ViewModel
import com.openclassrooms.rebonnte.data.model.Aisle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class AisleViewModel : ViewModel() {
    private val _aisles = MutableStateFlow<List<Aisle>>(emptyList())
    val aisles: StateFlow<List<Aisle>> = _aisles.asStateFlow()

    init {
        _aisles.value = listOf(Aisle(id = UUID.randomUUID().toString(), name = "Main Aisle"))
    }

    fun addRandomAisle() {
        val current = _aisles.value
        _aisles.value = current + Aisle(
            id = UUID.randomUUID().toString(),
            name = "Aisle ${current.size + 1}"
        )
    }
}
