package com.openclassrooms.rebonnte.ui.aisle

import androidx.lifecycle.ViewModel
import com.openclassrooms.rebonnte.data.model.Aisle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AisleViewModel : ViewModel() {
    var _aisles = MutableStateFlow<List<Aisle>>(emptyList())
    val aisles: StateFlow<List<Aisle>> get() = _aisles

    init {
        _aisles.value = listOf(Aisle("Main Aisle"))
    }

    fun addRandomAisle() {
        val currentAisles: MutableList<Aisle> = ArrayList(aisles.value)
        currentAisles.add(Aisle("Aisle " + (currentAisles.size + 1)))
        _aisles.value = currentAisles
    }
}

