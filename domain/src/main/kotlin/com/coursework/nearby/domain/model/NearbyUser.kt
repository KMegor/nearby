package com.coursework.nearby.domain.model

import java.time.Instant

/** Человек на карте: то, что о нём можно показать незнакомцу. */
data class NearbyUser(
    val id: String,
    val name: String,
    val age: Int,
    val bio: String,
    val avatarUrl: String?,
    val interests: List<String>,
    /** Приблизительная точка: сервер не отдаёт настоящие координаты. */
    val point: GeoPoint,
    val relation: Relation,
    val connectionId: String?,
    /** Для [Relation.COOLDOWN]: когда снова можно отправить запрос. */
    val retryAt: Instant?,
    /** Для [Relation.INCOMING]: это повторный запрос, вместе с отказом доступна блокировка. */
    val canBlockOnReject: Boolean,
)
