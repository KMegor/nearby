package com.coursework.nearby.presentation.common

import java.time.Duration
import java.time.Instant

/** Сколько полных часов осталось до [moment], с округлением вверх: за минуту до срока — 1. */
fun hoursUntil(moment: Instant?, now: Instant = Instant.now()): Int {
    val seconds = moment?.let { Duration.between(now, it).seconds } ?: 0
    return ((seconds + 3599) / 3600).coerceAtLeast(1).toInt()
}
