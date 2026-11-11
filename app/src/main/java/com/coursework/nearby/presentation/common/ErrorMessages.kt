package com.coursework.nearby.presentation.common

import androidx.annotation.StringRes
import com.coursework.nearby.R
import com.coursework.nearby.domain.error.AppError
import com.coursework.nearby.domain.error.AuthError
import com.coursework.nearby.domain.error.ServerError

/** Текст ошибки для пользователя. */
@StringRes
fun AppError.messageRes(): Int = when (this) {
    AppError.Network -> R.string.error_network
    AppError.Unknown -> R.string.error_unknown
    is AppError.Auth -> when (error) {
        AuthError.INVALID_CREDENTIALS -> R.string.error_invalid_credentials
        AuthError.USER_EXISTS -> R.string.error_user_exists
        AuthError.WEAK_PASSWORD -> R.string.error_weak_password
        AuthError.UNKNOWN -> R.string.error_unknown
    }
    is AppError.Server -> when (error) {
        ServerError.BANNED -> R.string.error_banned
        ServerError.AGE_RESTRICTED -> R.string.error_age_restricted
        ServerError.OUT_OF_RANGE, ServerError.NOT_VISIBLE -> R.string.error_out_of_range
        ServerError.USER_UNAVAILABLE -> R.string.error_user_unavailable
        ServerError.COOLDOWN -> R.string.error_cooldown
        ServerError.REQUEST_UNAVAILABLE -> R.string.error_request_unavailable
        ServerError.ALREADY_PENDING, ServerError.ALREADY_CONNECTED -> R.string.error_already_pending
        else -> R.string.error_unknown
    }
}
