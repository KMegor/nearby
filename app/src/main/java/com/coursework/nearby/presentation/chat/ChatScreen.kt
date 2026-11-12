package com.coursework.nearby.presentation.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coursework.nearby.R
import com.coursework.nearby.domain.model.Message
import com.coursework.nearby.domain.validation.MessageError
import com.coursework.nearby.domain.validation.MessageValidator
import com.coursework.nearby.presentation.common.Avatar
import com.coursework.nearby.presentation.common.messageRes
import com.coursework.nearby.presentation.common.timeLabel
import com.coursework.nearby.presentation.connections.BlockDialog

@Composable
fun ChatScreen(onBack: () -> Unit, viewModel: ChatViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showBlockDialog by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(state.isClosed) { if (state.isClosed) onBack() }
    // Новое сообщение — прокрутка к нему (список перевёрнут, индекс 0 — самое новое).
    LaunchedEffect(state.messages.lastOrNull()?.id) { listState.animateScrollToItem(0) }

    val partner = state.partner
    Column(Modifier.fillMaxSize().imePadding()) {
        ChatHeader(
            name = partner?.name.orEmpty(),
            avatarUrl = partner?.avatarUrl,
            onBack = onBack,
            onBlock = { showBlockDialog = true },
        )
        HorizontalDivider()

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            state = listState,
            reverseLayout = true,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            // Список перевёрнут: сообщения прижаты к полю ввода, как в мессенджерах.
            verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.Bottom),
        ) {
            items(state.messages.asReversed(), key = { it.id }) { message -> MessageBubble(message) }
            if (state.hasOlder) {
                item(key = "older") {
                    TextButton(
                        onClick = viewModel::onLoadOlder,
                        enabled = !state.isLoadingOlder,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.chat_load_older)) }
                }
            }
            if (state.messages.isEmpty()) {
                item(key = "empty") {
                    Text(
                        stringResource(R.string.chat_empty),
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        state.error?.let { error ->
            TextButton(onClick = viewModel::onErrorShown, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(error.messageRes()), color = MaterialTheme.colorScheme.error)
            }
        }

        if (partner?.canSend == false) {
            // Собеседник отстранён или переписка заморожена; причина не раскрывается.
            Text(
                stringResource(R.string.chat_cannot_send),
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        } else {
            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.Bottom) {
                OutlinedTextField(
                    value = state.input,
                    onValueChange = viewModel::onInputChange,
                    modifier = Modifier.weight(1f).testTag("messageInput"),
                    placeholder = { Text(stringResource(R.string.chat_placeholder)) },
                    maxLines = 4,
                    isError = state.inputError == MessageError.TOO_LONG,
                    supportingText = if (state.inputError == MessageError.TOO_LONG) {
                        { Text(stringResource(R.string.chat_error_too_long, MessageValidator.MAX_LENGTH)) }
                    } else null,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
                IconButton(
                    onClick = viewModel::onSend,
                    enabled = !state.isSending && state.input.isNotBlank(),
                    modifier = Modifier.testTag("send"),
                ) {
                    Icon(painterResource(R.drawable.ic_send), contentDescription = stringResource(R.string.chat_send))
                }
            }
        }
    }

    if (showBlockDialog && partner != null) {
        BlockDialog(
            name = partner.name,
            titleRes = R.string.block_chat_title,
            onDismiss = { showBlockDialog = false },
            onConfirm = { complaint ->
                showBlockDialog = false
                viewModel.onBlock(complaint)
            },
        )
    }
}

@Composable
private fun ChatHeader(name: String, avatarUrl: String?, onBack: () -> Unit, onBlock: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(painterResource(R.drawable.ic_back), contentDescription = stringResource(R.string.action_back))
        }
        Avatar(model = avatarUrl, name = name, size = 40.dp)
        Text(name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).padding(start = 12.dp))
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(painterResource(R.drawable.ic_more), contentDescription = stringResource(R.string.chat_menu))
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.chat_block)) },
                    onClick = {
                        menuOpen = false
                        onBlock()
                    },
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(message: Message) {
    val mine = message.isMine
    Box(Modifier.fillMaxWidth(), contentAlignment = if (mine) Alignment.CenterEnd else Alignment.CenterStart) {
        Column(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .background(
                    color = if (mine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(
                        topStart = 16.dp, topEnd = 16.dp,
                        bottomStart = if (mine) 16.dp else 4.dp, bottomEnd = if (mine) 4.dp else 16.dp,
                    ),
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.End,
        ) {
            val textColor = if (mine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            Text(message.body, color = textColor, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.align(Alignment.Start))
            Text(timeLabel(message.createdAt), color = textColor.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
        }
    }
}
