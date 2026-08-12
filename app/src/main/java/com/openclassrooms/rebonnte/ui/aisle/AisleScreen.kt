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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.openclassrooms.rebonnte.ui.model.AisleUi
import com.openclassrooms.rebonnte.ui.theme.RebonnteTheme

/**
 * Partie « avec etat » : elle connait le ViewModel et ne fait que lui prendre
 * son etat.
 */
@Composable
fun AisleScreen(
    viewModel: AisleViewModel,
    onAisleClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    AisleContent(
        aisles = uiState.aisles,
        onAisleClick = onAisleClick,
        modifier = modifier
    )
}

/**
 * Partie « sans etat » : des donnees et des lambdas, rien d'autre.
 *
 * C'est ce qui la rend previsualisable — un ViewModel demanderait Hilt, donc
 * une application lancee. C'est aussi ce qui la rend testable sans emulateur.
 */
@Composable
fun AisleContent(
    aisles: List<AisleUi>,
    onAisleClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(
            items = aisles,
            key = { it.id },
            // Toutes les lignes ont la meme mise en page : en le declarant,
            // Compose reutilise les composables au defilement au lieu d'en
            // reconstruire.
            contentType = { CONTENT_TYPE_AISLE }
        ) { aisle ->
            AisleItem(
                aisle = aisle,
                onClick = { onAisleClick(aisle.id) }
            )
        }
    }
}

private const val CONTENT_TYPE_AISLE = "aisle"

@Composable
fun AisleItem(aisle: AisleUi, onClick: () -> Unit) {
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

@Preview(showBackground = true)
@Composable
private fun AisleContentPreview() {
    RebonnteTheme {
        AisleContent(
            aisles = listOf(
                AisleUi(id = "standard", name = "Stockage standard"),
                AisleUi(id = "cold", name = "Stockage froid"),
                AisleUi(id = "secured", name = "Stockage securise")
            ),
            onAisleClick = {}
        )
    }
}

/** Le cas vide se regarde aussi : c'est ce que voit un premier lancement. */
@Preview(showBackground = true)
@Composable
private fun AisleContentEmptyPreview() {
    RebonnteTheme {
        AisleContent(aisles = emptyList(), onAisleClick = {})
    }
}
