package com.coursework.nearby.domain.usecase

import com.coursework.nearby.domain.model.ThemeMode
import com.coursework.nearby.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveThemeModeUseCase @Inject constructor(private val settings: SettingsRepository) {
    operator fun invoke(): Flow<ThemeMode> = settings.themeMode
}

class SetThemeModeUseCase @Inject constructor(private val settings: SettingsRepository) {
    suspend operator fun invoke(mode: ThemeMode) = settings.setThemeMode(mode)
}
