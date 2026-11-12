package com.coursework.nearby.presentation.connections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coursework.nearby.R
import com.coursework.nearby.domain.model.ComplaintReason
import com.coursework.nearby.domain.model.ConnectionSummary
import com.coursework.nearby.domain.model.Relation
import com.coursework.nearby.domain.model.RequestResponse
import com.coursework.nearby.presentation.common.Avatar
import com.coursework.nearby.presentation.common.LoadingScreen
import com.coursework.nearby.presentation.common.ageLabel
import com.coursework.nearby.presentation.common.hoursUntil
import com.coursework.nearby.presentation.common.messageRes

/** Вкладка «Запросы»: входящие с ответом и исходящие со статусом. */
@Composable
fun RequestsScreen(state: ConnectionsUiState, viewModel: ConnectionsViewModel) {
    var blockTarget by rememberSaveable { mutableStateOf<String?>(null) }

    if (!state.isLoaded && state.error == null) {
        LoadingScreen()
        return
    }

    val incoming = state.lists.incoming
    val outgoing = state.lists.outgoing

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        state.error?.let { error ->
            item(key = "error") {
                ElevatedCard(onClick = viewModel::onErrorShown, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(error.messageRes()), Modifier.padding(12.dp), color = MaterialTheme.colorScheme.error)
                }
            }
        }
        if (incoming.isEmpty() && outgoing.isEmpty()) {
            item(key = "empty") {
                Text(
                    stringResource(R.string.requests_empty),
                    modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
        if (incoming.isNotEmpty()) {
            item(key = "incoming-title") { SectionTitle(stringResource(R.string.requests_incoming)) }
            items(incoming, key = { it.connectionId }) { request ->
                IncomingRequestCard(
                    request = request,
                    busy = request.connectionId in state.busy,
                    onAccept = { viewModel.onRespond(request.connectionId, RequestResponse.Accept) },
                    onReject = { viewModel.onRespond(request.connectionId, RequestResponse.Reject) },
                    onBlock = { blockTarget = request.connectionId },
                )
            }
        }
        if (outgoing.isNotEmpty()) {
            item(key = "outgoing-title") { SectionTitle(stringResource(R.string.requests_outgoing)) }
            items(outgoing, key = { it.connectionId }) { request -> OutgoingRequestCard(request) }
        }
    }

    val target = incoming.firstOrNull { it.connectionId == blockTarget }
    if (target != null) {
        BlockDialog(
            name = target.name,
            titleRes = R.string.block_reject_title,
            onDismiss = { blockTarget = null },
            onConfirm = { complaint ->
                blockTarget = null
                viewModel.onRespond(target.connectionId, RequestResponse.Block(complaint))
            },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp))
}

@Composable
private fun PersonHeader(person: ConnectionSummary) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Avatar(model = person.avatarUrl, name = person.name, size = 48.dp)
        Column(Modifier.weight(1f)) {
            Text("${person.name}, ${ageLabel(person.age)}", style = MaterialTheme.typography.titleMedium)
            if (person.bio.isNotEmpty()) {
                Text(
                    person.bio,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun IncomingRequestCard(
    request: ConnectionSummary,
    busy: Boolean,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onBlock: () -> Unit,
) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PersonHeader(request)
            if (request.interests.isNotEmpty()) {
                Text(
                    request.interests.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAccept, enabled = !busy, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.requests_accept))
                }
                OutlinedButton(onClick = onReject, enabled = !busy, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.requests_reject))
                }
            }
            // Условие G4: блокировка доступна, начиная со второго отказа этому отправителю.
            if (request.canBlockOnReject) {
                TextButton(onClick = onBlock, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.requests_reject_and_block), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun OutgoingRequestCard(request: ConnectionSummary) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PersonHeader(request)
            Text(
                text = if (request.relation == Relation.COOLDOWN) {
                    stringResource(R.string.requests_status_rejected, hoursUntil(request.retryAt))
                } else {
                    stringResource(R.string.requests_status_waiting)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Подтверждение блокировки с необязательной жалобой (событие E10 только вместе с E7). */
@Composable
fun BlockDialog(name: String, titleRes: Int, onDismiss: () -> Unit, onConfirm: (ComplaintReason?) -> Unit) {
    var complain by rememberSaveable { mutableStateOf(false) }
    var reason by rememberSaveable { mutableStateOf(ComplaintReason.SPAM) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(titleRes)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.block_text, name))
                Row(
                    Modifier.fillMaxWidth().toggleable(value = complain, role = Role.Checkbox, onValueChange = { complain = it }),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = complain, onCheckedChange = null, modifier = Modifier.padding(12.dp))
                    Text(stringResource(R.string.block_complain))
                }
                if (complain) {
                    ComplaintReason.entries.forEach { option ->
                        Row(
                            Modifier.fillMaxWidth()
                                .selectable(selected = reason == option, role = Role.RadioButton, onClick = { reason = option })
                                .padding(start = 24.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = reason == option, onClick = null, modifier = Modifier.padding(12.dp))
                            Text(
                                stringResource(
                                    when (option) {
                                        ComplaintReason.SPAM -> R.string.complaint_spam
                                        ComplaintReason.ABUSE -> R.string.complaint_abuse
                                        ComplaintReason.OTHER -> R.string.complaint_other
                                    },
                                ),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(reason.takeIf { complain }) }) {
                Text(stringResource(R.string.block_confirm), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
