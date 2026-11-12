package com.coursework.nearby.presentation.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.coursework.nearby.R
import com.coursework.nearby.domain.model.NearbyUser
import com.coursework.nearby.domain.model.Relation
import com.coursework.nearby.presentation.common.Avatar
import com.coursework.nearby.presentation.common.PrimaryButton
import com.coursework.nearby.presentation.common.ageLabel
import com.coursework.nearby.presentation.common.hoursUntil

/** Карточка человека с карты: профиль и действие, доступное при текущем отношении. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UserCard(
    user: NearbyUser,
    isSending: Boolean,
    onSendRequest: () -> Unit,
    onOpenChat: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Avatar(model = user.avatarUrl, name = user.name, size = 84.dp, ring = true)
            Column {
                Text(user.name, style = MaterialTheme.typography.headlineSmall)
                Text(ageLabel(user.age), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (user.bio.isNotEmpty()) Text(user.bio, style = MaterialTheme.typography.bodyLarge)
        if (user.interests.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                user.interests.forEach { SuggestionChip(onClick = {}, label = { Text(it) }) }
            }
        }

        when (user.relation) {
            Relation.NONE -> PrimaryButton(
                text = stringResource(R.string.card_start_chat),
                onClick = onSendRequest,
                loading = isSending,
                modifier = Modifier.fillMaxWidth(),
            )
            Relation.OUTGOING -> PrimaryButton(
                text = stringResource(R.string.card_request_sent),
                onClick = {},
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
            )
            Relation.COOLDOWN -> PrimaryButton(
                text = stringResource(R.string.card_retry_in, hoursUntil(user.retryAt)),
                onClick = {},
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
            )
            Relation.INCOMING -> StatusText(stringResource(R.string.card_incoming))
            Relation.CHAT -> PrimaryButton(
                text = stringResource(R.string.card_open_chat),
                onClick = { user.connectionId?.let(onOpenChat) },
                modifier = Modifier.fillMaxWidth(),
            )
            Relation.BLOCKED_BY_ME -> Unit
        }
    }
}

@Composable
private fun StatusText(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
