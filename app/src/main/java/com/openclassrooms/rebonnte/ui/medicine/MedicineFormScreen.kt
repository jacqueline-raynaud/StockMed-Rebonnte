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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.openclassrooms.rebonnte.R
import com.openclassrooms.rebonnte.ui.model.AisleUi
import com.openclassrooms.rebonnte.ui.theme.RebonnteTheme

@Composable
fun MedicineFormScreen(
    viewModel: MedicineFormViewModel,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val aisles by viewModel.aisles.collectAsState()

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onSaved()
    }

    MedicineFormContent(
        state = state,
        aisles = aisles,
        onNameChange = viewModel::onNameChange,
        onAisleChange = viewModel::onAisleChange,
        onStockChange = viewModel::onStockChange,
        onSubmit = viewModel::submit,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicineFormContent(
    state: MedicineFormUiState,
    aisles: List<AisleUi>,
    onNameChange: (String) -> Unit,
    onAisleChange: (String) -> Unit,
    onStockChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = state.name,
            onValueChange = onNameChange,
            label = { Text(stringResource(R.string.form_medicine_name)) },
            singleLine = true,
            isError = state.nameError != null,
            supportingText = { state.nameError?.let { Text(stringResource(it)) } },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = aisles.firstOrNull { it.id == state.aisleId }?.name.orEmpty(),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.form_storage_location)) },
                isError = state.aisleError != null,
                supportingText = { state.aisleError?.let { Text(stringResource(it)) } },
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
                            onAisleChange(aisle.id)
                            expanded = false
                        }
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))


        if (!state.isEditing) {
            OutlinedTextField(
                value = state.stock,
                onValueChange = onStockChange,
                label = { Text(stringResource(R.string.form_initial_quantity)) },
                singleLine = true,
                isError = state.stockError != null,
                supportingText = { state.stockError?.let { Text(stringResource(it)) } },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onSubmit,
            enabled = !state.isSubmitting,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                stringResource(
                    if (state.isEditing) R.string.form_save else R.string.form_submit
                )
            )
        }

        state.submitError?.let { error ->
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(error),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        if (aisles.isEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.form_no_aisle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

private val previewAisles = listOf(
    AisleUi(id = "standard", name = "Stockage standard"),
    AisleUi(id = "cold", name = "Stockage froid"),
    AisleUi(id = "secured", name = "Stockage securise")
)

@Preview(showBackground = true)
@Composable
private fun MedicineFormContentPreview() {
    RebonnteTheme {
        MedicineFormContent(
            state = MedicineFormUiState(name = "Doliprane", stock = "20", aisleId = "standard"),
            aisles = previewAisles,
            onNameChange = {},
            onAisleChange = {},
            onStockChange = {},
            onSubmit = {}
        )
    }
}


@Preview(showBackground = true)
@Composable
private fun MedicineFormContentNoAislePreview() {
    RebonnteTheme {
        MedicineFormContent(
            state = MedicineFormUiState(aisleError = R.string.form_error_aisle_required),
            aisles = emptyList(),
            onNameChange = {},
            onAisleChange = {},
            onStockChange = {},
            onSubmit = {}
        )
    }
}
