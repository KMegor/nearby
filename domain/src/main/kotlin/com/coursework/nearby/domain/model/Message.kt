package com.coursework.nearby.domain.model

import java.time.Instant

data class Message(
    val id: Long,
    val connectionId: String,
    val senderId: String,
    val body: String,
    val createdAt: Instant,
    val isMine: Boolean,
)
