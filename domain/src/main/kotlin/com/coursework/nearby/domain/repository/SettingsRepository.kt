package com.coursework.nearby.domain.repository

import com.coursework.nearby.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

/** Настройки, которые хранятся только на устройстве. */
interface SettingsRepository {
    val themeMode: Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
}
