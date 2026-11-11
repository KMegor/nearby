package com.coursework.nearby.domain.usecase

import com.coursework.nearby.domain.model.Interest
import com.coursework.nearby.domain.model.Profile
import com.coursework.nearby.domain.model.ProfileDraft
import com.coursework.nearby.domain.repository.ProfileRepository
import com.coursework.nearby.domain.validation.ProfileFieldError
import com.coursework.nearby.domain.validation.ProfileValidator
import java.time.Clock
import java.time.ZoneOffset
import javax.inject.Inject

sealed interface SaveProfileResult {
    data class Saved(val profile: Profile) : SaveProfileResult
    data class Invalid(val errors: Set<ProfileFieldError>) : SaveProfileResult
}

class SaveProfileUseCase @Inject constructor(
    private val profiles: ProfileRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(draft: ProfileDraft, avatarUri: String?): SaveProfileResult {
        val currentYear = clock.instant().atOffset(ZoneOffset.UTC).year
        val errors = ProfileValidator.validate(draft, currentYear)
        if (errors.isNotEmpty()) return SaveProfileResult.Invalid(errors)
        val cleaned = draft.copy(name = draft.name.trim(), bio = draft.bio.trim())
        return SaveProfileResult.Saved(profiles.saveProfile(cleaned, avatarUri))
    }
}

class GetInterestsUseCase @Inject constructor(private val profiles: ProfileRepository) {
    suspend operator fun invoke(): List<Interest> = profiles.getInterests()
}

class GetMyProfileUseCase @Inject constructor(private val profiles: ProfileRepository) {
    operator fun invoke(): Profile? = profiles.myProfile.value
}
