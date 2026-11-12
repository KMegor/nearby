package com.coursework.nearby.domain.usecase

import com.coursework.nearby.domain.model.ComplaintReason
import com.coursework.nearby.domain.model.ConnectionSummary
import com.coursework.nearby.domain.model.Message
import com.coursework.nearby.domain.repository.ChatRepository
import com.coursework.nearby.domain.repository.ConnectionRepository
import com.coursework.nearby.domain.validation.MessageError
import com.coursework.nearby.domain.validation.MessageValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ObserveMessagesUseCase @Inject constructor(private val chat: ChatRepository) {
    operator fun invoke(connectionId: String): Flow<List<Message>> = chat.messages(connectionId)
}

class SyncChatUseCase @Inject constructor(private val chat: ChatRepository) {
    operator fun invoke(connectionId: String): Flow<Nothing> = chat.sync(connectionId)
}

class LoadOlderMessagesUseCase @Inject constructor(private val chat: ChatRepository) {
    suspend operator fun invoke(connectionId: String): Boolean = chat.loadOlder(connectionId)
}

sealed interface SendMessageResult {
    data object Sent : SendMessageResult
    data class Invalid(val error: MessageError) : SendMessageResult
}

class SendMessageUseCase @Inject constructor(private val chat: ChatRepository) {
    suspend operator fun invoke(connectionId: String, body: String): SendMessageResult {
        MessageValidator.validate(body)?.let { return SendMessageResult.Invalid(it) }
        chat.send(connectionId, body.trim())
        return SendMessageResult.Sent
    }
}

class MarkChatReadUseCase @Inject constructor(
    private val chat: ChatRepository,
    private val connections: ConnectionRepository,
) {
    suspend operator fun invoke(connectionId: String) {
        chat.markRead(connectionId)
        connections.refresh()
    }
}

/** Собеседник чата; null, если такой связи у пользователя нет (например, он её заблокировал). */
class ObserveChatPartnerUseCase @Inject constructor(private val connections: ConnectionRepository) {
    operator fun invoke(connectionId: String): Flow<ConnectionSummary?> =
        connections.connections.map { all -> all.firstOrNull { it.connectionId == connectionId } }
}

/** Событие E7 из чата, с необязательной жалобой E10. */
class BlockInChatUseCase @Inject constructor(private val connections: ConnectionRepository) {
    suspend operator fun invoke(connectionId: String, complaint: ComplaintReason?) =
        connections.blockChat(connectionId, complaint)
}
