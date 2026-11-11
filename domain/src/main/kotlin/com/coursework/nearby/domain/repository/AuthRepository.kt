package com.coursework.nearby.domain.repository

import com.coursework.nearby.domain.model.SessionState
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val session: Flow<SessionState>
    suspend fun signIn(email: String, password: String)
    suspend fun signUp(email: String, password: String)
    suspend fun signOut()
}
