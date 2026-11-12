package com.coursework.nearby.domain.model

/** Оформление приложения. По умолчанию тёмное — основное для этого приложения. */
enum class ThemeMode {
    DARK, LIGHT, SYSTEM;

    companion object {
        val DEFAULT = DARK
        fun fromName(name: String?): ThemeMode = entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
