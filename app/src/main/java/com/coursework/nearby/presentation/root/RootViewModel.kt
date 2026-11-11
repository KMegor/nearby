package com.coursework.nearby.presentation.root

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coursework.nearby.domain.model.AppState
import com.coursework.nearby.domain.usecase.ObserveAppStateUseCase
import com.coursework.nearby.domain.usecase.SignOutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RootViewModel @Inject constructor(
    observeAppState: ObserveAppStateUseCase,
    private val signOutUseCase: SignOutUseCase,
) : ViewModel() {

    private val attempt = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<AppState> = attempt
        .flatMapLatest { observeAppState() }
        .stateIn(viewModelScope, SharingStarted.Lazily, AppState.Loading)

    /** Перечитать профиль: после ошибки сети или чтобы проверить, не закончилось ли отстранение. */
    fun retry() = attempt.update { it + 1 }

    fun signOut() {
        viewModelScope.launch { runCatching { signOutUseCase() } }
    }
}
