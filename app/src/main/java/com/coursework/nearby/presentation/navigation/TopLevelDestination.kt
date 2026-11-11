package com.coursework.nearby.presentation.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.coursework.nearby.R

/** Четыре вкладки нижней панели. */
enum class TopLevelDestination(
    val route: String,
    @param:StringRes val labelRes: Int,
    @param:DrawableRes val iconRes: Int,
) {
    MAP("map", R.string.tab_map, R.drawable.ic_tab_map),
    REQUESTS("requests", R.string.tab_requests, R.drawable.ic_tab_requests),
    CHATS("chats", R.string.tab_chats, R.drawable.ic_tab_chats),
    PROFILE("profile", R.string.tab_profile, R.drawable.ic_tab_profile),
}
