package com.openclassrooms.rebonnte.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.openclassrooms.rebonnte.R
import com.openclassrooms.rebonnte.ui.theme.RebonnteTheme

/**
 * Bandeau permanent, et non un dialogue.
 *
 * Une coupure reseau peut durer : un dialogue se ferme et ne dit plus rien
 * trois minutes plus tard, alors que la situation, elle, n'a pas change. Le
 * bandeau reste tant que le reseau manque et disparait des son retour, sans
 * demander d'action.
 *
 * Il accompagne [OfflineContent], qui bloque l'ecran : le bandeau nomme la
 * cause en permanence, y compris apres un defilement.
 */
@Composable
fun OfflineBanner(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            // Decoratif : le texte a cote porte deja toute l'information, et
            // TalkBack le lira.
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onTertiaryContainer
        )
        Text(
            text = stringResource(R.string.offline_banner),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OfflineBannerPreview() {
    RebonnteTheme {
        OfflineBanner()
    }
}

/**
 * Ecran de blocage hors ligne.
 *
 * Decision metier : sans reseau, l'application n'affiche aucune donnee et
 * n'autorise aucune action. Deux raisons, tranchees avec le metier :
 *
 * - Les transactions Firestore, dont dependent les mouvements de stock, ne
 *   fonctionnent pas hors ligne. Laisser les boutons actifs promettrait des
 *   operations qui n'auraient pas lieu.
 * - Un comptage manuel effectue sur des chiffres possiblement perimes est pire
 *   que pas de comptage : il produit un ecart d'inventaire que personne ne sait
 *   ensuite expliquer.
 *
 * L'entreprise equipe ses operateurs et fournit la couverture reseau : le
 * hors-ligne est un incident, pas un mode de travail.
 */
@Composable
fun OfflineContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.offline_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.offline_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.offline_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OfflineContentPreview() {
    RebonnteTheme {
        Column {
            OfflineBanner()
            OfflineContent()
        }
    }
}
