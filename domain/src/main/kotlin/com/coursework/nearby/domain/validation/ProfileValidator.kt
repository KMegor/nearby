package com.coursework.nearby.domain.validation

import com.coursework.nearby.domain.model.ProfileDraft

enum class ProfileFieldError {
    NAME_EMPTY, NAME_TOO_LONG, BIRTH_YEAR_INVALID, UNDERAGE, BIO_TOO_LONG, TOO_MANY_INTERESTS
}

/** Те же ограничения, что проверяет сервер в save_profile; здесь — чтобы показать ошибку у поля. */
object ProfileValidator {
    const val MAX_NAME_LENGTH = 50
    const val MAX_BIO_LENGTH = 200
    const val MAX_INTERESTS = 5
    const val MIN_AGE = 18
    private const val MAX_AGE = 120

    fun validate(draft: ProfileDraft, currentYear: Int): Set<ProfileFieldError> = buildSet {
        val name = draft.name.trim()
        if (name.isEmpty()) add(ProfileFieldError.NAME_EMPTY)
        if (name.length > MAX_NAME_LENGTH) add(ProfileFieldError.NAME_TOO_LONG)

        val year = draft.birthYear
        when {
            year == null || year > currentYear || year < currentYear - MAX_AGE -> add(ProfileFieldError.BIRTH_YEAR_INVALID)
            year > currentYear - MIN_AGE -> add(ProfileFieldError.UNDERAGE)
        }

        if (draft.bio.trim().length > MAX_BIO_LENGTH) add(ProfileFieldError.BIO_TOO_LONG)
        if (draft.interestIds.size > MAX_INTERESTS) add(ProfileFieldError.TOO_MANY_INTERESTS)
    }
}
