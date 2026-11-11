package com.coursework.nearby.domain.error

enum class AuthError { INVALID_CREDENTIALS, USER_EXISTS, WEAK_PASSWORD, UNKNOWN }

sealed interface AppError {
    /** Сервер отклонил событие автомата состояний. */
    data class Server(val error: ServerError) : AppError
    data class Auth(val error: AuthError) : AppError
    data object Network : AppError
    data object Unknown : AppError
}

class AppException(val error: AppError, cause: Throwable? = null) : Exception(error.toString(), cause)
