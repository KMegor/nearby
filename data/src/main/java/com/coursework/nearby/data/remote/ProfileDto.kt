package com.coursework.nearby.data.remote

import com.coursework.nearby.domain.model.Interest
import com.coursework.nearby.domain.model.Profile
import com.coursework.nearby.domain.model.SearchRadius
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.OffsetDateTime

@Serializable
internal data class ProfileDto(
    val id: String,
    val name: String,
    @SerialName("birth_year") val birthYear: Int,
    val bio: String = "",
    @SerialName("avatar_path") val avatarPath: String? = null,
    val visible: Boolean = false,
    @SerialName("radius_km") val radiusKm: Int = 1,
    @SerialName("banned_until") val bannedUntil: String? = null,
) {
    fun toDomain(interests: List<Interest>, avatarUrl: String?) = Profile(
        id = id,
        name = name,
        birthYear = birthYear,
        bio = bio,
        avatarUrl = avatarUrl,
        interests = interests,
        visible = visible,
        radius = SearchRadius.fromKm(radiusKm),
        bannedUntil = parseBannedUntil(bannedUntil),
    )
}

@Serializable
internal data class InterestDto(val id: Int, val name: String)

@Serializable
internal data class ProfileInterestDto(@SerialName("interest_id") val interestId: Int)

/** Бессрочное отстранение сервер хранит как 'infinity'. */
internal fun parseBannedUntil(value: String?): Instant? = when (value) {
    null -> null
    "infinity" -> Instant.MAX
    else -> OffsetDateTime.parse(value).toInstant()
}
