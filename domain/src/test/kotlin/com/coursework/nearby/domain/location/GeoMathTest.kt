package com.coursework.nearby.domain.location

import com.coursework.nearby.domain.model.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Test

class GeoMathTest {
    private val moscow = GeoPoint(55.752, 37.6206)

    @Test
    fun `distance between known cities`() {
        val petersburg = GeoPoint(59.9386, 30.3141)
        assertEquals(634_000.0, GeoMath.distanceMeters(moscow, petersburg), 3_000.0)
    }

    @Test
    fun `destination lies at the requested distance`() {
        for (bearing in listOf(0.0, 90.0, 200.0, 315.0)) {
            assertEquals(3_000.0, GeoMath.distanceMeters(moscow, GeoMath.destination(moscow, 3_000.0, bearing)), 1.0)
        }
    }

    @Test
    fun `circle is closed and every vertex is on the radius`() {
        val circle = GeoMath.circle(moscow, 1_000.0, segments = 36)
        assertEquals(37, circle.size)
        assertEquals(circle.first(), circle.last())
        circle.forEach { assertEquals(1_000.0, GeoMath.distanceMeters(moscow, it), 0.5) }
    }
}
