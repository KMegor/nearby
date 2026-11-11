package com.coursework.nearby.domain.model

/**
 * Как связь с собеседником выглядит для текущего пользователя.
 * Сервер намеренно не раскрывает заблокированному сам факт блокировки,
 * поэтому отдельного значения «меня заблокировали» нет.
 */
enum class Relation(val code: String) {
    /** S0/S1: связи нет либо срок отказа истёк. */
    NONE("none"),

    /** S2: запрос отправлен мной. */
    OUTGOING("outgoing"),

    /** S2: запрос пришёл мне. */
    INCOMING("incoming"),

    /** S3: чат существует. */
    CHAT("chat"),

    /** S4: отказ, 24 часа ещё не прошли. */
    COOLDOWN("cooldown"),

    /** S5: собеседник заблокирован мной. */
    BLOCKED_BY_ME("blocked_by_me");

    companion object {
        fun fromCode(code: String): Relation =
            entries.firstOrNull { it.code == code } ?: NONE
    }
}
