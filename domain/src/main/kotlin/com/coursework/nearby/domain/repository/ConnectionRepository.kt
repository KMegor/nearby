package com.coursework.nearby.domain.repository

import com.coursework.nearby.domain.model.ConnectionSummary
import com.coursework.nearby.domain.model.Relation
import com.coursework.nearby.domain.model.RequestResponse
import kotlinx.coroutines.flow.StateFlow

/** События автомата состояний для пары пользователей. */
interface ConnectionRepository {
    /** Все связи текущего пользователя; обновляется после [refresh] и после каждого события. */
    val connections: StateFlow<List<ConnectionSummary>>

    suspend fun refresh()

    /** Забыть загруженное: при выходе из аккаунта. */
    fun clear()

    /** Событие E4. Возвращает [Relation.OUTGOING] либо [Relation.CHAT], если запрос оказался встречным. */
    suspend fun sendRequest(userId: String): Relation

    /** События E5, E6, E7 из состояния «запрос отправлен». */
    suspend fun respond(connectionId: String, response: RequestResponse)

    /** Событие E8. */
    suspend fun unblock(connectionId: String)
}
