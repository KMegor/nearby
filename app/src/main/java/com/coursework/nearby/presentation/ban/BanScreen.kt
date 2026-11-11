package com.coursework.nearby.presentation.ban

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.coursework.nearby.R
import com.coursework.nearby.domain.model.AppState
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** Единственный экран, доступный отстранённому пользователю. */
@Composable
fun BanScreen(state: AppState.Banned, onCheckAgain: () -> Unit, onSignOut: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.ban_title), style = MaterialTheme.typography.headlineSmall)
        Text(
            text = if (state.permanent) {
                stringResource(R.string.ban_permanent)
            } else {
                val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG, FormatStyle.SHORT).withLocale(Locale.forLanguageTag("ru"))
                stringResource(R.string.ban_until, formatter.format(state.until.atZone(ZoneId.systemDefault())))
            },
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        if (!state.permanent) {
            Button(onClick = onCheckAgain) { Text(stringResource(R.string.ban_check_again)) }
        }
        TextButton(onClick = onSignOut) { Text(stringResource(R.string.action_sign_out)) }
    }
}
