package com.coursework.nearby.domain.location

import com.coursework.nearby.domain.model.GeoPoint
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object GeoMath {
    private const val EARTH_RADIUS_METERS = 6_371_000.0

    fun distanceMeters(a: GeoPoint, b: GeoPoint): Double {
        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLng = Math.toRadians(b.longitude - a.longitude)
        val h = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(a.latitude)) * cos(Math.toRadians(b.latitude)) * sin(dLng / 2).pow(2)
        return 2 * EARTH_RADIUS_METERS * asin(sqrt(h))
    }

    /** Точка на расстоянии [meters] от [from] по азимуту [bearingDegrees] (0 — север, 90 — восток). */
    fun destination(from: GeoPoint, meters: Double, bearingDegrees: Double): GeoPoint {
        val angular = meters / EARTH_RADIUS_METERS
        val bearing = Math.toRadians(bearingDegrees)
        val lat1 = Math.toRadians(from.latitude)
        val lng1 = Math.toRadians(from.longitude)
        val lat2 = asin(sin(lat1) * cos(angular) + cos(lat1) * sin(angular) * cos(bearing))
        val lng2 = lng1 + atan2(sin(bearing) * sin(angular) * cos(lat1), cos(angular) - sin(lat1) * sin(lat2))
        return GeoPoint(Math.toDegrees(lat2), Math.toDegrees(lng2))
    }

    /** Замкнутый контур круга радиуса поиска для отрисовки на карте. */
    fun circle(center: GeoPoint, radiusMeters: Double, segments: Int = 72): List<GeoPoint> {
        val ring = (0 until segments).map { destination(center, radiusMeters, it * 360.0 / segments) }
        return ring + ring.first()
    }
}
