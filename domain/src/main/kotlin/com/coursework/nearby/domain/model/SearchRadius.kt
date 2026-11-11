package com.coursework.nearby.domain.model

/** Допустимые значения радиуса поиска (инвариант «Диапазон радиуса»). */
enum class SearchRadius(val km: Int) {
    KM_1(1), KM_3(3), KM_5(5), KM_10(10);

    companion object {
        val DEFAULT = KM_1
        fun fromKm(km: Int): SearchRadius = entries.firstOrNull { it.km == km } ?: DEFAULT
    }
}
