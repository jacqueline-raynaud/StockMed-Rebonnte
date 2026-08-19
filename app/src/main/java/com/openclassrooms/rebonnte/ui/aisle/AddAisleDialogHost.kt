package com.openclassrooms.rebonnte.ui.aisle

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
/**
 * La fenetre de creation d'emplacement, et sa fermeture.
 *
 * Elle ne se ferme pas au clic mais au succes : un nom refuse doit rester
 * affiche avec son message, sinon il disparaitrait avant d'avoir ete lu.
 */
@Composable
internal fun AddAisleDialogHost(
    aisleViewModel: AisleViewModel,
    onClose: () -> Unit
) {
    val newAisleError by aisleViewModel.newAisleError.collectAsState()
    val aisleCreated by aisleViewModel.aisleCreated.collectAsState()

    LaunchedEffect(aisleCreated) {
        if (aisleCreated) {
            onClose()
            aisleViewModel.aisleCreatedShown()
        }
    }

    AddAisleDialog(
        onDismiss = {
            onClose()
            aisleViewModel.clearNewAisleError()
        },
        onConfirm = aisleViewModel::addAisle,
        errorMessage = newAisleError,
        onNameChange = aisleViewModel::clearNewAisleError
    )
}
