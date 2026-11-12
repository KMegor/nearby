package com.coursework.nearby.data.remote

import com.coursework.nearby.domain.model.ConnectionSummary
import com.coursework.nearby.domain.model.Relation
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.OffsetDateTime

@Serializable
internal data class ConnectionSummaryDto(
    @SerialName("connection_id") val connectionId: String,
    @SerialName("partner_id") val partnerId: String,
    val name: String,
    val age: Int,
    val bio: String = "",
    @SerialName("avatar_path") val avatarPath: String? = null,
    val interests: List<String> = emptyList(),
    val relation: String,
    @SerialName("can_send") val canSend: Boolean = false,
    @SerialName("can_block_on_reject") val canBlockOnReject: Boolean = false,
    @SerialName("retry_at") val retryAt: String? = null,
    @SerialName("last_message") val lastMessage: String? = null,
    @SerialName("last_message_at") val lastMessageAt: String? = null,
    val unread: Int = 0,
    @SerialName("updated_at") val updatedAt: String,
) {
    fun toDomain(avatarUrl: String?) = ConnectionSummary(
        connectionId = connectionId,
        partnerId = partnerId,
        name = name,
        age = age,
        bio = bio,
        avatarUrl = avatarUrl,
        interests = interests,
        relation = Relation.fromCode(relation),
        canSend = canSend,
        canBlockOnReject = canBlockOnReject,
        retryAt = retryAt?.toInstant(),
        lastMessage = lastMessage,
        lastMessageAt = lastMessageAt?.toInstant(),
        unread = unread,
        updatedAt = updatedAt.toInstant(),
    )
}

internal fun String.toInstant(): Instant = OffsetDateTime.parse(this).toInstant()
