package com.coursework.nearby.data.repository

import com.coursework.nearby.data.remote.NearbyUserDto
import com.coursework.nearby.data.remote.remoteCall
import com.coursework.nearby.domain.model.GeoPoint
import com.coursework.nearby.domain.model.NearbyUser
import com.coursework.nearby.domain.model.SearchRadius
import com.coursework.nearby.domain.repository.PresenceRepository
import com.coursework.nearby.domain.repository.ProfileRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

class PresenceRepositoryImpl @Inject constructor(
    private val client: SupabaseClient,
    private val profiles: ProfileRepository,
) : PresenceRepository {

    override suspend fun setVisibility(visible: Boolean) {
        remoteCall { client.postgrest.rpc("set_visibility", buildJsonObject { put("p_visible", visible) }) }
        // Видимость и радиус — поля профиля; перечитываем его, чтобы экран показывал то, что на сервере.
        profiles.refreshMyProfile()
    }

    override suspend fun setRadius(radius: SearchRadius) {
        remoteCall { client.postgrest.rpc("set_radius", buildJsonObject { put("p_km", radius.km) }) }
        profiles.refreshMyProfile()
    }

    override suspend fun updateLocation(point: GeoPoint) {
        remoteCall {
            client.postgrest.rpc(
                "update_location",
                buildJsonObject {
                    put("p_lat", point.latitude)
                    put("p_lng", point.longitude)
                },
            )
        }
    }

    override suspend fun getNearbyUsers(): List<NearbyUser> = remoteCall {
        val avatars = client.storage.from("avatars")
        client.postgrest.rpc("nearby_users").decodeList<NearbyUserDto>()
            .map { dto -> dto.toDomain(avatarUrl = dto.avatarPath?.let { avatars.publicUrl(it) }) }
    }
}
