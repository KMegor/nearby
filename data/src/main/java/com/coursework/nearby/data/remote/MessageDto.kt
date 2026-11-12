package com.coursework.nearby.data.remote

import com.coursework.nearby.data.local.MessageEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class MessageDto(
    val id: Long,
    @SerialName("connection_id") val connectionId: String,
    val sender: String,
    val body: String,
    @SerialName("created_at") val createdAt: String,
) {
    fun toEntity() = MessageEntity(
        id = id,
        connectionId = connectionId,
        senderId = sender,
        body = body,
        createdAtMillis = createdAt.toInstant().toEpochMilli(),
    )
}
