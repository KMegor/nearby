package com.coursework.nearby.domain.validation

import com.coursework.nearby.domain.model.ProfileDraft
import org.junit.Assert.assertEquals
import org.junit.Test

class ValidatorsTest {
    private val year = 2026
    private val valid = ProfileDraft(name = "Анна", birthYear = 2000, bio = "о себе", interestIds = setOf(1, 2))

    @Test
    fun `valid profile has no errors`() {
        assertEquals(emptySet<ProfileFieldError>(), ProfileValidator.validate(valid, year))
    }

    @Test
    fun `name must not be blank or too long`() {
        assertEquals(setOf(ProfileFieldError.NAME_EMPTY), ProfileValidator.validate(valid.copy(name = "   "), year))
        assertEquals(setOf(ProfileFieldError.NAME_TOO_LONG), ProfileValidator.validate(valid.copy(name = "a".repeat(51)), year))
        assertEquals(emptySet<ProfileFieldError>(), ProfileValidator.validate(valid.copy(name = "a".repeat(50)), year))
    }

    @Test
    fun `user must be at least 18 this year`() {
        assertEquals(emptySet<ProfileFieldError>(), ProfileValidator.validate(valid.copy(birthYear = 2008), year))
        assertEquals(setOf(ProfileFieldError.UNDERAGE), ProfileValidator.validate(valid.copy(birthYear = 2009), year))
    }

    @Test
    fun `birth year must be plausible`() {
        assertEquals(setOf(ProfileFieldError.BIRTH_YEAR_INVALID), ProfileValidator.validate(valid.copy(birthYear = null), year))
        assertEquals(setOf(ProfileFieldError.BIRTH_YEAR_INVALID), ProfileValidator.validate(valid.copy(birthYear = 2030), year))
        assertEquals(setOf(ProfileFieldError.BIRTH_YEAR_INVALID), ProfileValidator.validate(valid.copy(birthYear = 1800), year))
    }

    @Test
    fun `bio and interests are limited`() {
        assertEquals(setOf(ProfileFieldError.BIO_TOO_LONG), ProfileValidator.validate(valid.copy(bio = "a".repeat(201)), year))
        assertEquals(
            setOf(ProfileFieldError.TOO_MANY_INTERESTS),
            ProfileValidator.validate(valid.copy(interestIds = (1..6).toSet()), year),
        )
        assertEquals(emptySet<ProfileFieldError>(), ProfileValidator.validate(valid.copy(interestIds = (1..5).toSet()), year))
    }

    @Test
    fun `credentials are checked before going to the server`() {
        assertEquals(emptySet<CredentialsFieldError>(), CredentialsValidator.validate(" user@mail.ru ", "secret"))
        assertEquals(setOf(CredentialsFieldError.EMAIL_INVALID), CredentialsValidator.validate("user@mail", "secret"))
        assertEquals(setOf(CredentialsFieldError.EMAIL_INVALID), CredentialsValidator.validate("user mail@x.ru", "secret"))
        assertEquals(setOf(CredentialsFieldError.PASSWORD_TOO_SHORT), CredentialsValidator.validate("user@mail.ru", "12345"))
    }
}
