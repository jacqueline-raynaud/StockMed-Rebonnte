package com.openclassrooms.rebonnte.ui.auth

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.openclassrooms.rebonnte.R
import com.openclassrooms.rebonnte.ui.theme.RebonnteTheme

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    AuthContent(
        state = state,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onDisplayNameChange = viewModel::onDisplayNameChange,
        onSubmit = viewModel::submit,
        onToggleMode = viewModel::toggleMode,
        modifier = modifier
    )
}

@Composable
fun AuthContent(
    state: AuthUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onToggleMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSignUp = state.mode == AuthMode.SIGN_UP

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(
                if (isSignUp) R.string.auth_title_sign_up else R.string.auth_title_sign_in
            ),
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.height(24.dp))

        // Le nom n'existe qu'a la creation : on ne le redemande pas a chaque
        // connexion.
        if (isSignUp) {
            AuthField(
                value = state.displayName,
                onValueChange = onDisplayNameChange,
                label = R.string.auth_field_name,
                error = state.displayNameError,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
            Spacer(Modifier.height(8.dp))
        }

        AuthField(
            value = state.email,
            onValueChange = onEmailChange,
            label = R.string.auth_field_email,
            error = state.emailError,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            )
        )
        Spacer(Modifier.height(8.dp))

        AuthField(
            value = state.password,
            onValueChange = onPasswordChange,
            label = R.string.auth_field_password,
            error = state.passwordError,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            visualTransformation = PasswordVisualTransformation()
        )

        // Erreur du formulaire entier — un refus du serveur — par opposition aux
        // erreurs de champ, qui s'affichent sous le champ fautif.
        state.formError?.let { error ->
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(error),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(Modifier.height(24.dp))
        SubmitButton(
            isSignUp = isSignUp,
            isSubmitting = state.isSubmitting,
            onSubmit = onSubmit
        )

        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onToggleMode, enabled = !state.isSubmitting) {
            Text(
                stringResource(
                    if (isSignUp) {
                        R.string.auth_switch_to_sign_in
                    } else {
                        R.string.auth_switch_to_sign_up
                    }
                )
            )
        }
    }
}

/**
 * Un champ du formulaire, avec son libelle et son erreur.
 *
 * Les trois champs ne different que par leur libelle, leur clavier et leur
 * masquage : le reste — ligne unique, largeur, mise en erreur, message de
 * support — etait recopie a l'identique trois fois.
 */
@Composable
private fun AuthField(
    value: String,
    onValueChange: (String) -> Unit,
    @StringRes label: Int,
    @StringRes error: Int?,
    keyboardOptions: KeyboardOptions,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(label)) },
        singleLine = true,
        isError = error != null,
        supportingText = { error?.let { Text(stringResource(it)) } },
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        modifier = Modifier.fillMaxWidth()
    )
}

/** Le bouton porte l'attente : desactive, et son libelle cede la place. */
@Composable
private fun SubmitButton(
    isSignUp: Boolean,
    isSubmitting: Boolean,
    onSubmit: () -> Unit
) {
    Button(
        onClick = onSubmit,
        enabled = !isSubmitting,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isSubmitting) {
            CircularProgressIndicator(
                modifier = Modifier.height(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Text(
                stringResource(
                    if (isSignUp) R.string.auth_action_sign_up else R.string.auth_action_sign_in
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AuthContentSignInPreview() {
    RebonnteTheme {
        AuthContent(
            state = AuthUiState(email = "operateur@rebonnte.fr"),
            onEmailChange = {},
            onPasswordChange = {},
            onDisplayNameChange = {},
            onSubmit = {},
            onToggleMode = {}
        )
    }
}

/** preview avec les erreurs de affichées. */
@Preview(showBackground = true)
@Composable
private fun AuthContentErrorsPreview() {
    RebonnteTheme {
        AuthContent(
            state = AuthUiState(
                mode = AuthMode.SIGN_UP,
                email = "operateur",
                emailError = R.string.auth_error_email_invalid,
                passwordError = R.string.auth_error_password_too_short,
                displayNameError = R.string.auth_error_name_required,
                formError = R.string.auth_error_network
            ),
            onEmailChange = {},
            onPasswordChange = {},
            onDisplayNameChange = {},
            onSubmit = {},
            onToggleMode = {}
        )
    }
}
