package com.coursework.nearby.domain.usecase

import com.coursework.nearby.domain.error.AppError
import com.coursework.nearby.domain.error.AppException
import com.coursework.nearby.domain.model.AppState
import com.coursework.nearby.domain.model.ProfileDraft
import com.coursework.nearby.domain.model.SessionState
import com.coursework.nearby.domain.validation.CredentialsFieldError
import com.coursework.nearby.domain.validation.ProfileFieldError
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class UseCasesTest {
    private val now = Instant.parse("2026-11-12T10:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val auth = FakeAuthRepository()
    private val profiles = FakeProfileRepository()
    private val observe = ObserveAppStateUseCase(auth, profiles, clock)

    @Test
    fun `app state follows session and profile`() = runTest(UnconfinedTestDispatcher()) {
        val states = mutableListOf<AppState>()
        val job = launch { observe().toList(states) }

        auth.session.value = SessionState.SignedOut
        auth.session.value = SessionState.SignedIn("me")
        SaveProfileUseCase(profiles, clock)(ProfileDraft("Анна", 2000, "", emptySet()), null)
        auth.session.value = SessionState.SignedOut
        job.cancel()

        assertEquals(
            listOf(
                AppState.Loading,
                AppState.SignedOut,
                AppState.Loading,
                AppState.NeedsProfile,
                AppState.Ready(profiles.stored!!),
                AppState.SignedOut,
            ),
            states,
        )
    }

    @Test
    fun `banned profile leads to ban screen until the term ends`() = runTest {
        auth.session.value = SessionState.SignedIn("me")

        profiles.stored = profile(bannedUntil = now.plusSeconds(3600))
        assertEquals(AppState.Banned(now.plusSeconds(3600), permanent = false), observe().first { it != AppState.Loading })

        profiles.stored = profile(bannedUntil = Instant.MAX)
        assertEquals(AppState.Banned(Instant.MAX, permanent = true), observe().first { it != AppState.Loading })

        profiles.stored = profile(bannedUntil = now.minusSeconds(1))
        assertTrue(observe().first { it != AppState.Loading } is AppState.Ready)
    }

    @Test
    fun `failed profile load is reported instead of asking to fill the profile`() = runTest {
        auth.session.value = SessionState.SignedIn("me")
        profiles.refreshError = AppException(AppError.Network)
        assertEquals(AppState.Failed(AppError.Network), observe().first { it != AppState.Loading })
    }

    @Test
    fun `invalid credentials never reach the server`() = runTest {
        val result = SignInUseCase(auth)("not-an-email", "123")
        assertEquals(
            AuthResult.Invalid(setOf(CredentialsFieldError.EMAIL_INVALID, CredentialsFieldError.PASSWORD_TOO_SHORT)),
            result,
        )
        assertTrue(auth.calls.isEmpty())
    }

    @Test
    fun `email is trimmed on sign up`() = runTest {
        assertEquals(AuthResult.Success, SignUpUseCase(auth)("  user@mail.ru ", "secret"))
        assertEquals(listOf("signUp:user@mail.ru"), auth.calls)
    }

    @Test
    fun `invalid profile is not saved`() = runTest {
        val result = SaveProfileUseCase(profiles, clock)(ProfileDraft("", 2015, "", emptySet()), null)
        assertEquals(
            SaveProfileResult.Invalid(setOf(ProfileFieldError.NAME_EMPTY, ProfileFieldError.UNDERAGE)),
            result,
        )
        assertEquals(null, profiles.stored)
    }

    @Test
    fun `profile text is trimmed and avatar is passed along`() = runTest {
        val result = SaveProfileUseCase(profiles, clock)(ProfileDraft("  Анна ", 2000, " привет ", setOf(1)), "content://photo")
        val saved = (result as SaveProfileResult.Saved).profile
        assertEquals("Анна", saved.name)
        assertEquals("привет", saved.bio)
        assertEquals("content://photo", profiles.savedAvatarUri)
    }
}
