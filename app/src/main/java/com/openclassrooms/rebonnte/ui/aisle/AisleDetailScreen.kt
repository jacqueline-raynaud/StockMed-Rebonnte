package com.openclassrooms.rebonnte.ui.aisle

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.openclassrooms.rebonnte.ui.medicine.MedicineContent
import com.openclassrooms.rebonnte.ui.medicine.MedicineListUiState
import com.openclassrooms.rebonnte.ui.medicine.MedicineViewModel

/**
 * Medicines for a specific location.
 *
 * The screen used to read the shared list of all medicines and keep the ones
 * carrying this aisle: the whole stock came down the wire to display a handful
 * of lines. The filter now belongs to the query, so only the medicines of this
 * aisle are ever read.
 *
 * Reuses [MedicineContent] and its previews.
 */
@Composable
fun AisleDetailScreen(
    aisleId: String,
    medicineViewModel: MedicineViewModel,
    onMedicineClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // remember(aisleId) to avoid recreating a flow upon every recomposition
    val medicinesFlow = remember(aisleId) { medicineViewModel.observeMedicinesInAisle(aisleId) }
    val uiState by medicinesFlow.collectAsState(initial = MedicineListUiState())

    MedicineContent(
        medicines = uiState.medicines,
        isLoading = uiState.isLoading,
        errorMessage = uiState.errorMessage,
        onMedicineClick = onMedicineClick,
        modifier = modifier
    )
}

