package com.openclassrooms.rebonnte.ui.aisle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openclassrooms.rebonnte.data.model.AisleDto
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

    val aisles: StateFlow<List<AisleDto>> = repository.observeAisles()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    /**
     * Remplace l'ancien addRandomAisle, qui fabriquait des « Aisle 2 »,
     * « Aisle 3 » sans signification. Un emplacement de stockage porte un nom
     * choisi par l'operateur.
     */
    fun addAisle(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.addAisle(name)
        }
    }
}
