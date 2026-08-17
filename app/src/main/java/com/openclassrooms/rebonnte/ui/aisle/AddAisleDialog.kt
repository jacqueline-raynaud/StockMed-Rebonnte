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
