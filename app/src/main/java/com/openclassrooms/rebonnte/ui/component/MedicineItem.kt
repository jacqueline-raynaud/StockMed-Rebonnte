package com.openclassrooms.rebonnte.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.openclassrooms.rebonnte.R
import com.openclassrooms.rebonnte.ui.model.MedicineUi

/**
 * Ligne de liste d'un medicament.
 *
 * Il en existait deux versions incompatibles, dans MedicineScreen et dans
 * AisleDetailActivity, avec des signatures et des styles differents. Celle-ci
 * les remplace.
 *
 * Le chevron n'a pas de contentDescription : c'est une decoration, le libelle
 * de la ligne porte deja l'information. Lui en donner une ferait doublon a
 * l'oreille sous TalkBack.
 */
@Composable
fun MedicineItem(
    medicine: MedicineUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = medicine.name,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = stringResource(R.string.medicine_stock, medicine.stock),
                style = MaterialTheme.typography.bodyMedium,
                // Couleur du theme plutot que Color.Gray en dur : illisible en
                // mode sombre.
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null
        )
    }
}
