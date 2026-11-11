package com.coursework.nearby.data.repository

import com.coursework.nearby.data.media.AvatarEncoder
import com.coursework.nearby.data.remote.InterestDto
import com.coursework.nearby.data.remote.ProfileDto
import com.coursework.nearby.data.remote.ProfileInterestDto
import com.coursework.nearby.data.remote.remoteCall
import com.coursework.nearby.domain.error.AppError
import com.coursework.nearby.domain.error.AppException
import com.coursework.nearby.domain.error.ServerError
import com.coursework.nearby.domain.model.Interest
import com.coursework.nearby.domain.model.Profile
import com.coursework.nearby.domain.model.ProfileDraft
import com.coursework.nearby.domain.repository.ProfileRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import javax.inject.Inject

class ProfileRepositoryImpl @Inject constructor(
    private val client: SupabaseClient,
    private val avatarEncoder: AvatarEncoder,
) : ProfileRepository {

    private val profile = MutableStateFlow<Profile?>(null)
    override val myProfile: StateFlow<Profile?> = profile.asStateFlow()

    private var interestsCache: List<Interest>? = null
    private val avatars get() = client.storage.from(AVATARS_BUCKET)

    override suspend fun refreshMyProfile(): Profile? = remoteCall {
        // Правила доступа отдают из profiles и profile_interests только строки текущего пользователя.
        val dto = client.from("profiles").select().decodeSingleOrNull<ProfileDto>()
        val loaded = dto?.let {
            val ids = client.from("profile_interests").select().decodeList<ProfileInterestDto>()
                .map { it.interestId }.toSet()
            dto.toDomain(
                interests = getInterests().filter { it.id in ids },
                avatarUrl = dto.avatarPath?.let { avatars.publicUrl(it) },
            )
        }
        profile.value = loaded
        loaded
    }

    override suspend fun getInterests(): List<Interest> = interestsCache ?: remoteCall {
        client.from("interests").select().decodeList<InterestDto>()
            .sortedBy { it.id }
            .map { Interest(it.id, it.name) }
            .also { interestsCache = it }
    }

    override suspend fun saveProfile(draft: ProfileDraft, avatarUri: String?): Profile = remoteCall {
        val previous = client.postgrest.rpc(
            "save_profile",
            buildJsonObject {
                put("p_name", draft.name)
                put("p_birth_year", draft.birthYear)
                put("p_bio", draft.bio)
                putJsonArray("p_interest_ids") { draft.interestIds.sorted().forEach { add(it) } }
            },
        ).decodeAs<ProfileDto>()

        if (avatarUri != null) uploadAvatar(avatarUri, previousPath = previous.avatarPath)
        refreshMyProfile() ?: throw AppException(AppError.Server(ServerError.PROFILE_REQUIRED))
    }

    private suspend fun uploadAvatar(uri: String, previousPath: String?) {
        val userId = client.auth.currentUserOrNull()?.id
            ?: throw AppException(AppError.Server(ServerError.NOT_AUTHENTICATED))
        // Новое имя файла при каждой загрузке, чтобы у других не оставалась старая картинка в кэше.
        val path = "$userId/${System.currentTimeMillis()}.jpg"
        avatars.upload(path, avatarEncoder.encode(uri)) { contentType = ContentType.Image.JPEG }
        client.postgrest.rpc("set_avatar", buildJsonObject { put("p_path", path) })
        if (previousPath != null) runCatching { avatars.delete(previousPath) }
    }

    private companion object {
        const val AVATARS_BUCKET = "avatars"
    }
}
