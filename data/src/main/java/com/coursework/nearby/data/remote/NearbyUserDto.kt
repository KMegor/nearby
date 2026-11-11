package com.coursework.nearby.data.remote

import com.coursework.nearby.domain.model.GeoPoint
import com.coursework.nearby.domain.model.NearbyUser
import com.coursework.nearby.domain.model.Relation
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.OffsetDateTime

@Serializable
internal data class NearbyUserDto(
    @SerialName("user_id") val userId: String,
    val name: String,
    val age: Int,
    val bio: String = "",
    @SerialName("avatar_path") val avatarPath: String? = null,
    val interests: List<String> = emptyList(),
    val lat: Double,
    val lng: Double,
    val relation: String,
    @SerialName("connection_id") val connectionId: String? = null,
    @SerialName("retry_at") val retryAt: String? = null,
    @SerialName("can_block_on_reject") val canBlockOnReject: Boolean = false,
) {
    fun toDomain(avatarUrl: String?) = NearbyUser(
        id = userId,
        name = name,
        age = age,
        bio = bio,
        avatarUrl = avatarUrl,
        interests = interests,
        point = GeoPoint(lat, lng),
        relation = Relation.fromCode(relation),
        connectionId = connectionId,
        retryAt = retryAt?.let { OffsetDateTime.parse(it).toInstant() },
        canBlockOnReject = canBlockOnReject,
    )
}
