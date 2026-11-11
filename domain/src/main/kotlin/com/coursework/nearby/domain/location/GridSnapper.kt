package com.coursework.nearby.domain.location

import com.coursework.nearby.domain.model.GeoPoint
import kotlin.math.cos
import kotlin.math.max

/**
 * Огрубление координат до сетки с шагом около 200 м (инвариант «Приватность местоположения»).
 * Выполняется на устройстве до отправки, поэтому точные координаты его не покидают.
 * Формула обязана совпадать с серверной функцией `_snap`: сервер повторяет привязку,
 * и при совпадении она ничего не меняет.
 */
object GridSnapper {
    /** Шаг сетки по широте в градусах, около 222 м. */
    const val STEP_DEGREES = 0.002

    fun snap(point: GeoPoint): GeoPoint {
        val latitude = Math.rint(point.latitude / STEP_DEGREES) * STEP_DEGREES
        // Градус долготы сужается к полюсам, поэтому шаг по долготе растёт с широтой.
        val longitudeStep = STEP_DEGREES / max(cos(Math.toRadians(latitude)), 0.01)
        val longitude = Math.rint(point.longitude / longitudeStep) * longitudeStep
        return GeoPoint(latitude, longitude)
    }
}
