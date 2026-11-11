package com.coursework.nearby.domain.usecase

import com.coursework.nearby.domain.error.AppError
import com.coursework.nearby.domain.error.AppException
import com.coursework.nearby.domain.error.ServerError
import com.coursework.nearby.domain.model.AppState
import com.coursework.nearby.domain.model.SessionState
import com.coursework.nearby.domain.repository.AuthRepository
import com.coursework.nearby.domain.repository.ProfileRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/**
 * Сводит сессию и профиль в одно состояние верхнего уровня:
 * вход → заполнение профиля → отстранение → основное приложение.
 */
class ObserveAppStateUseCase @Inject constructor(
    private val auth: AuthRepository,
    private val profiles: ProfileRepository,
    private val clock: Clock,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<AppState> = auth.session
        .distinctUntilChanged()
        .flatMapLatest { session ->
            when (session) {
                SessionState.Loading -> flowOf(AppState.Loading)
                SessionState.SignedOut -> flowOf(AppState.SignedOut)
                is SessionState.SignedIn -> signedInState()
            }
        }
        .distinctUntilChanged()

    private fun signedInState(): Flow<AppState> = flow {
        emit(AppState.Loading)
        try {
            profiles.refreshMyProfile()
        } catch (e: AppException) {
            emit(AppState.Failed(e.error))
            return@flow
        }
        emitAll(
            profiles.myProfile.map { profile ->
                when {
                    profile == null -> AppState.NeedsProfile
                    profile.isBanned(clock.instant()) ->
                        AppState.Banned(profile.bannedUntil!!, permanent = profile.bannedUntil == Instant.MAX)
                    else -> AppState.Ready(profile)
                }
            },
        )
    }

    companion object {
        /** Сервер отвечает этим кодом на любое действие отстранённого пользователя. */
        fun isBanError(error: AppError): Boolean = error == AppError.Server(ServerError.BANNED)
    }
}
