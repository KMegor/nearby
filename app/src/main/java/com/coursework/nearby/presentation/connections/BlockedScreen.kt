package com.coursework.nearby.presentation.connections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.coursework.nearby.R
import com.coursework.nearby.presentation.common.Avatar
import com.coursework.nearby.presentation.common.messageRes

/** «Заблокированные»: единственное место, где заблокированного можно найти и разблокировать. */
@Composable
fun BlockedScreen(state: ConnectionsUiState, viewModel: ConnectionsViewModel, onBack: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "title") { Text(stringResource(R.string.blocked_title), style = MaterialTheme.typography.headlineSmall) }
        state.error?.let { error ->
            item(key = "error") {
                Text(stringResource(error.messageRes()), color = MaterialTheme.colorScheme.error)
            }
        }
        if (state.lists.blocked.isEmpty()) {
            item(key = "empty") {
                Text(
                    stringResource(R.string.blocked_empty),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
        items(state.lists.blocked, key = { it.connectionId }) { person ->
            ElevatedCard(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Avatar(model = person.avatarUrl, name = person.name, size = 48.dp)
                    Text(person.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    OutlinedButton(
                        onClick = { viewModel.onUnblock(person.connectionId) },
                        enabled = person.connectionId !in state.busy,
                    ) {
                        Text(stringResource(R.string.blocked_unblock))
                    }
                }
            }
        }
        item(key = "back") {
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.action_back)) }
        }
    }
}
