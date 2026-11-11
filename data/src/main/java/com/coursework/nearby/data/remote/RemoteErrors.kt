package com.coursework.nearby.data.remote

import com.coursework.nearby.domain.error.AppError
import com.coursework.nearby.domain.error.AppException
import com.coursework.nearby.domain.error.AuthError
import com.coursework.nearby.domain.error.ServerError
import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import kotlinx.coroutines.CancellationException
import java.io.IOException

/** Выполняет обращение к серверу и переводит его ошибки в ошибки предметной области. */
internal suspend fun <T> remoteCall(block: suspend () -> T): T =
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        throw e.toAppException()
    }

internal fun Throwable.toAppException(): AppException = when (this) {
    is AppException -> this
    is AuthRestException -> AppException(AppError.Auth(errorCode.toAuthError()), this)
    // Серверные функции сообщают причину отказа коротким кодом в тексте исключения.
    is PostgrestRestException -> AppException(AppError.Server(ServerError.fromCode(error)), this)
    is RestException -> AppException(AppError.Unknown, this)
    is IOException -> AppException(AppError.Network, this)
    else -> AppException(AppError.Unknown, this)
}

private fun AuthErrorCode?.toAuthError(): AuthError = when (this) {
    AuthErrorCode.InvalidCredentials -> AuthError.INVALID_CREDENTIALS
    AuthErrorCode.UserAlreadyExists, AuthErrorCode.EmailExists -> AuthError.USER_EXISTS
    AuthErrorCode.WeakPassword -> AuthError.WEAK_PASSWORD
    else -> AuthError.UNKNOWN
}
