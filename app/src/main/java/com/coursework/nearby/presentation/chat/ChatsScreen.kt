package com.coursework.nearby.presentation.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Badge
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coursework.nearby.R
import com.coursework.nearby.domain.model.ConnectionSummary
import com.coursework.nearby.presentation.common.Avatar
import com.coursework.nearby.presentation.common.LoadingScreen
import com.coursework.nearby.presentation.common.ScreenTitle
import com.coursework.nearby.presentation.common.messageRes
import com.coursework.nearby.presentation.common.timeLabel
import com.coursework.nearby.presentation.connections.ConnectionsUiState
import com.coursework.nearby.presentation.connections.ConnectionsViewModel

/** Вкладка «Чаты»: список чатов с последним сообщением и счётчиком непрочитанных. */
@Composable
fun ChatsScreen(state: ConnectionsUiState, viewModel: ConnectionsViewModel, onOpenChat: (String) -> Unit) {
    // При возврате из переписки список обновляется сразу, не дожидаясь очередного опроса.
    LaunchedEffect(Unit) { viewModel.onRefresh() }

    if (!state.isLoaded && state.error == null) {
        LoadingScreen()
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        item(key = "title") {
            ScreenTitle(stringResource(R.string.tab_chats), Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp))
        }
        state.error?.let { error ->
            item(key = "error") {
                Text(stringResource(error.messageRes()), Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error)
            }
        }
        if (state.lists.chats.isEmpty()) {
            item(key = "empty") {
                Text(
                    stringResource(R.string.chats_empty),
                    modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 64.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
        items(state.lists.chats, key = { it.connectionId }) { chat ->
            ChatRow(chat, onClick = { onOpenChat(chat.connectionId) })
            HorizontalDivider(Modifier.padding(start = 84.dp), color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun ChatRow(chat: ConnectionSummary, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Avatar(model = chat.avatarUrl, name = chat.name, size = 56.dp)
        Column(Modifier.weight(1f)) {
            Text(chat.name, style = MaterialTheme.typography.titleMedium)
            Text(
                chat.lastMessage ?: stringResource(R.string.chats_no_messages),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            chat.lastMessageAt?.let {
                Text(timeLabel(it), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (chat.unread > 0) Badge { Text(chat.unread.toString()) }
        }
    }
}
