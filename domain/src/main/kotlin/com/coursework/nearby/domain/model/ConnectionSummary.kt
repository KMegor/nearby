package com.coursework.nearby.domain.model

import java.time.Instant

/** Связь с собеседником в том виде, в каком сервер показывает её текущему пользователю. */
data class ConnectionSummary(
    val connectionId: String,
    val partnerId: String,
    val name: String,
    val age: Int,
    val bio: String,
    val avatarUrl: String?,
    val interests: List<String>,
    val relation: Relation,
    /** Для [Relation.CHAT]: можно ли сейчас писать (нет блокировки и отстранения). */
    val canSend: Boolean,
    /** Для [Relation.INCOMING]: это повторный запрос, вместе с отказом доступна блокировка (условие G4). */
    val canBlockOnReject: Boolean,
    /** Для [Relation.COOLDOWN]: когда снова можно отправить запрос. */
    val retryAt: Instant?,
    val lastMessage: String?,
    val lastMessageAt: Instant?,
    val unread: Int,
    val updatedAt: Instant,
)

enum class ComplaintReason(val code: String) { SPAM("spam"), ABUSE("abuse"), OTHER("other") }

/** Ответ адресата на запрос: события E5, E6 и E7 (с необязательной жалобой E10). */
sealed interface RequestResponse {
    data object Accept : RequestResponse
    data object Reject : RequestResponse
    data class Block(val complaint: ComplaintReason?) : RequestResponse
}

/** Разбиение связей по разделам интерфейса. */
data class ConnectionLists(
    val incoming: List<ConnectionSummary> = emptyList(),
    val outgoing: List<ConnectionSummary> = emptyList(),
    val chats: List<ConnectionSummary> = emptyList(),
    val blocked: List<ConnectionSummary> = emptyList(),
) {
    companion object {
        fun from(connections: List<ConnectionSummary>) = ConnectionLists(
            incoming = connections.filter { it.relation == Relation.INCOMING },
            // Отказ показывается отправителю рядом с ожидающими запросами — со сроком повтора.
            outgoing = connections.filter { it.relation == Relation.OUTGOING || it.relation == Relation.COOLDOWN },
            chats = connections.filter { it.relation == Relation.CHAT },
            blocked = connections.filter { it.relation == Relation.BLOCKED_BY_ME },
        )
    }
}
