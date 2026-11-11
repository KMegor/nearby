package com.coursework.nearby.domain.repository

import com.coursework.nearby.domain.model.GeoPoint
import com.coursework.nearby.domain.model.NearbyUser
import com.coursework.nearby.domain.model.SearchRadius

/** Режим пользователя (S0/S1) и поиск людей рядом. */
interface PresenceRepository {
    /** События E1 и E2. */
    suspend fun setVisibility(visible: Boolean)

    /** Событие E3. */
    suspend fun setRadius(radius: SearchRadius)

    /** [point] должен быть уже огрублён. */
    suspend fun updateLocation(point: GeoPoint)

    suspend fun getNearbyUsers(): List<NearbyUser>
}
