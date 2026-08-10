package com.openclassrooms.rebonnte.ui.medicine

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicineFormScreen(
    viewModel: MedicineFormViewModel,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val aisles by viewModel.aisles.collectAsState()
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onSaved()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = state.name,
            onValueChange = viewModel::onNameChange,
            label = { Text("Nom du médicament") },
            singleLine = true,
            isError = state.nameError != null,
            supportingText = { state.nameError?.let { Text(it) } },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))

        // Liste deroulante alimentee par les emplacements existants : on ne
        // saisit pas un rayon librement, on en choisit un.
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = aisles.firstOrNull { it.id == state.aisleId }?.name.orEmpty(),
                onValueChange = {},
                readOnly = true,
                label = { Text("Emplacement de stockage") },
                isError = state.aisleError != null,
                supportingText = { state.aisleError?.let { Text(it) } },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor(),
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                aisles.forEach { aisle ->
                    DropdownMenuItem(
                        text = { Text(aisle.name) },
                        onClick = {
                            viewModel.onAisleChange(aisle.id)
                            expanded = false
                        }
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = state.stock,
            onValueChange = viewModel::onStockChange,
            label = { Text("Quantité initiale") },
            singleLine = true,
            isError = state.stockError != null,
            supportingText = { state.stockError?.let { Text(it) } },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = viewModel::submit,
            enabled = !state.isSubmitting,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Créer le médicament")
        }

        if (aisles.isEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Aucun emplacement de stockage n'est disponible. " +
                    "Créez-en un depuis l'onglet des rayons.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
