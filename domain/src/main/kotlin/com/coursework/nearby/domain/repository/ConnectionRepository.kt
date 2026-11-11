package com.coursework.nearby.domain.repository

import com.coursework.nearby.domain.model.Relation

/** События автомата состояний для пары пользователей. */
interface ConnectionRepository {
    /** Событие E4. Возвращает [Relation.OUTGOING] либо [Relation.CHAT], если запрос оказался встречным. */
    suspend fun sendRequest(userId: String): Relation
}
