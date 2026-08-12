package com.openclassrooms.rebonnte.ui.auth

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

        if (isSignUp) {
            OutlinedTextField(
                value = state.displayName,
                onValueChange = onDisplayNameChange,
                label = { Text(stringResource(R.string.auth_field_name)) },
                singleLine = true,
                isError = state.displayNameError != null,
                supportingText = { state.displayNameError?.let { Text(stringResource(it)) } },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
        }

        OutlinedTextField(
            value = state.email,
            onValueChange = onEmailChange,
            label = { Text(stringResource(R.string.auth_field_email)) },
            singleLine = true,
            isError = state.emailError != null,
            supportingText = { state.emailError?.let { Text(stringResource(it)) } },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = state.password,
            onValueChange = onPasswordChange,
            label = { Text(stringResource(R.string.auth_field_password)) },
            singleLine = true,
            isError = state.passwordError != null,
            supportingText = { state.passwordError?.let { Text(stringResource(it)) } },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            modifier = Modifier.fillMaxWidth()
        )

        state.formError?.let { error ->
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(error),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onSubmit,
            enabled = !state.isSubmitting,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.height(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(
                    stringResource(
                        if (isSignUp) {
                            R.string.auth_action_sign_up
                        } else {
                            R.string.auth_action_sign_in
                        }
                    )
                )
            }
        }

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

/** Le cas qui compte le plus : les erreurs de saisie affichees. */
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
