package com.openclassrooms.rebonnte.ui.aisle

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.openclassrooms.rebonnte.data.model.AisleDto

@Composable
fun AisleScreen(
    viewModel: AisleViewModel,
    onAisleClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val aisles by viewModel.aisles.collectAsState()

    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(aisles, key = { it.id }) { aisle ->
            AisleItem(
                aisle = aisle,
                onClick = { onAisleClick(aisle.id) }
            )
        }
    }
}

@Composable
fun AisleItem(aisle: AisleDto, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = aisle.name, style = MaterialTheme.typography.bodyLarge)
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null
        )
    }
}
