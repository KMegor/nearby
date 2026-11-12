package com.coursework.nearby.presentation.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coursework.nearby.domain.model.ThemeMode
import com.coursework.nearby.domain.usecase.ObserveThemeModeUseCase
import com.coursework.nearby.domain.usecase.SetThemeModeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    observeThemeMode: ObserveThemeModeUseCase,
    private val setThemeMode: SetThemeModeUseCase,
) : ViewModel() {

    val mode: StateFlow<ThemeMode> =
        observeThemeMode().stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.DEFAULT)

    fun onModeChange(mode: ThemeMode) {
        viewModelScope.launch { setThemeMode(mode) }
    }
}
