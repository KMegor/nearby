package com.coursework.nearby.presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coursework.nearby.R
import com.coursework.nearby.domain.validation.CredentialsFieldError
import com.coursework.nearby.domain.validation.CredentialsValidator
import com.coursework.nearby.presentation.common.messageRes

@Composable
fun AuthScreen(modifier: Modifier = Modifier, viewModel: AuthViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
        Text(
            stringResource(R.string.auth_tagline),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))

        val fieldModifier = Modifier.widthIn(max = 420.dp).fillMaxWidth()
        OutlinedTextField(
            value = state.email,
            onValueChange = viewModel::onEmailChange,
            modifier = fieldModifier.testTag("email"),
            label = { Text(stringResource(R.string.auth_email)) },
            singleLine = true,
            enabled = !state.isLoading,
            isError = CredentialsFieldError.EMAIL_INVALID in state.fieldErrors,
            supportingText = if (CredentialsFieldError.EMAIL_INVALID in state.fieldErrors) {
                { Text(stringResource(R.string.auth_error_email)) }
            } else null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        )
        OutlinedTextField(
            value = state.password,
            onValueChange = viewModel::onPasswordChange,
            modifier = fieldModifier.testTag("password"),
            label = { Text(stringResource(R.string.auth_password)) },
            singleLine = true,
            enabled = !state.isLoading,
            isError = CredentialsFieldError.PASSWORD_TOO_SHORT in state.fieldErrors,
            supportingText = if (CredentialsFieldError.PASSWORD_TOO_SHORT in state.fieldErrors) {
                { Text(stringResource(R.string.auth_error_password, CredentialsValidator.MIN_PASSWORD_LENGTH)) }
            } else null,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { viewModel.onSubmit() }),
        )

        state.error?.let {
            Text(stringResource(it.messageRes()), color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        }

        Button(onClick = viewModel::onSubmit, enabled = !state.isLoading, modifier = fieldModifier.testTag("submit")) {
            if (state.isLoading) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text(stringResource(if (state.isSignUp) R.string.auth_sign_up else R.string.auth_sign_in))
            }
        }
        TextButton(onClick = viewModel::onToggleMode, enabled = !state.isLoading, modifier = Modifier.testTag("toggle")) {
            Text(stringResource(if (state.isSignUp) R.string.auth_to_sign_in else R.string.auth_to_sign_up))
        }
    }
}
