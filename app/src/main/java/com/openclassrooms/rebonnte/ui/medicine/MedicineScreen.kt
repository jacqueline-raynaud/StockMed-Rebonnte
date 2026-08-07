package com.openclassrooms.rebonnte.ui.medicine

import android.content.Context
import androidx.compose.runtime.Composable

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import com.openclassrooms.rebonnte.data.model.Medicine

@Composable
fun MedicineScreen(viewModel: MedicineViewModel = viewModel()) {
    val medicines by viewModel.medicines.collectAsState(initial = emptyList())
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(medicines, key = { it.id }) { medicine ->
            MedicineItem(medicine = medicine, onClick = {
                startDetailActivity(context, medicine.id)
            })
        }
    }
}

@Composable
fun MedicineItem(medicine: Medicine, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(text = medicine.name, style = MaterialTheme.typography.bodyLarge)
            Text(text = "Stock: ${medicine.stock}", style = MaterialTheme.typography.bodyMedium)
        }
        Icon(imageVector = Icons.Default.KeyboardArrowRight, contentDescription = "Arrow")
    }
}

private fun startDetailActivity(context: Context, medicineId: String) {
    val intent = Intent(context, MedicineDetailActivity::class.java).apply {
        putExtra(MedicineDetailActivity.EXTRA_MEDICINE_ID, medicineId)
    }
    context.startActivity(intent)
}
