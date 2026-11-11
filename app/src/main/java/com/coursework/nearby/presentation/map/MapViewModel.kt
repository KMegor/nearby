package com.coursework.nearby.presentation.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coursework.nearby.domain.error.AppError
import com.coursework.nearby.domain.error.AppException
import com.coursework.nearby.domain.model.GeoPoint
import com.coursework.nearby.domain.model.NearbyUser
import com.coursework.nearby.domain.model.SearchRadius
import com.coursework.nearby.domain.usecase.GetNearbyUsersUseCase
import com.coursework.nearby.domain.usecase.ObserveMyProfileUseCase
import com.coursework.nearby.domain.usecase.SendRequestUseCase
import com.coursework.nearby.domain.usecase.SetRadiusUseCase
import com.coursework.nearby.domain.usecase.SetVisibilityUseCase
import com.coursework.nearby.domain.usecase.TrackLocationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MapUiState(
    val visible: Boolean = false,
    val radius: SearchRadius = SearchRadius.DEFAULT,
    val hasLocationPermission: Boolean = false,
    /** Собственная огрублённая точка — та же, что известна серверу. */
    val myPoint: GeoPoint? = null,
    val users: List<NearbyUser> = emptyList(),
    val selectedUser: NearbyUser? = null,
    /** Идёт смена видимости или радиуса. */
    val isUpdatingMode: Boolean = false,
    val isSendingRequest: Boolean = false,
    val error: AppError? = null,
)

/**
 * Режим пользователя (S0/S1) и карта. Живёт на уровне вкладок: пока приложение открыто
 * и видимость включена, местоположение передаётся серверу независимо от выбранной вкладки.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MapViewModel @Inject constructor(
    observeMyProfile: ObserveMyProfileUseCase,
    trackLocation: TrackLocationUseCase,
    private val getNearbyUsers: GetNearbyUsersUseCase,
    private val setVisibility: SetVisibilityUseCase,
    private val setRadius: SetRadiusUseCase,
    private val sendRequest: SendRequestUseCase,
) : ViewModel() {

    private data class Local(
        val selectedUserId: String? = null,
        val isUpdatingMode: Boolean = false,
        val isSendingRequest: Boolean = false,
        val error: AppError? = null,
    )

    private val profile = observeMyProfile()
    private val hasPermission = MutableStateFlow(false)
    private val mapShown = MutableStateFlow(false)
    private val refreshRequests = MutableStateFlow(0)
    private val local = MutableStateFlow(Local())

    private val myPoint: StateFlow<GeoPoint?> =
        combine(profile.map { it?.visible == true }, hasPermission) { visible, permitted -> visible && permitted }
            .distinctUntilChanged()
            .flatMapLatest { tracking ->
                if (!tracking) {
                    flowOf(null)
                } else {
                    trackLocation()
                        .map<GeoPoint, GeoPoint?> { it }
                        .onStart { emit(null) }
                        .catch { report(it) }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    /** Список обновляется, пока открыта карта: после каждой смены условий и раз в [POLL_INTERVAL_MS]. */
    private val users: Flow<List<NearbyUser>> =
        combine(myPoint, profile.map { it?.radius }.distinctUntilChanged(), mapShown, refreshRequests) { point, _, shown, _ ->
            point != null && shown
        }.flatMapLatest { active ->
            if (!active) {
                flowOf(emptyList())
            } else {
                flow {
                    while (true) {
                        try {
                            emit(getNearbyUsers())
                        } catch (e: AppException) {
                            report(e)
                        }
                        delay(POLL_INTERVAL_MS)
                    }
                }
            }
        }

    val state: StateFlow<MapUiState> =
        combine(profile, hasPermission, myPoint, users, local) { profile, permitted, point, users, local ->
            MapUiState(
                visible = profile?.visible == true,
                radius = profile?.radius ?: SearchRadius.DEFAULT,
                hasLocationPermission = permitted,
                myPoint = point,
                users = users,
                selectedUser = users.firstOrNull { it.id == local.selectedUserId },
                isUpdatingMode = local.isUpdatingMode,
                isSendingRequest = local.isSendingRequest,
                error = local.error,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), MapUiState())

    fun onPermissionChanged(granted: Boolean) {
        hasPermission.value = granted
    }

    fun onMapShown(shown: Boolean) {
        mapShown.value = shown
    }

    /** События E1 и E2. Разрешение на геолокацию экран запрашивает до вызова. */
    fun onVisibilityChange(visible: Boolean) = updateMode { setVisibility(visible) }

    /** Событие E3. */
    fun onRadiusChange(radius: SearchRadius) = updateMode { setRadius(radius) }

    fun onUserSelected(userId: String?) = local.update { it.copy(selectedUserId = userId) }

    /** Событие E4. */
    fun onSendRequest(userId: String) {
        if (local.value.isSendingRequest) return
        local.update { it.copy(isSendingRequest = true, error = null) }
        viewModelScope.launch {
            try {
                sendRequest(userId)
            } catch (e: AppException) {
                report(e)
            } finally {
                local.update { it.copy(isSendingRequest = false) }
                refreshRequests.update { it + 1 }
            }
        }
    }

    fun onErrorShown() = local.update { it.copy(error = null) }

    private fun updateMode(action: suspend () -> Unit) {
        if (local.value.isUpdatingMode) return
        local.update { it.copy(isUpdatingMode = true, error = null) }
        viewModelScope.launch {
            try {
                action()
            } catch (e: AppException) {
                report(e)
            } finally {
                local.update { it.copy(isUpdatingMode = false) }
            }
        }
    }

    private fun report(e: Throwable) {
        if (e is CancellationException) throw e
        local.update { it.copy(error = (e as? AppException)?.error ?: AppError.Unknown) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val POLL_INTERVAL_MS = 30_000L
    }
}
