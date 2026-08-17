package com.openclassrooms.rebonnte.ui.aisle

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.openclassrooms.rebonnte.ui.medicine.MedicineContent
import com.openclassrooms.rebonnte.ui.medicine.MedicineViewModel
import com.openclassrooms.rebonnte.ui.theme.RebonnteTheme

/**
 * Medicines for a specifiqc location
 * reuse [MedicineContent] and its preview
 */
@Composable
fun AisleDetailScreen(
    aisleId: String,
    medicineViewModel: MedicineViewModel,
    onMedicineClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by medicineViewModel.uiState.collectAsState()

    val medicines = remember(uiState.medicines, aisleId) {
        uiState.medicines.filter { it.aisleId == aisleId }
    }

    MedicineContent(
        medicines = medicines,
        onMedicineClick = onMedicineClick,
        modifier = modifier
    )
}

