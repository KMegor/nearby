package com.coursework.nearby.domain.usecase

import com.coursework.nearby.domain.model.Message
import com.coursework.nearby.domain.repository.ChatRepository
import com.coursework.nearby.domain.validation.MessageError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatUseCasesTest {
    private class FakeChat : ChatRepository {
        val sent = mutableListOf<String>()
        override fun messages(connectionId: String): Flow<List<Message>> = emptyFlow()
        override fun sync(connectionId: String): Flow<Nothing> = emptyFlow()
        override suspend fun loadOlder(connectionId: String) = false
        override suspend fun send(connectionId: String, body: String) { sent += body }
        override suspend fun markRead(connectionId: String) = Unit
        override suspend fun clear() = Unit
    }

    private val chat = FakeChat()
    private val send = SendMessageUseCase(chat)

    @Test
    fun `message is trimmed before sending`() = runTest {
        assertEquals(SendMessageResult.Sent, send("c", "  привет  "))
        assertEquals(listOf("привет"), chat.sent)
    }

    @Test
    fun `blank message is not sent`() = runTest {
        assertEquals(SendMessageResult.Invalid(MessageError.EMPTY), send("c", "   \n "))
        assertEquals(emptyList<String>(), chat.sent)
    }

    @Test
    fun `message length is limited to 1000 characters`() = runTest {
        assertEquals(SendMessageResult.Sent, send("c", "a".repeat(1000)))
        assertEquals(SendMessageResult.Invalid(MessageError.TOO_LONG), send("c", "a".repeat(1001)))
        assertEquals(1, chat.sent.size)
    }
}
