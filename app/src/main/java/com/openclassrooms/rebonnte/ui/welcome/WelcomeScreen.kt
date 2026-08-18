package com.openclassrooms.rebonnte.ui.welcome

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.openclassrooms.rebonnte.R
import com.openclassrooms.rebonnte.ui.model.UserUi
import com.openclassrooms.rebonnte.ui.theme.RebonnteTheme

/**
 * allows the username to be validated if the previous user did not log out
 */
@Composable
fun WelcomeScreen(
    user: UserUi,
    onContinue: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    onDeleteAccount: (String) -> Unit = {},
    isDeletingAccount: Boolean = false,
    @StringRes deleteAccountError: Int? = null,
    onDeleteAccountErrorShown: () -> Unit = {}
) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    // if error re open windows
    LaunchedEffect(deleteAccountError) {
        if (deleteAccountError != null) confirmDelete = true
    }

    if (confirmDelete) {
        DeleteAccountDialog(
            isDeleting = isDeletingAccount,
            errorMessage = deleteAccountError,
            onConfirm = onDeleteAccount,
            onDismiss = {
                confirmDelete = false
                onDeleteAccountErrorShown()
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.welcome_greeting, user.displayName),
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = user.email,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(32.dp))
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.welcome_notice),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(16.dp)
            )
        }

        Spacer(Modifier.height(32.dp))
        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.welcome_continue))
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_sign_out))
        }

        Spacer(Modifier.height(24.dp))
        TextButton(onClick = { confirmDelete = true }) {
            Text(
                text = stringResource(R.string.account_delete),
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WelcomeScreenPreview() {
    RebonnteTheme {
        WelcomeScreen(
            user = UserUi(email = "operateur@rebonnte.fr", displayName = "Jacqueline"),
            onContinue = {},
            onSignOut = {}
        )
    }
}


@Composable
private fun DeleteAccountDialog(
    isDeleting: Boolean,
    @StringRes errorMessage: Int?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var password by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        title = { Text(stringResource(R.string.account_delete_title)) },
        text = {
            Column {
                Text(stringResource(R.string.account_delete_message))
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.account_delete_history_notice),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.account_delete_password)) },
                    singleLine = true,
                    enabled = !isDeleting,
                    isError = errorMessage != null,
                    supportingText = { errorMessage?.let { Text(stringResource(it)) } },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(password) },
                // Un mot de passe vide serait refuse par Firebase apres un
                // aller-retour reseau inutile.
                enabled = !isDeleting && password.isNotEmpty()
            ) {
                Text(
                    text = stringResource(R.string.action_delete),
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isDeleting) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun DeleteAccountDialogPreview() {
    RebonnteTheme {
        DeleteAccountDialog(
            isDeleting = false,
            errorMessage = R.string.auth_error_bad_credentials,
            onConfirm = {},
            onDismiss = {}
        )
    }
}
