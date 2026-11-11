package com.coursework.nearby.domain.usecase

import com.coursework.nearby.domain.error.AppError
import com.coursework.nearby.domain.error.AppException
import com.coursework.nearby.domain.error.ServerError
import com.coursework.nearby.domain.location.GridSnapper
import com.coursework.nearby.domain.location.LocationProvider
import com.coursework.nearby.domain.model.GeoPoint
import com.coursework.nearby.domain.model.NearbyUser
import com.coursework.nearby.domain.model.SearchRadius
import com.coursework.nearby.domain.repository.PresenceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class PresenceUseCasesTest {
    private val home = GeoPoint(55.7512345, 37.6198765)
    private val sameCell = GeoPoint(55.7515, 37.6201)
    private val otherCell = GeoPoint(55.7560, 37.6198765)

    private class MutableClock(var now: Instant = Instant.parse("2026-11-12T10:00:00Z")) : Clock() {
        override fun instant(): Instant = now
        override fun getZone(): ZoneId = ZoneId.of("UTC")
        override fun withZone(zone: ZoneId?): Clock = this
    }

    private class FakePresence : PresenceRepository {
        val sent = mutableListOf<GeoPoint>()
        var failures = ArrayDeque<AppException>()
        var nearbyError: AppException? = null
        override suspend fun setVisibility(visible: Boolean) = Unit
        override suspend fun setRadius(radius: SearchRadius) = Unit
        override suspend fun updateLocation(point: GeoPoint) {
            failures.removeFirstOrNull()?.let { throw it }
            sent += point
        }
        override suspend fun getNearbyUsers(): List<NearbyUser> {
            nearbyError?.let { throw it }
            return emptyList()
        }
    }

    private val clock = MutableClock()
    private val presence = FakePresence()

    /** Точки приходят с заданным интервалом в секундах. */
    private fun provider(vararg fixes: Pair<Long, GeoPoint>) = object : LocationProvider {
        override fun locations(): Flow<GeoPoint> = flow {
            for ((afterSeconds, point) in fixes) {
                clock.now = clock.now.plusSeconds(afterSeconds)
                emit(point)
            }
        }
    }

    private fun track(vararg fixes: Pair<Long, GeoPoint>) = TrackLocationUseCase(provider(*fixes), presence, clock)()

    @Test
    fun `only coarsened coordinates leave the device`() = runTest {
        val emitted = track(0L to home).toList()
        assertEquals(listOf(GridSnapper.snap(home)), presence.sent)
        assertEquals(presence.sent, emitted)
        assertTrue(presence.sent.none { it == home })
    }

    @Test
    fun `moving inside one cell sends nothing new`() = runTest {
        track(0L to home, 30L to sameCell, 30L to home).toList()
        assertEquals(1, presence.sent.size)
    }

    @Test
    fun `moving to another cell is sent immediately`() = runTest {
        track(0L to home, 10L to otherCell).toList()
        assertEquals(listOf(GridSnapper.snap(home), GridSnapper.snap(otherCell)), presence.sent)
    }

    @Test
    fun `standing still refreshes the location every two minutes`() = runTest {
        track(0L to home, 60L to home, 60L to home, 60L to home, 60L to home).toList()
        // отправки на 0-й, 120-й и 240-й секундах
        assertEquals(3, presence.sent.size)
    }

    @Test
    fun `network failure does not stop tracking`() = runTest {
        presence.failures.add(AppException(AppError.Network))
        val emitted = track(0L to home, 10L to home).toList()
        assertEquals(listOf(GridSnapper.snap(home)), emitted)
    }

    @Test
    fun `server refusal stops tracking`() = runTest {
        presence.failures.add(AppException(AppError.Server(ServerError.BANNED)))
        try {
            track(0L to home).toList()
            fail("ожидалась ошибка")
        } catch (e: AppException) {
            assertEquals(AppError.Server(ServerError.BANNED), e.error)
        }
    }

    @Test
    fun `nearby list is empty until the server knows where the user is`() = runTest {
        val getNearby = GetNearbyUsersUseCase(presence)
        presence.nearbyError = AppException(AppError.Server(ServerError.LOCATION_STALE))
        assertEquals(emptyList<NearbyUser>(), getNearby())
        presence.nearbyError = AppException(AppError.Server(ServerError.NOT_VISIBLE))
        assertEquals(emptyList<NearbyUser>(), getNearby())
        presence.nearbyError = AppException(AppError.Network)
        try {
            getNearby()
            fail("ожидалась ошибка")
        } catch (e: AppException) {
            assertEquals(AppError.Network, e.error)
        }
    }
}
