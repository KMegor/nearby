package com.coursework.nearby.presentation.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.coursework.nearby.R
import com.coursework.nearby.domain.model.SearchRadius
import com.coursework.nearby.presentation.common.messageRes

fun Context.hasFineLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(state: MapUiState, viewModel: MapViewModel) {
    val context = LocalContext.current
    var showRationale by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true
        viewModel.onPermissionChanged(granted)
        if (granted) viewModel.onVisibilityChange(true)
    }

    // Разрешение могли отозвать в настройках, пока приложение было свёрнуто.
    LifecycleResumeEffect(Unit) {
        viewModel.onPermissionChanged(context.hasFineLocationPermission())
        onPauseOrDispose {}
    }
    DisposableEffect(Unit) {
        viewModel.onMapShown(true)
        onDispose { viewModel.onMapShown(false) }
    }

    Box(Modifier.fillMaxSize()) {
        NearbyMap(
            myPoint = state.myPoint.takeIf { state.visible },
            radius = state.radius,
            users = if (state.visible) state.users else emptyList(),
            onUserClick = viewModel::onUserSelected,
            modifier = Modifier.fillMaxSize(),
        )

        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ModeCard(
                state = state,
                onVisibilityChange = { wanted ->
                    when {
                        !wanted -> viewModel.onVisibilityChange(false)
                        context.hasFineLocationPermission() -> viewModel.onVisibilityChange(true)
                        else -> showRationale = true
                    }
                },
                onRadiusChange = viewModel::onRadiusChange,
            )
            state.error?.let { error ->
                ElevatedCard(onClick = viewModel::onErrorShown) {
                    Text(
                        stringResource(error.messageRes()),
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }

    if (showRationale) {
        AlertDialog(
            onDismissRequest = { showRationale = false },
            title = { Text(stringResource(R.string.map_permission_title)) },
            text = { Text(stringResource(R.string.map_permission_text)) },
            confirmButton = {
                TextButton(onClick = {
                    showRationale = false
                    permissionLauncher.launch(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                    )
                }) { Text(stringResource(R.string.map_permission_allow)) }
            },
            dismissButton = {
                TextButton(onClick = { showRationale = false }) { Text(stringResource(R.string.map_permission_later)) }
            },
        )
    }

    state.selectedUser?.let { user ->
        ModalBottomSheet(onDismissRequest = { viewModel.onUserSelected(null) }) {
            UserCard(user = user, isSending = state.isSendingRequest, onSendRequest = { viewModel.onSendRequest(user.id) })
        }
    }
}

/** Переключатель видимости («как у VPN») и выбор радиуса. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModeCard(state: MapUiState, onVisibilityChange: (Boolean) -> Unit, onRadiusChange: (SearchRadius) -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.map_visibility), style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = stringResource(
                            when {
                                !state.visible -> R.string.map_status_hidden
                                !state.hasLocationPermission -> R.string.map_status_no_permission
                                state.myPoint == null -> R.string.map_status_locating
                                state.users.isEmpty() -> R.string.map_status_nobody
                                else -> R.string.map_status_visible
                            },
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = state.visible,
                    onCheckedChange = onVisibilityChange,
                    enabled = !state.isUpdatingMode,
                    modifier = Modifier.testTag("visibility"),
                )
            }
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SearchRadius.entries.forEachIndexed { index, radius ->
                    SegmentedButton(
                        selected = state.radius == radius,
                        onClick = { onRadiusChange(radius) },
                        shape = SegmentedButtonDefaults.itemShape(index, SearchRadius.entries.size),
                        enabled = !state.isUpdatingMode,
                    ) {
                        Text(stringResource(R.string.map_radius_km, radius.km))
                    }
                }
            }
        }
    }
}
