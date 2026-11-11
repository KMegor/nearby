package com.coursework.nearby.data.repository

import com.coursework.nearby.data.remote.remoteCall
import com.coursework.nearby.domain.model.Relation
import com.coursework.nearby.domain.repository.ConnectionRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

class ConnectionRepositoryImpl @Inject constructor(
    private val client: SupabaseClient,
) : ConnectionRepository {

    override suspend fun sendRequest(userId: String): Relation = remoteCall {
        // Сервер возвращает новое состояние связи: pending либо active (встречный запрос).
        val state = client.postgrest.rpc("send_request", buildJsonObject { put("p_to", userId) }).decodeAs<String>()
        if (state == "active") Relation.CHAT else Relation.OUTGOING
    }
}
