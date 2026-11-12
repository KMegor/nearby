package com.coursework.nearby.domain.repository

import com.coursework.nearby.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    /** Сообщения чата из локального кэша, от старых к новым. Доступны и без сети. */
    fun messages(connectionId: String): Flow<List<Message>>

    /**
     * Пока поток собирают, кэш поддерживается актуальным: загружается последняя страница истории,
     * затем новые сообщения приходят в реальном времени. Ничего не выдаёт; завершается ошибкой,
     * если историю загрузить не удалось.
     */
    fun sync(connectionId: String): Flow<Nothing>

    /** Догружает более ранние сообщения. Возвращает false, когда история закончилась. */
    suspend fun loadOlder(connectionId: String): Boolean

    /** Событие E9. */
    suspend fun send(connectionId: String, body: String)

    suspend fun markRead(connectionId: String)

    /** Удалить кэш: при выходе из аккаунта. */
    suspend fun clear()
}
