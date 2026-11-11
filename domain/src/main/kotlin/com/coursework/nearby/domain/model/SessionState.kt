package com.coursework.nearby.domain.model

sealed interface SessionState {
    data object Loading : SessionState
    data object SignedOut : SessionState
    data class SignedIn(val userId: String) : SessionState
}
