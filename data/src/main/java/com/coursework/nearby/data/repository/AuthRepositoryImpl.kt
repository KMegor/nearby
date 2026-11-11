package com.coursework.nearby.data.repository

import com.coursework.nearby.data.remote.remoteCall
import com.coursework.nearby.domain.model.SessionState
import com.coursework.nearby.domain.repository.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val client: SupabaseClient,
) : AuthRepository {

    override val session: Flow<SessionState> = client.auth.sessionStatus.map { status ->
        when (status) {
            is SessionStatus.Authenticated -> SessionState.SignedIn(status.session.user?.id.orEmpty())
            is SessionStatus.NotAuthenticated -> SessionState.SignedOut
            // Не удалось обновить сессию (например, нет сети): она ещё есть, запросы покажут ошибку сети.
            is SessionStatus.RefreshFailure ->
                client.auth.currentUserOrNull()?.let { SessionState.SignedIn(it.id) } ?: SessionState.SignedOut
            is SessionStatus.Initializing -> SessionState.Loading
        }
    }

    override suspend fun signIn(email: String, password: String) = remoteCall {
        client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    override suspend fun signUp(email: String, password: String) {
        remoteCall {
            client.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
        }
    }

    override suspend fun signOut() = remoteCall { client.auth.signOut() }
}
