package com.openclassrooms.rebonnte.ui.aisle

import androidx.annotation.StringRes
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.openclassrooms.rebonnte.R
import com.openclassrooms.rebonnte.ui.theme.RebonnteTheme

/**
 * Creation d'un emplacement de stockage.
 *
 * Remplace l'ancien bouton qui fabriquait « Aisle 2 », « Aisle 3 » : un
 * emplacement porte un nom choisi, pas un numero.
 *
 * La fenetre **ne se ferme pas d'elle-meme** : un nom refuse doit rester
 * affiche avec son erreur, sinon l'operateur perd sa saisie et ne sait pas
 * pourquoi rien ne s'est passe. C'est l'appelant qui la ferme, une fois la
 * creation aboutie.
 */
@Composable
fun AddAisleDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    @StringRes errorMessage: Int? = null,
    onNameChange: () -> Unit = {}
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.aisle_dialog_title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    // L'erreur ne survit pas a la correction : la garder
                    // affichee pendant que l'operateur retape serait un
                    // reproche permanent.
                    onNameChange()
                },
                label = { Text(stringResource(R.string.aisle_dialog_name)) },
                singleLine = true,
                isError = errorMessage != null,
                supportingText = { errorMessage?.let { Text(stringResource(it)) } }
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                // Un nom fait d'espaces ne passe deja pas ce garde : isNotBlank
                // est faux pour une suite d'espaces. La regle est verifiee une
                // seconde fois dans le ViewModel, ou elle ne depend pas de
                // l'affichage.
                enabled = name.isNotBlank()
            ) {
                Text(stringResource(R.string.action_create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

@Preview
@Composable
private fun AddAisleDialogPreview() {
    RebonnteTheme {
        AddAisleDialog(onDismiss = {}, onConfirm = {})
    }
}

/** Le cas qui compte : un nom deja pris, refuse sous le champ. */
@Preview
@Composable
private fun AddAisleDialogDuplicatePreview() {
    RebonnteTheme {
        AddAisleDialog(
            onDismiss = {},
            onConfirm = {},
            errorMessage = R.string.aisle_error_duplicate
        )
    }
}
