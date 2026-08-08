package com.openclassrooms.rebonnte.ui.aisle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openclassrooms.rebonnte.data.model.Aisle
import com.openclassrooms.rebonnte.data.repository.AisleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AisleViewModel @Inject constructor(
    private val repository: AisleRepository
) : ViewModel() {

    val aisles: StateFlow<List<Aisle>> = repository.observeAisles()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun addRandomAisle() {
        viewModelScope.launch {
            repository.addAisle("Aisle ${aisles.value.size + 1}")
        }
    }
}
