package com.coursework.nearby.domain.repository

import com.coursework.nearby.domain.model.Interest
import com.coursework.nearby.domain.model.Profile
import com.coursework.nearby.domain.model.ProfileDraft
import kotlinx.coroutines.flow.StateFlow

interface ProfileRepository {
    /** Профиль текущего пользователя; null — не заполнен или ещё не загружен. */
    val myProfile: StateFlow<Profile?>

    suspend fun refreshMyProfile(): Profile?
    suspend fun getInterests(): List<Interest>

    /**
     * Сохраняет профиль и, если задан [avatarUri], загружает новое фото.
     * [myProfile] обновляется один раз, когда выполнено всё.
     */
    suspend fun saveProfile(draft: ProfileDraft, avatarUri: String?): Profile
}
