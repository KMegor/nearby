package com.coursework.nearby.presentation.common

import java.time.Duration
import java.time.Instant

/** Сколько полных часов осталось до [moment], с округлением вверх: за минуту до срока — 1. */
fun hoursUntil(moment: Instant?, now: Instant = Instant.now()): Int {
    val seconds = moment?.let { Duration.between(now, it).seconds } ?: 0
    return ((seconds + 3599) / 3600).coerceAtLeast(1).toInt()
}

private val TIME_FORMAT = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
private val DATE_FORMAT = java.time.format.DateTimeFormatter.ofPattern("dd.MM")

/** Время для сегодняшних сообщений, дата — для более ранних. */
fun timeLabel(moment: Instant, zone: java.time.ZoneId = java.time.ZoneId.systemDefault(), now: Instant = Instant.now()): String {
    val local = moment.atZone(zone)
    return if (local.toLocalDate() == now.atZone(zone).toLocalDate()) TIME_FORMAT.format(local) else DATE_FORMAT.format(local)
}
