package com.coursework.nearby.domain.error

/**
 * Отказы сервера при попытке выполнить событие автомата состояний.
 * Код совпадает с текстом исключения в серверных функциях.
 */
enum class ServerError(val code: String) {
    NOT_AUTHENTICATED("not_authenticated"),
    PROFILE_REQUIRED("profile_required"),
    BANNED("banned"),
    AGE_RESTRICTED("age_restricted"),
    TOO_MANY_INTERESTS("too_many_interests"),
    BAD_RADIUS("bad_radius"),
    BAD_LOCATION("bad_location"),
    NOT_VISIBLE("not_visible"),
    LOCATION_STALE("location_stale"),
    BAD_TARGET("bad_target"),
    OUT_OF_RANGE("out_of_range"),
    USER_UNAVAILABLE("user_unavailable"),
    ALREADY_PENDING("already_pending"),
    ALREADY_CONNECTED("already_connected"),
    COOLDOWN("cooldown"),
    REQUEST_UNAVAILABLE("request_unavailable"),
    TRY_AGAIN("try_again"),
    NOT_FOUND("not_found"),
    NOT_PENDING("not_pending"),
    NOT_ADDRESSEE("not_addressee"),
    BLOCK_NOT_AVAILABLE("block_not_available"),
    COMPLAINT_REQUIRES_BLOCK("complaint_requires_block"),
    BAD_REASON("bad_reason"),
    BAD_ACTION("bad_action"),
    NOT_ACTIVE("not_active"),
    NOT_BLOCKED_BY_YOU("not_blocked_by_you"),
    CANNOT_SEND("cannot_send"),
    EMPTY_MESSAGE("empty_message"),
    MESSAGE_TOO_LONG("message_too_long"),
    UNKNOWN("unknown");

    companion object {
        fun fromCode(code: String?): ServerError =
            entries.firstOrNull { it.code == code?.trim() } ?: UNKNOWN
    }
}
