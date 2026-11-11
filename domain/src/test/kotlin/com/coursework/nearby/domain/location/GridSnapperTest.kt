package com.coursework.nearby.domain.location

import com.coursework.nearby.domain.model.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class GridSnapperTest {

    /** Эталон получен от серверной функции `_snap` на локальной базе. */
    @Test
    fun `matches the server function`() {
        val cases = listOf(
            GeoPoint(55.7512345, 37.6198765) to GeoPoint(55.752, 37.6206469693539),
            GeoPoint(-33.8568, 151.2153) to GeoPoint(-33.856, 151.21615757228),
            GeoPoint(59.9386, 30.3141) to GeoPoint(59.938, 30.3151992145045),
        )
        for ((input, expected) in cases) {
            val actual = GridSnapper.snap(input)
            assertEquals(expected.latitude, actual.latitude, 1e-9)
            assertEquals(expected.longitude, actual.longitude, 1e-9)
        }
    }

    @Test
    fun `snapping twice changes nothing`() {
        val once = GridSnapper.snap(GeoPoint(55.7512345, 37.6198765))
        val twice = GridSnapper.snap(once)
        assertEquals(once.latitude, twice.latitude, 1e-12)
        assertEquals(once.longitude, twice.longitude, 1e-12)
    }

    @Test
    fun `nearby points fall into the same cell`() {
        assertEquals(
            GridSnapper.snap(GeoPoint(55.7512345, 37.6198765)),
            GridSnapper.snap(GeoPoint(55.7515, 37.6201)),
        )
    }

    @Test
    fun `snapped point stays within one cell of the original`() {
        val points = listOf(
            GeoPoint(55.7512345, 37.6198765),
            GeoPoint(0.0009, 0.0009),
            GeoPoint(69.3558, 88.1893),
            GeoPoint(-54.8019, -68.3030),
        )
        for (point in points) {
            val distance = distanceMeters(point, GridSnapper.snap(point))
            // половина диагонали ячейки 222 x 222 м
            assertTrue("сдвиг $distance м для $point", distance < 160.0)
        }
    }

    @Test
    fun `exact coordinates are not recoverable from the result`() {
        val snapped = GridSnapper.snap(GeoPoint(55.7512345, 37.6198765))
        assertTrue(distanceMeters(GeoPoint(55.7512345, 37.6198765), snapped) > 50.0)
    }

    private fun distanceMeters(a: GeoPoint, b: GeoPoint): Double {
        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLng = Math.toRadians(b.longitude - a.longitude)
        val h = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(a.latitude)) * cos(Math.toRadians(b.latitude)) * sin(dLng / 2).pow(2)
        return 2 * 6_371_000.0 * asin(sqrt(h))
    }
}
