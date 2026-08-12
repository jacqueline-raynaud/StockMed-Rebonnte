package com.openclassrooms.rebonnte.ui.aisle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openclassrooms.rebonnte.data.repository.AisleRepository
import com.openclassrooms.rebonnte.ui.model.AisleUi
import com.openclassrooms.rebonnte.ui.model.toUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AisleViewModel @Inject constructor(
    private val repository: AisleRepository
) : ViewModel() {

    val aisles: StateFlow<List<AisleUi>> = repository.observeAisles()
        .map { aisles -> aisles.map { it.toUi() } }
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
