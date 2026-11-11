package com.coursework.nearby.domain.model

import java.time.Instant

data class Profile(
    val id: String,
    val name: String,
    val birthYear: Int,
    val bio: String,
    val avatarUrl: String?,
    val interests: List<Interest>,
    val visible: Boolean,
    val radius: SearchRadius,
    /** Момент окончания отстранения; [Instant.MAX] означает «навсегда». */
    val bannedUntil: Instant?,
) {
    fun age(currentYear: Int): Int = currentYear - birthYear

    /** Срок отстранения проверяется по времени, как и на сервере (поле banned_until). */
    fun isBanned(now: Instant): Boolean = bannedUntil != null && bannedUntil.isAfter(now)
}

/** Данные формы профиля до проверки и сохранения. */
data class ProfileDraft(
    val name: String,
    val birthYear: Int?,
    val bio: String,
    val interestIds: Set<Int>,
)
