package com.coursework.nearby.domain.validation

enum class CredentialsFieldError { EMAIL_INVALID, PASSWORD_TOO_SHORT }

object CredentialsValidator {
    const val MIN_PASSWORD_LENGTH = 6
    private val EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}$")

    fun validate(email: String, password: String): Set<CredentialsFieldError> = buildSet {
        if (!EMAIL.matches(email.trim())) add(CredentialsFieldError.EMAIL_INVALID)
        if (password.length < MIN_PASSWORD_LENGTH) add(CredentialsFieldError.PASSWORD_TOO_SHORT)
    }
}
