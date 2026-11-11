package com.coursework.nearby.presentation.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coursework.nearby.R
import com.coursework.nearby.domain.validation.ProfileFieldError
import com.coursework.nearby.domain.validation.ProfileValidator
import com.coursework.nearby.presentation.common.Avatar
import com.coursework.nearby.presentation.common.messageRes

/**
 * Форма профиля. [isSetup] — первое заполнение после регистрации: после сохранения
 * приложение само перейдёт к основным вкладкам; иначе вызывается [onDone].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileEditScreen(
    isSetup: Boolean,
    onDone: () -> Unit,
    onSignOut: (() -> Unit)?,
    modifier: Modifier = Modifier,
    viewModel: ProfileEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        viewModel.onAvatarPicked(uri?.toString())
    }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved && !isSetup) onDone()
    }

    Column(
        modifier = modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            stringResource(if (isSetup) R.string.profile_setup_title else R.string.profile_edit_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        if (isSetup) {
            Text(
                stringResource(R.string.profile_setup_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Avatar(model = state.pendingAvatarUri ?: state.avatarUrl, name = state.name, size = 88.dp)
            OutlinedButton(
                onClick = {
                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                enabled = !state.isSaving,
            ) {
                val hasPhoto = state.pendingAvatarUri != null || state.avatarUrl != null
                Text(stringResource(if (hasPhoto) R.string.profile_change_photo else R.string.profile_choose_photo))
            }
        }

        val errors = state.fieldErrors
        OutlinedTextField(
            value = state.name,
            onValueChange = viewModel::onNameChange,
            modifier = Modifier.fillMaxWidth().testTag("name"),
            label = { Text(stringResource(R.string.profile_name)) },
            singleLine = true,
            enabled = !state.isSaving,
            isError = ProfileFieldError.NAME_EMPTY in errors || ProfileFieldError.NAME_TOO_LONG in errors,
            supportingText = when {
                ProfileFieldError.NAME_EMPTY in errors -> ({ Text(stringResource(R.string.profile_error_name_empty)) })
                ProfileFieldError.NAME_TOO_LONG in errors ->
                    ({ Text(stringResource(R.string.profile_error_name_long, ProfileValidator.MAX_NAME_LENGTH)) })
                else -> null
            },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
        )
        OutlinedTextField(
            value = state.birthYear,
            onValueChange = viewModel::onBirthYearChange,
            modifier = Modifier.fillMaxWidth().testTag("birthYear"),
            label = { Text(stringResource(R.string.profile_birth_year)) },
            singleLine = true,
            enabled = !state.isSaving,
            isError = ProfileFieldError.BIRTH_YEAR_INVALID in errors || ProfileFieldError.UNDERAGE in errors,
            supportingText = when {
                ProfileFieldError.BIRTH_YEAR_INVALID in errors -> ({ Text(stringResource(R.string.profile_error_year)) })
                ProfileFieldError.UNDERAGE in errors ->
                    ({ Text(stringResource(R.string.profile_error_underage, ProfileValidator.MIN_AGE)) })
                else -> null
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        )
        OutlinedTextField(
            value = state.bio,
            onValueChange = viewModel::onBioChange,
            modifier = Modifier.fillMaxWidth().testTag("bio"),
            label = { Text(stringResource(R.string.profile_bio)) },
            minLines = 3,
            enabled = !state.isSaving,
            isError = ProfileFieldError.BIO_TOO_LONG in errors,
            supportingText = {
                Text(
                    if (ProfileFieldError.BIO_TOO_LONG in errors) {
                        stringResource(R.string.profile_error_bio_long, ProfileValidator.MAX_BIO_LENGTH)
                    } else {
                        "${state.bio.trim().length} / ${ProfileValidator.MAX_BIO_LENGTH}"
                    },
                )
            },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )

        if (state.allInterests.isNotEmpty()) {
            Text(
                stringResource(R.string.profile_interests, ProfileValidator.MAX_INTERESTS),
                style = MaterialTheme.typography.titleSmall,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.allInterests.forEach { interest ->
                    FilterChip(
                        selected = interest.id in state.selectedInterests,
                        onClick = { viewModel.onInterestToggle(interest.id) },
                        label = { Text(interest.name) },
                        enabled = !state.isSaving,
                    )
                }
            }
        }

        state.error?.let { Text(stringResource(it.messageRes()), color = MaterialTheme.colorScheme.error) }

        Button(onClick = viewModel::onSave, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth().testTag("save")) {
            if (state.isSaving) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text(stringResource(R.string.action_save))
            }
        }
        if (!isSetup) {
            TextButton(onClick = onDone, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_back))
            }
        }
        if (onSignOut != null) {
            TextButton(onClick = onSignOut, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_sign_out))
            }
        }
    }
}
