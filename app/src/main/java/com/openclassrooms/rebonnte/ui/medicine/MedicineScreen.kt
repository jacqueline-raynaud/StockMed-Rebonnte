package com.openclassrooms.rebonnte.ui.medicine

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.openclassrooms.rebonnte.R
import com.openclassrooms.rebonnte.ui.component.ErrorState
import com.openclassrooms.rebonnte.ui.component.LoadingState
import com.openclassrooms.rebonnte.ui.component.MedicineItem
import com.openclassrooms.rebonnte.ui.model.MedicineUi
import com.openclassrooms.rebonnte.ui.theme.RebonnteTheme

@Composable
fun MedicineScreen(
    viewModel: MedicineViewModel,
    onMedicineClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    MedicineContent(
        medicines = uiState.medicines,
        isLoading = uiState.isLoading,
        errorMessage = uiState.errorMessage,
        onMedicineClick = onMedicineClick,
        modifier = modifier
    )
}

@Composable
fun MedicineContent(
    medicines: List<MedicineUi>,
    onMedicineClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    @StringRes errorMessage: Int? = null
) {
    if (isLoading) {
        LoadingState(modifier)
        return
    }
    if (errorMessage != null) {
        ErrorState(errorMessage, modifier)
        return
    }

    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(
            items = medicines,
            key = { it.id },
            contentType = { CONTENT_TYPE_MEDICINE }
        ) { medicine ->
            MedicineItem(
                medicine = medicine,
                onClick = { onMedicineClick(medicine.id) }
            )
        }
    }
}

private const val CONTENT_TYPE_MEDICINE = "medicine"

@Preview(showBackground = true)
@Composable
private fun MedicineContentPreview() {
    RebonnteTheme {
        MedicineContent(
            medicines = listOf(
                MedicineUi(
                    id = "1",
                    name = "Doliprane 1000 mg",
                    stock = 42,
                    aisleId = "standard",
                    locationName = "Stockage standard"
                ),
                MedicineUi(
                    id = "2",
                    name = "Insuline",
                    stock = 3,
                    aisleId = "cold",
                    locationName = "Stockage froid"
                ),
                MedicineUi(
                    id = "3",
                    name = "Morphine",
                    stock = 0,
                    aisleId = "secured",
                    locationName = null
                )
            ),
            onMedicineClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MedicineContentLoadingPreview() {
    RebonnteTheme {
        MedicineContent(medicines = emptyList(), onMedicineClick = {}, isLoading = true)
    }
}

@Preview(showBackground = true)
@Composable
private fun MedicineContentErrorPreview() {
    RebonnteTheme {
        MedicineContent(
            medicines = emptyList(),
            onMedicineClick = {},
            errorMessage = R.string.error_permission
        )
    }
}
