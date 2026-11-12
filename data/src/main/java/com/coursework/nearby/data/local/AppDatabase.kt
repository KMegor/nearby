package com.coursework.nearby.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Сообщение в локальном кэше. Ключ — серверный id, поэтому повторная запись ничего не дублирует. */
@Entity(tableName = "messages", indices = [Index("connectionId", "id")])
data class MessageEntity(
    @PrimaryKey val id: Long,
    val connectionId: String,
    val senderId: String,
    val body: String,
    val createdAtMillis: Long,
)

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE connectionId = :connectionId ORDER BY id")
    fun observe(connectionId: String): Flow<List<MessageEntity>>

    @Upsert
    suspend fun upsert(messages: List<MessageEntity>)

    @Query("SELECT MIN(id) FROM messages WHERE connectionId = :connectionId")
    suspend fun oldestId(connectionId: String): Long?

    @Query("DELETE FROM messages")
    suspend fun clear()
}

/** Локальный кэш только для чтения: источник истины — сервер, отправка без сети не выполняется. */
@Database(entities = [MessageEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
}
