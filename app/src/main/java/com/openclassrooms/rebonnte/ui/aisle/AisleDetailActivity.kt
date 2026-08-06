package com.openclassrooms.rebonnte.ui.aisle

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.openclassrooms.rebonnte.MainActivity
import com.openclassrooms.rebonnte.data.model.Medicine
import com.openclassrooms.rebonnte.ui.medicine.MedicineDetailActivity
import com.openclassrooms.rebonnte.ui.medicine.MedicineViewModel
import com.openclassrooms.rebonnte.ui.theme.RebonnteTheme

class AisleDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val name = intent.getStringExtra("nameAisle") ?: "Unknown"
        val viewModel = ViewModelProvider(MainActivity.mainActivity)[MedicineViewModel::class.java]
        setContent {
            RebonnteTheme {
                AisleDetailScreen(name, viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AisleDetailScreen(name: String, viewModel: MedicineViewModel) {
    val medicines by viewModel.medicines.collectAsState(initial = emptyList())
    val filteredMedicines = medicines.filter { it.nameAisle == name }
    val context = LocalContext.current

    Scaffold { paddingValues ->
        LazyColumn(
            contentPadding = paddingValues,
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredMedicines) { medicine ->
                MedicineItem(medicine = medicine, onClick = { name ->
                    val intent = Intent(context, MedicineDetailActivity::class.java).apply {
                        putExtra("nameMedicine", name)
                    }
                    context.startActivity(intent)
                })
            }
        }
    }
}

@Composable
fun MedicineItem(medicine: Medicine, onClick: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(medicine.name) }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(text = medicine.name, fontWeight = FontWeight.Bold)
            Text(text = "Stock: ${medicine.stock}", color = Color.Gray)
        }
        Icon(imageVector = Icons.Default.KeyboardArrowRight, contentDescription = "Arrow")
    }
}
