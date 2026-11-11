package com.coursework.nearby.domain.location

import com.coursework.nearby.domain.model.GeoPoint
import kotlinx.coroutines.flow.Flow

/** Источник точных координат устройства. За пределы устройства они уходят только после [GridSnapper]. */
interface LocationProvider {
    /** Первая точка приходит как можно раньше, дальше — периодически, пока поток собирают. */
    fun locations(): Flow<GeoPoint>
}
