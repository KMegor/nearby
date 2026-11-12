package com.coursework.nearby.presentation.connections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coursework.nearby.domain.error.AppError
import com.coursework.nearby.domain.error.AppException
import com.coursework.nearby.domain.model.ConnectionLists
import com.coursework.nearby.domain.model.RequestResponse
import com.coursework.nearby.domain.usecase.ObserveConnectionsUseCase
import com.coursework.nearby.domain.usecase.RefreshConnectionsUseCase
import com.coursework.nearby.domain.usecase.RespondToRequestUseCase
import com.coursework.nearby.domain.usecase.UnblockUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConnectionsUiState(
    val lists: ConnectionLists = ConnectionLists(),
    val isLoaded: Boolean = false,
    /** Связи, по которым сейчас выполняется действие. */
    val busy: Set<String> = emptySet(),
    val error: AppError? = null,
)

/**
 * Запросы, чаты и заблокированные. Живёт на уровне вкладок: пока приложение открыто,
 * список перечитывается раз в [POLL_INTERVAL_MS], чтобы новые запросы появлялись сами.
 */
@HiltViewModel
class ConnectionsViewModel @Inject constructor(
    observeConnections: ObserveConnectionsUseCase,
    private val refreshConnections: RefreshConnectionsUseCase,
    private val respondToRequest: RespondToRequestUseCase,
    private val unblock: UnblockUseCase,
) : ViewModel() {

    private data class Local(val isLoaded: Boolean = false, val busy: Set<String> = emptySet(), val error: AppError? = null)

    private val local = MutableStateFlow(Local())

    /** Ничего не выдаёт; существует, пока экран подписан на [state]. */
    private val polling = flow<Unit> {
        while (true) {
            try {
                refreshConnections()
                local.update { it.copy(isLoaded = true) }
            } catch (e: AppException) {
                local.update { it.copy(error = e.error) }
            }
            delay(POLL_INTERVAL_MS)
        }
    }.onStart { emit(Unit) }

    val state: StateFlow<ConnectionsUiState> =
        combine(observeConnections(), local, polling) { lists, local, _ ->
            ConnectionsUiState(lists = lists, isLoaded = local.isLoaded, busy = local.busy, error = local.error)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ConnectionsUiState())

    /** События E5, E6, E7 (с необязательной жалобой E10). */
    fun onRespond(connectionId: String, response: RequestResponse) = act(connectionId) {
        respondToRequest(connectionId, response)
    }

    /** Событие E8. */
    fun onUnblock(connectionId: String) = act(connectionId) { unblock(connectionId) }

    /** Перечитать список сейчас, не дожидаясь очередного опроса. */
    fun onRefresh() {
        viewModelScope.launch {
            try {
                refreshConnections()
            } catch (e: AppException) {
                local.update { it.copy(error = e.error) }
            }
        }
    }

    fun onErrorShown() = local.update { it.copy(error = null) }

    private fun act(connectionId: String, action: suspend () -> Unit) {
        if (connectionId in local.value.busy) return
        local.update { it.copy(busy = it.busy + connectionId, error = null) }
        viewModelScope.launch {
            try {
                action()
            } catch (e: AppException) {
                local.update { it.copy(error = e.error) }
                // Состояние на сервере могло измениться (например, запрос уже не ждёт ответа).
                runCatching { refreshConnections() }
            } finally {
                local.update { it.copy(busy = it.busy - connectionId) }
            }
        }
    }

    private companion object {
        const val POLL_INTERVAL_MS = 20_000L
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
