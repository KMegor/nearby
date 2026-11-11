package com.coursework.nearby.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coursework.nearby.domain.model.Profile
import com.coursework.nearby.presentation.map.MapScreen
import com.coursework.nearby.presentation.map.MapViewModel
import com.coursework.nearby.presentation.map.hasFineLocationPermission
import com.coursework.nearby.presentation.placeholder.PlaceholderScreen
import com.coursework.nearby.presentation.profile.ProfileEditScreen
import com.coursework.nearby.presentation.profile.ProfileScreen

private const val ROUTE_PROFILE_EDIT = "profile/edit"

/** Основное приложение: четыре вкладки и экраны, открываемые поверх них. */
@Composable
fun MainTabs(profile: Profile, onSignOut: () -> Unit) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val onTab = TopLevelDestination.entries.any { it.route == currentDestination?.route }

    // Собирается на уровне вкладок, чтобы местоположение передавалось при любой открытой вкладке.
    val mapViewModel: MapViewModel = hiltViewModel()
    val mapState by mapViewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LifecycleResumeEffect(Unit) {
        mapViewModel.onPermissionChanged(context.hasFineLocationPermission())
        onPauseOrDispose {}
    }

    Scaffold(
        bottomBar = {
            if (onTab) {
                NavigationBar {
                    TopLevelDestination.entries.forEach { destination ->
                        NavigationBarItem(
                            selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true,
                            onClick = {
                                navController.navigate(destination.route) {
                                    // Вкладки не накапливаются в стеке и сохраняют своё состояние.
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(painterResource(destination.iconRes), contentDescription = null) },
                            label = { Text(stringResource(destination.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopLevelDestination.MAP.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(TopLevelDestination.MAP.route) {
                MapScreen(state = mapState, viewModel = mapViewModel)
            }
            listOf(TopLevelDestination.REQUESTS, TopLevelDestination.CHATS).forEach { destination ->
                composable(destination.route) {
                    PlaceholderScreen(titleRes = destination.labelRes, iconRes = destination.iconRes)
                }
            }
            composable(TopLevelDestination.PROFILE.route) {
                ProfileScreen(
                    profile = profile,
                    onEdit = { navController.navigate(ROUTE_PROFILE_EDIT) },
                    onSignOut = onSignOut,
                )
            }
            composable(ROUTE_PROFILE_EDIT) {
                ProfileEditScreen(isSetup = false, onDone = { navController.popBackStack() }, onSignOut = null)
            }
        }
    }
}
