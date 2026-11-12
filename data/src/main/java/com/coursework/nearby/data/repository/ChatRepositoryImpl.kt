package com.coursework.nearby.data.repository

import com.coursework.nearby.data.local.MessageDao
import com.coursework.nearby.data.remote.MessageDto
import com.coursework.nearby.data.remote.remoteCall
import com.coursework.nearby.domain.model.Message
import com.coursework.nearby.domain.repository.ChatRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val client: SupabaseClient,
    private val dao: MessageDao,
) : ChatRepository {

    override fun messages(connectionId: String): Flow<List<Message>> =
        dao.observe(connectionId).map { entities ->
            val me = client.auth.currentUserOrNull()?.id
            entities.map {
                Message(
                    id = it.id,
                    connectionId = it.connectionId,
                    senderId = it.senderId,
                    body = it.body,
                    createdAt = Instant.ofEpochMilli(it.createdAtMillis),
                    isMine = it.senderId == me,
                )
            }
        }

    override fun sync(connectionId: String): Flow<Nothing> = flow {
        coroutineScope {
            val channel = client.channel("chat-$connectionId")
            // Сервер присылает только те строки, которые пользователю разрешено читать (правила доступа).
            val inserts = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                table = "messages"
                filter("connection_id", FilterOperator.EQ, connectionId)
            }
            val collector = launch {
                inserts.collect { dao.upsert(listOf(it.decodeRecord<MessageDto>().toEntity())) }
            }
            try {
                channel.subscribe()
                loadPage(connectionId, beforeId = null)
                // Сообщение могло прийти между загрузкой истории и установкой подписки.
                channel.status.first { it == RealtimeChannel.Status.SUBSCRIBED }
                loadPage(connectionId, beforeId = null)
                awaitCancellation()
            } finally {
                collector.cancel()
                withContext(NonCancellable) { runCatching { client.realtime.removeChannel(channel) } }
            }
        }
    }

    override suspend fun loadOlder(connectionId: String): Boolean {
        val oldest = dao.oldestId(connectionId) ?: return false
        return loadPage(connectionId, beforeId = oldest) == PAGE_SIZE
    }

    override suspend fun send(connectionId: String, body: String) {
        val sent = remoteCall {
            client.postgrest.rpc(
                "send_message",
                buildJsonObject {
                    put("p_connection", connectionId)
                    put("p_body", body)
                },
            ).decodeAs<MessageDto>()
        }
        dao.upsert(listOf(sent.toEntity()))
    }

    override suspend fun markRead(connectionId: String) {
        remoteCall { client.postgrest.rpc("mark_read", buildJsonObject { put("p_connection", connectionId) }) }
    }

    override suspend fun clear() = dao.clear()

    /** Загружает страницу истории в кэш и возвращает число полученных сообщений. */
    private suspend fun loadPage(connectionId: String, beforeId: Long?): Int {
        val page = remoteCall {
            client.from("messages").select {
                filter {
                    eq("connection_id", connectionId)
                    if (beforeId != null) lt("id", beforeId)
                }
                order("id", Order.DESCENDING)
                limit(PAGE_SIZE.toLong())
            }.decodeList<MessageDto>()
        }
        dao.upsert(page.map { it.toEntity() })
        return page.size
    }

    private companion object {
        const val PAGE_SIZE = 50
    }
}
