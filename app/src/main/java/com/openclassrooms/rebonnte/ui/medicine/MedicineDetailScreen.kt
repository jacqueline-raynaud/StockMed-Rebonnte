package com.openclassrooms.rebonnte.ui.medicine

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.openclassrooms.rebonnte.data.model.History
import com.openclassrooms.rebonnte.ui.aisle.AisleViewModel
import java.text.DateFormat
import java.util.Date

@Composable
fun MedicineDetailScreen(
    medicineId: String,
    medicineViewModel: MedicineViewModel,
    aisleViewModel: AisleViewModel,
    modifier: Modifier = Modifier
) {
    // remember(medicineId) : sans cela, un nouveau Flow serait cree a chaque
    // recomposition.
    val medicineFlow = remember(medicineId) { medicineViewModel.observeMedicine(medicineId) }
    val historyFlow = remember(medicineId) { medicineViewModel.observeHistory(medicineId) }

    val medicine by medicineFlow.collectAsState(initial = null)
    val histories by historyFlow.collectAsState(initial = emptyList())
    val aisles by aisleViewModel.aisles.collectAsState()

    val currentMedicine = medicine
    if (currentMedicine == null) {
        // L'ancien code faisait un `return` au milieu du composable : ecran
        // blanc sans explication.
        Text(
            text = "Medicament introuvable",
            modifier = modifier.padding(16.dp)
        )
        return
    }

    Column(modifier = modifier.padding(16.dp)) {
        TextField(
            value = currentMedicine.name,
            onValueChange = {},
            label = { Text("Name") },
            enabled = false,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            // Le medicament ne porte que l'identifiant de son rayon ; le
            // libelle se resout ici, a l'affichage.
            value = aisles.firstOrNull { it.id == currentMedicine.aisleId }?.name
                ?: "Rayon inconnu",
            onValueChange = {},
            label = { Text("Aisle") },
            enabled = false,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = { medicineViewModel.updateStock(currentMedicine.id, delta = -1) }) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Minus One"
                )
            }
            TextField(
                value = currentMedicine.stock.toString(),
                onValueChange = {},
                label = { Text("Stock") },
                enabled = false,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { medicineViewModel.updateStock(currentMedicine.id, delta = 1) }) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Plus One"
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "History", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(histories, key = { it.id }) { history ->
                HistoryItem(history = history)
            }
        }
    }
}

@Composable
fun HistoryItem(history: History) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = history.medicineName, fontWeight = FontWeight.Bold)
            Text(text = "User: ${history.userEmail.ifEmpty { "utilisateur inconnu" }}")
            Text(text = "Date: ${formatHistoryDate(history.date)}")
            Text(text = "Stock: ${history.stockBefore} -> ${history.stockAfter}")
            Text(text = "Details: ${history.details}")
        }
    }
}

private fun formatHistoryDate(epochMillis: Long): String =
    if (epochMillis == 0L) {
        "-"
    } else {
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
            .format(Date(epochMillis))
    }
