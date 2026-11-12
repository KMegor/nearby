package com.coursework.nearby.presentation.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coursework.nearby.domain.error.AppError
import com.coursework.nearby.domain.error.AppException
import com.coursework.nearby.domain.model.ComplaintReason
import com.coursework.nearby.domain.model.ConnectionSummary
import com.coursework.nearby.domain.model.Message
import com.coursework.nearby.domain.usecase.BlockInChatUseCase
import com.coursework.nearby.domain.usecase.LoadOlderMessagesUseCase
import com.coursework.nearby.domain.usecase.MarkChatReadUseCase
import com.coursework.nearby.domain.usecase.ObserveChatPartnerUseCase
import com.coursework.nearby.domain.usecase.ObserveMessagesUseCase
import com.coursework.nearby.domain.usecase.SendMessageResult
import com.coursework.nearby.domain.usecase.SendMessageUseCase
import com.coursework.nearby.domain.usecase.SyncChatUseCase
import com.coursework.nearby.domain.validation.MessageError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val partner: ConnectionSummary? = null,
    /** От старых к новым. */
    val messages: List<Message> = emptyList(),
    val input: String = "",
    val isSending: Boolean = false,
    /** Есть ли на сервере более ранние сообщения. */
    val hasOlder: Boolean = true,
    val isLoadingOlder: Boolean = false,
    val inputError: MessageError? = null,
    val error: AppError? = null,
    /** Чат закрыт блокировкой: экран нужно покинуть. */
    val isClosed: Boolean = false,
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeMessages: ObserveMessagesUseCase,
    observePartner: ObserveChatPartnerUseCase,
    syncChat: SyncChatUseCase,
    private val sendMessage: SendMessageUseCase,
    private val loadOlderMessages: LoadOlderMessagesUseCase,
    private val markChatRead: MarkChatReadUseCase,
    private val blockInChat: BlockInChatUseCase,
) : ViewModel() {

    private val connectionId: String = checkNotNull(savedStateHandle[ARG_CONNECTION_ID])

    private data class Local(
        val input: String = "",
        val isSending: Boolean = false,
        val hasOlder: Boolean = true,
        val isLoadingOlder: Boolean = false,
        val inputError: MessageError? = null,
        val error: AppError? = null,
        val isClosed: Boolean = false,
    )

    private val local = MutableStateFlow(Local())
    private val messages = observeMessages(connectionId)

    /** Синхронизация идёт, пока экран подписан на [state]; при сбое остаётся кэш и показывается ошибка. */
    private val syncing = syncChat(connectionId)
        .map<Nothing, Unit> { }
        .onStart { emit(Unit) }
        .catch { e ->
            local.update { it.copy(error = (e as? AppException)?.error ?: AppError.Unknown) }
            emit(Unit)
        }

    val state: StateFlow<ChatUiState> =
        combine(messages, observePartner(connectionId), local, syncing) { messages, partner, local, _ ->
            ChatUiState(
                partner = partner,
                messages = messages,
                input = local.input,
                isSending = local.isSending,
                // Если в кэше меньше страницы, более ранних сообщений на сервере нет.
                hasOlder = local.hasOlder && messages.size >= PAGE_SIZE,
                isLoadingOlder = local.isLoadingOlder,
                inputError = local.inputError,
                error = local.error,
                isClosed = local.isClosed,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ChatUiState())

    init {
        // Открытый чат считается прочитанным: отмечаем при входе и при каждом новом сообщении.
        viewModelScope.launch {
            messages.map { it.lastOrNull()?.id }.distinctUntilChanged().collect {
                runCatching { markChatRead(connectionId) }
            }
        }
    }

    fun onInputChange(value: String) = local.update { it.copy(input = value, inputError = null) }

    /** Событие E9. */
    fun onSend() {
        val current = local.value
        if (current.isSending) return
        local.update { it.copy(isSending = true, error = null) }
        viewModelScope.launch {
            try {
                when (val result = sendMessage(connectionId, current.input)) {
                    SendMessageResult.Sent -> local.update { it.copy(input = "") }
                    is SendMessageResult.Invalid -> local.update { it.copy(inputError = result.error) }
                }
            } catch (e: AppException) {
                local.update { it.copy(error = e.error) }
            } finally {
                local.update { it.copy(isSending = false) }
            }
        }
    }

    fun onLoadOlder() {
        if (local.value.isLoadingOlder) return
        local.update { it.copy(isLoadingOlder = true) }
        viewModelScope.launch {
            try {
                val more = loadOlderMessages(connectionId)
                local.update { it.copy(hasOlder = more) }
            } catch (e: AppException) {
                local.update { it.copy(error = e.error) }
            } finally {
                local.update { it.copy(isLoadingOlder = false) }
            }
        }
    }

    /** Событие E7 с необязательной жалобой E10. */
    fun onBlock(complaint: ComplaintReason?) {
        viewModelScope.launch {
            try {
                blockInChat(connectionId, complaint)
                local.update { it.copy(isClosed = true) }
            } catch (e: AppException) {
                local.update { it.copy(error = e.error) }
            }
        }
    }

    fun onErrorShown() = local.update { it.copy(error = null) }

    companion object {
        const val ARG_CONNECTION_ID = "connectionId"
        private const val STOP_TIMEOUT_MS = 5_000L
        private const val PAGE_SIZE = 50
    }
}
