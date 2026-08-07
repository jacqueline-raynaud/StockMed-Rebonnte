package com.openclassrooms.rebonnte.ui.aisle

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.openclassrooms.rebonnte.ui.component.MedicineItem
import com.openclassrooms.rebonnte.ui.medicine.MedicineViewModel

@Composable
fun AisleDetailScreen(
    aisleId: String,
    medicineViewModel: MedicineViewModel,
    onMedicineClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val medicines by medicineViewModel.medicines.collectAsState()
    val filteredMedicines = medicines.filter { it.aisleId == aisleId }

    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(filteredMedicines, key = { it.id }) { medicine ->
            MedicineItem(
                medicine = medicine,
                onClick = { onMedicineClick(medicine.id) }
            )
        }
    }
}
