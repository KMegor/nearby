package com.coursework.nearby.data.repository

import com.coursework.nearby.data.remote.ConnectionSummaryDto
import com.coursework.nearby.data.remote.remoteCall
import com.coursework.nearby.domain.model.ComplaintReason
import com.coursework.nearby.domain.model.ConnectionSummary
import com.coursework.nearby.domain.model.Relation
import com.coursework.nearby.domain.model.RequestResponse
import com.coursework.nearby.domain.repository.ConnectionRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

class ConnectionRepositoryImpl @Inject constructor(
    private val client: SupabaseClient,
) : ConnectionRepository {

    private val state = MutableStateFlow<List<ConnectionSummary>>(emptyList())
    override val connections: StateFlow<List<ConnectionSummary>> = state.asStateFlow()

    override suspend fun refresh() {
        state.value = remoteCall {
            val avatars = client.storage.from("avatars")
            // Таблица связей закрыта от прямого чтения: список отдаёт функция, которая не раскрывает
            // заблокированному сам факт блокировки.
            client.postgrest.rpc("my_connections").decodeList<ConnectionSummaryDto>()
                .map { dto -> dto.toDomain(avatarUrl = dto.avatarPath?.let { avatars.publicUrl(it) }) }
        }
    }

    override fun clear() {
        state.value = emptyList()
    }

    override suspend fun sendRequest(userId: String): Relation {
        // Сервер возвращает новое состояние связи: pending либо active (встречный запрос).
        val result = remoteCall {
            client.postgrest.rpc("send_request", buildJsonObject { put("p_to", userId) }).decodeAs<String>()
        }
        refresh()
        return if (result == "active") Relation.CHAT else Relation.OUTGOING
    }

    override suspend fun respond(connectionId: String, response: RequestResponse) {
        remoteCall {
            client.postgrest.rpc(
                "respond_request",
                buildJsonObject {
                    put("p_connection", connectionId)
                    when (response) {
                        RequestResponse.Accept -> put("p_action", "accept")
                        RequestResponse.Reject -> put("p_action", "reject")
                        is RequestResponse.Block -> {
                            put("p_action", "block")
                            put("p_complain", response.complaint != null)
                            response.complaint?.let { put("p_reason", it.code) }
                        }
                    }
                },
            )
        }
        refresh()
    }

    override suspend fun blockChat(connectionId: String, complaint: ComplaintReason?) {
        remoteCall {
            client.postgrest.rpc(
                "block_user",
                buildJsonObject {
                    put("p_connection", connectionId)
                    put("p_complain", complaint != null)
                    complaint?.let { put("p_reason", it.code) }
                },
            )
        }
        refresh()
    }

    override suspend fun unblock(connectionId: String) {
        remoteCall { client.postgrest.rpc("unblock_user", buildJsonObject { put("p_connection", connectionId) }) }
        refresh()
    }
}
