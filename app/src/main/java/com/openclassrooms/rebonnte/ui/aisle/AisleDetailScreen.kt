package com.openclassrooms.rebonnte.ui.aisle

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.openclassrooms.rebonnte.ui.medicine.MedicineContent
import com.openclassrooms.rebonnte.ui.medicine.MedicineViewModel

/**
 * Les medicaments d'un emplacement.
 *
 * Aucune composable propre : c'est la meme liste que l'ecran des medicaments,
 * restreinte a un emplacement. Elle reutilise donc [MedicineContent] — et sa
 * previsualisation par la meme occasion.
 */
@Composable
fun AisleDetailScreen(
    aisleId: String,
    medicineViewModel: MedicineViewModel,
    onMedicineClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by medicineViewModel.uiState.collectAsState()

    // remember : le filtrage ne se refait que si la liste ou l'emplacement
    // changent, pas a chaque recomposition.
    val medicines = remember(uiState.medicines, aisleId) {
        uiState.medicines.filter { it.aisleId == aisleId }
    }

    MedicineContent(
        medicines = medicines,
        onMedicineClick = onMedicineClick,
        modifier = modifier
    )
}
