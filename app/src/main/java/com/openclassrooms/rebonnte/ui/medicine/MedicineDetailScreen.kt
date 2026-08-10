package com.openclassrooms.rebonnte.ui.medicine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
    onDeleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    // remember(medicineId) : sans cela, un nouveau Flow serait cree a chaque
    // recomposition.
    val medicineFlow = remember(medicineId) { medicineViewModel.observeMedicine(medicineId) }
    val historyFlow = remember(medicineId) { medicineViewModel.observeHistory(medicineId) }

    val medicine by medicineFlow.collectAsState(initial = null)
    val histories by historyFlow.collectAsState(initial = emptyList())
    val aisles by aisleViewModel.aisles.collectAsState()

    var quantity by rememberSaveableQuantity()
    var confirmDelete by remember { mutableStateOf(false) }

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

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Supprimer ce médicament ?") },
            text = {
                Text(
                    "${currentMedicine.name} sera retiré du stock. " +
                        "Son historique reste consultable."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    medicineViewModel.deleteMedicine(currentMedicine.id)
                    onDeleted()
                }) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Annuler") }
            }
        )
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
            // Le medicament ne porte que l'identifiant de son emplacement ; le
            // libelle se resout ici, a l'affichage.
            value = aisles.firstOrNull { it.id == currentMedicine.aisleId }?.name
                ?: "Emplacement inconnu",
            onValueChange = {},
            label = { Text("Emplacement") },
            enabled = false,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = currentMedicine.stock.toString(),
            onValueChange = {},
            label = { Text("Stock") },
            enabled = false,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Une quantite saisie plutot que des appuis repetes : retirer cinquante
        // boites produit une seule operation, donc une seule entree
        // d'historique.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = quantity,
                onValueChange = { quantity = it.filter(Char::isDigit).take(5) },
                label = { Text("Quantité") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(120.dp)
            )
            val amount = quantity.toIntOrNull() ?: 0
            OutlinedButton(
                onClick = { medicineViewModel.updateStock(currentMedicine.id, -amount) },
                enabled = amount > 0
            ) {
                Text("Retirer")
            }
            Button(
                onClick = { medicineViewModel.updateStock(currentMedicine.id, amount) },
                enabled = amount > 0
            ) {
                Text("Ajouter")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = { confirmDelete = true }) {
            Text("Supprimer ce médicament", color = MaterialTheme.colorScheme.error)
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

/** La quantite saisie survit a une rotation d'ecran. */
@Composable
private fun rememberSaveableQuantity() =
    androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("1") }

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
