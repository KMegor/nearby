package com.coursework.nearby.domain.usecase

import com.coursework.nearby.domain.error.AppException
import com.coursework.nearby.domain.model.Interest
import com.coursework.nearby.domain.model.Profile
import com.coursework.nearby.domain.model.ProfileDraft
import com.coursework.nearby.domain.model.SearchRadius
import com.coursework.nearby.domain.model.SessionState
import com.coursework.nearby.domain.repository.AuthRepository
import com.coursework.nearby.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.Instant

class FakeAuthRepository : AuthRepository {
    override val session = MutableStateFlow<SessionState>(SessionState.Loading)
    val calls = mutableListOf<String>()

    override suspend fun signIn(email: String, password: String) {
        calls += "signIn:$email"
        session.value = SessionState.SignedIn("me")
    }

    override suspend fun signUp(email: String, password: String) {
        calls += "signUp:$email"
        session.value = SessionState.SignedIn("me")
    }

    override suspend fun signOut() {
        session.value = SessionState.SignedOut
    }
}

class FakeProfileRepository : ProfileRepository {
    override val myProfile = MutableStateFlow<Profile?>(null)
    var stored: Profile? = null
    var refreshError: AppException? = null
    var savedAvatarUri: String? = null

    override suspend fun refreshMyProfile(): Profile? {
        refreshError?.let { throw it }
        myProfile.value = stored
        return stored
    }

    override suspend fun getInterests(): List<Interest> = listOf(Interest(1, "Музыка"), Interest(2, "Спорт"))

    override suspend fun saveProfile(draft: ProfileDraft, avatarUri: String?): Profile {
        savedAvatarUri = avatarUri
        val profile = profile(name = draft.name, birthYear = draft.birthYear!!, bio = draft.bio)
        stored = profile
        myProfile.value = profile
        return profile
    }
}

fun profile(
    name: String = "Анна",
    birthYear: Int = 2000,
    bio: String = "",
    bannedUntil: Instant? = null,
) = Profile(
    id = "me", name = name, birthYear = birthYear, bio = bio, avatarUrl = null, interests = emptyList(),
    visible = false, radius = SearchRadius.KM_1, bannedUntil = bannedUntil,
)
