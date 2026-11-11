package com.coursework.nearby.domain.model

import com.coursework.nearby.domain.error.AppError
import java.time.Instant

/** Что показывать пользователю на верхнем уровне приложения. */
sealed interface AppState {
    data object Loading : AppState
    data object SignedOut : AppState

    /** Вошёл, но профиль ещё не заполнен. */
    data object NeedsProfile : AppState

    /** Аккаунт отстранён по жалобам до [until]; [permanent] — навсегда. */
    data class Banned(val until: Instant, val permanent: Boolean) : AppState

    data class Ready(val profile: Profile) : AppState
    data class Failed(val error: AppError) : AppState
}
