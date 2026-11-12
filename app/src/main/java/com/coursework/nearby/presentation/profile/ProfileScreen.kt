package com.coursework.nearby.presentation.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.coursework.nearby.R
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coursework.nearby.domain.model.Profile
import com.coursework.nearby.domain.model.ThemeMode
import com.coursework.nearby.presentation.common.PrimaryButton
import com.coursework.nearby.presentation.common.SecondaryButton
import com.coursework.nearby.presentation.theme.ThemeViewModel
import com.coursework.nearby.presentation.common.Avatar
import com.coursework.nearby.presentation.common.ageLabel
import java.time.Year

/** Вкладка «Профиль»: так пользователя видят другие. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    profile: Profile,
    onEdit: () -> Unit,
    onBlocked: () -> Unit,
    onSignOut: () -> Unit,
    themeViewModel: ThemeViewModel = hiltViewModel(),
) {
    val themeMode by themeViewModel.mode.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(12.dp))
        Avatar(model = profile.avatarUrl, name = profile.name, size = 132.dp, ring = true)
        Spacer(Modifier.height(4.dp))
        Text(profile.name, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.testTag("profileName"))
        Text(
            ageLabel(profile.age(Year.now().value)),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            profile.bio.ifEmpty { stringResource(R.string.profile_no_bio) },
            style = MaterialTheme.typography.bodyLarge,
            color = if (profile.bio.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        if (profile.interests.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                profile.interests.forEach { SuggestionChip(onClick = {}, label = { Text(it.name) }) }
            }
        }
        Spacer(Modifier.height(16.dp))
        PrimaryButton(stringResource(R.string.profile_edit), onClick = onEdit, modifier = Modifier.fillMaxWidth().testTag("editProfile"))
        SecondaryButton(stringResource(R.string.profile_blocked), onClick = onBlocked, modifier = Modifier.fillMaxWidth().testTag("blocked"))

        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.profile_theme),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            ThemeMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = themeMode == mode,
                    onClick = { themeViewModel.onModeChange(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size),
                ) {
                    Text(
                        stringResource(
                            when (mode) {
                                ThemeMode.DARK -> R.string.theme_dark
                                ThemeMode.LIGHT -> R.string.theme_light
                                ThemeMode.SYSTEM -> R.string.theme_system
                            },
                        ),
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        TextButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth().testTag("signOut")) {
            Text(stringResource(R.string.action_sign_out))
        }
    }
}
