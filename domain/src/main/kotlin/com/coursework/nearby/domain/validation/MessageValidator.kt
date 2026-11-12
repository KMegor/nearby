package com.coursework.nearby.domain.validation

enum class MessageError { EMPTY, TOO_LONG }

/** Те же ограничения, что в серверной функции send_message. */
object MessageValidator {
    const val MAX_LENGTH = 1000

    fun validate(body: String): MessageError? {
        val text = body.trim()
        return when {
            text.isEmpty() -> MessageError.EMPTY
            text.length > MAX_LENGTH -> MessageError.TOO_LONG
            else -> null
        }
    }
}
