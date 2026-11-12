package com.coursework.nearby.domain.usecase

import com.coursework.nearby.domain.repository.AuthRepository
import com.coursework.nearby.domain.repository.ConnectionRepository
import com.coursework.nearby.domain.validation.CredentialsFieldError
import com.coursework.nearby.domain.validation.CredentialsValidator
import javax.inject.Inject

sealed interface AuthResult {
    data object Success : AuthResult
    data class Invalid(val errors: Set<CredentialsFieldError>) : AuthResult
}

class SignInUseCase @Inject constructor(private val auth: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): AuthResult {
        val errors = CredentialsValidator.validate(email, password)
        if (errors.isNotEmpty()) return AuthResult.Invalid(errors)
        auth.signIn(email.trim(), password)
        return AuthResult.Success
    }
}

class SignUpUseCase @Inject constructor(private val auth: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): AuthResult {
        val errors = CredentialsValidator.validate(email, password)
        if (errors.isNotEmpty()) return AuthResult.Invalid(errors)
        auth.signUp(email.trim(), password)
        return AuthResult.Success
    }
}

class SignOutUseCase @Inject constructor(
    private val auth: AuthRepository,
    private val connections: ConnectionRepository,
) {
    suspend operator fun invoke() {
        // Следующий вошедший не должен увидеть чужие запросы и чаты.
        connections.clear()
        auth.signOut()
    }
}
