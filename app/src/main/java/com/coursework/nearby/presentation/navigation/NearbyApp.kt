package com.coursework.nearby.presentation.navigation

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coursework.nearby.domain.model.AppState
import com.coursework.nearby.presentation.auth.AuthScreen
import com.coursework.nearby.presentation.ban.BanScreen
import com.coursework.nearby.presentation.common.ErrorScreen
import com.coursework.nearby.presentation.common.LoadingScreen
import com.coursework.nearby.presentation.profile.ProfileEditScreen
import com.coursework.nearby.presentation.root.RootViewModel

/** Верхний уровень: какой из разделов приложения показывать при текущем состоянии аккаунта. */
@Composable
fun NearbyApp(viewModel: RootViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (val current = state) {
            AppState.Loading -> LoadingScreen()
            AppState.SignedOut -> AuthScreen(modifier = Modifier.safeDrawingPadding())
            AppState.NeedsProfile -> ProfileEditScreen(
                isSetup = true,
                onDone = {},
                onSignOut = viewModel::signOut,
                modifier = Modifier.safeDrawingPadding(),
            )
            is AppState.Banned -> BanScreen(
                state = current,
                onCheckAgain = viewModel::retry,
                onSignOut = viewModel::signOut,
                modifier = Modifier.safeDrawingPadding(),
            )
            is AppState.Failed -> ErrorScreen(current.error, onRetry = viewModel::retry, onSignOut = viewModel::signOut)
            is AppState.Ready -> MainTabs(profile = current.profile, onSignOut = viewModel::signOut)
        }
    }
}
