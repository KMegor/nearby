package com.coursework.nearby.domain.usecase

import com.coursework.nearby.domain.error.AppError
import com.coursework.nearby.domain.error.AppException
import com.coursework.nearby.domain.error.ServerError
import com.coursework.nearby.domain.location.GridSnapper
import com.coursework.nearby.domain.location.LocationProvider
import com.coursework.nearby.domain.model.GeoPoint
import com.coursework.nearby.domain.model.NearbyUser
import com.coursework.nearby.domain.model.Profile
import com.coursework.nearby.domain.model.Relation
import com.coursework.nearby.domain.model.SearchRadius
import com.coursework.nearby.domain.repository.ConnectionRepository
import com.coursework.nearby.domain.repository.PresenceRepository
import com.coursework.nearby.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

class ObserveMyProfileUseCase @Inject constructor(private val profiles: ProfileRepository) {
    operator fun invoke(): StateFlow<Profile?> = profiles.myProfile
}

class SetVisibilityUseCase @Inject constructor(private val presence: PresenceRepository) {
    suspend operator fun invoke(visible: Boolean) = presence.setVisibility(visible)
}

class SetRadiusUseCase @Inject constructor(private val presence: PresenceRepository) {
    suspend operator fun invoke(radius: SearchRadius) = presence.setRadius(radius)
}

/**
 * Пока поток собирают, передаёт серверу огрублённое местоположение:
 * сразу при смене ячейки сетки и не реже раза в [HEARTBEAT], чтобы запись на сервере
 * не устарела (свежесть 30 минут). Выдаёт точку после каждой успешной отправки.
 */
class TrackLocationUseCase @Inject constructor(
    private val locationProvider: LocationProvider,
    private val presence: PresenceRepository,
    private val clock: Clock,
) {
    operator fun invoke(): Flow<GeoPoint> = flow {
        var lastCell: GeoPoint? = null
        var lastSentAt: Instant? = null
        locationProvider.locations().collect { exact ->
            val cell = GridSnapper.snap(exact)
            val now = clock.instant()
            val due = lastSentAt?.let { Duration.between(it, now) >= HEARTBEAT } ?: true
            if (cell != lastCell || due) {
                try {
                    presence.updateLocation(cell)
                    lastCell = cell
                    lastSentAt = now
                    emit(cell)
                } catch (e: AppException) {
                    // Сбой сети не останавливает слежение: следующая точка будет отправлена заново.
                    if (e.error != AppError.Network) throw e
                }
            }
        }
    }

    companion object {
        val HEARTBEAT: Duration = Duration.ofMinutes(2)
    }
}

class GetNearbyUsersUseCase @Inject constructor(private val presence: PresenceRepository) {
    /** Пустой список, если сервер ещё не считает пользователя видимым или не знает, где он. */
    suspend operator fun invoke(): List<NearbyUser> =
        try {
            presence.getNearbyUsers()
        } catch (e: AppException) {
            val error = (e.error as? AppError.Server)?.error
            if (error == ServerError.NOT_VISIBLE || error == ServerError.LOCATION_STALE) emptyList() else throw e
        }
}

class SendRequestUseCase @Inject constructor(private val connections: ConnectionRepository) {
    suspend operator fun invoke(userId: String): Relation = connections.sendRequest(userId)
}
