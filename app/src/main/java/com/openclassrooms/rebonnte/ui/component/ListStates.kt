package com.openclassrooms.rebonnte.ui.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.openclassrooms.rebonnte.R
import com.openclassrooms.rebonnte.ui.UiMessage

/**
 * Les deux etats qu'une liste vide ne savait pas exprimer.
 *
 * Un `LazyColumn` vide se lisait « il n'y a rien en stock », que les donnees
 * soient en route ou que la lecture ait echoue. Sur un stock de medicaments,
 * confondre « rien » et « je ne sais pas » n'est pas anodin.
 */
@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

/**
 * Un geste qui n'a pas abouti, annonce par une fenetre a valider.
 *
 * Un snackbar ne convient pas ici : il s'efface tout seul, en bas de l'ecran,
 * et rien ne garantit qu'il ait ete lu. Pour un mouvement de stock refuse,
 * l'operateur doit **acquitter** le message — sinon il repart en croyant son
 * retrait enregistre, et l'ecart n'apparaitra qu'a l'inventaire.
 *
 * Le snackbar reste pour les confirmations : une operation reussie n'a pas
 * besoin d'etre validee.
 */
@Composable
fun ActionErrorDialog(message: UiMessage, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text(stringResource(R.string.action_failed_title)) },
        text = {
            Text(stringResource(message.res, *message.args.toTypedArray()))
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_ok)) }
        }
    )
}

@Composable
fun ErrorState(@StringRes message: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(message),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}
