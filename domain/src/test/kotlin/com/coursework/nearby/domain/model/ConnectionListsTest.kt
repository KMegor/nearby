package com.coursework.nearby.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class ConnectionListsTest {
    private fun connection(id: String, relation: Relation) = ConnectionSummary(
        connectionId = id, partnerId = "p$id", name = "User $id", age = 25, bio = "", avatarUrl = null,
        interests = emptyList(), relation = relation, canSend = relation == Relation.CHAT, canBlockOnReject = false,
        retryAt = null, lastMessage = null, lastMessageAt = null, unread = 0, updatedAt = Instant.EPOCH,
    )

    @Test
    fun `connections are split into interface sections`() {
        val lists = ConnectionLists.from(
            listOf(
                connection("1", Relation.INCOMING),
                connection("2", Relation.OUTGOING),
                connection("3", Relation.COOLDOWN),
                connection("4", Relation.CHAT),
                connection("5", Relation.BLOCKED_BY_ME),
                connection("6", Relation.INCOMING),
            ),
        )
        assertEquals(listOf("1", "6"), lists.incoming.map { it.connectionId })
        assertEquals(listOf("2", "3"), lists.outgoing.map { it.connectionId })
        assertEquals(listOf("4"), lists.chats.map { it.connectionId })
        assertEquals(listOf("5"), lists.blocked.map { it.connectionId })
    }

    @Test
    fun `blocked partner never appears among requests or chats`() {
        val lists = ConnectionLists.from(listOf(connection("1", Relation.BLOCKED_BY_ME)))
        assertEquals(0, lists.incoming.size + lists.outgoing.size + lists.chats.size)
    }
}
