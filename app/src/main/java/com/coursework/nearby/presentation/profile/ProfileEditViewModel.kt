package com.coursework.nearby.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coursework.nearby.domain.error.AppError
import com.coursework.nearby.domain.error.AppException
import com.coursework.nearby.domain.model.Interest
import com.coursework.nearby.domain.model.ProfileDraft
import com.coursework.nearby.domain.usecase.GetInterestsUseCase
import com.coursework.nearby.domain.usecase.GetMyProfileUseCase
import com.coursework.nearby.domain.usecase.SaveProfileResult
import com.coursework.nearby.domain.usecase.SaveProfileUseCase
import com.coursework.nearby.domain.validation.ProfileFieldError
import com.coursework.nearby.domain.validation.ProfileValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileEditUiState(
    val name: String = "",
    val birthYear: String = "",
    val bio: String = "",
    val selectedInterests: Set<Int> = emptySet(),
    val allInterests: List<Interest> = emptyList(),
    /** Уже сохранённое фото. */
    val avatarUrl: String? = null,
    /** Выбранное, но ещё не загруженное фото; отправляется при сохранении. */
    val pendingAvatarUri: String? = null,
    val fieldErrors: Set<ProfileFieldError> = emptySet(),
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: AppError? = null,
)

@HiltViewModel
class ProfileEditViewModel @Inject constructor(
    getMyProfile: GetMyProfileUseCase,
    private val getInterests: GetInterestsUseCase,
    private val saveProfile: SaveProfileUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(
        getMyProfile()?.let { profile ->
            ProfileEditUiState(
                name = profile.name,
                birthYear = profile.birthYear.toString(),
                bio = profile.bio,
                selectedInterests = profile.interests.map { it.id }.toSet(),
                avatarUrl = profile.avatarUrl,
            )
        } ?: ProfileEditUiState(),
    )
    val state: StateFlow<ProfileEditUiState> = _state.asStateFlow()

    init {
        loadInterests()
    }

    fun loadInterests() {
        viewModelScope.launch {
            try {
                val interests = getInterests()
                _state.update { it.copy(allInterests = interests, error = null) }
            } catch (e: AppException) {
                _state.update { it.copy(error = e.error) }
            }
        }
    }

    fun onNameChange(value: String) = _state.update {
        it.copy(name = value, fieldErrors = it.fieldErrors - ProfileFieldError.NAME_EMPTY - ProfileFieldError.NAME_TOO_LONG)
    }

    fun onBirthYearChange(value: String) = _state.update {
        it.copy(
            birthYear = value.filter(Char::isDigit).take(4),
            fieldErrors = it.fieldErrors - ProfileFieldError.BIRTH_YEAR_INVALID - ProfileFieldError.UNDERAGE,
        )
    }

    fun onBioChange(value: String) = _state.update {
        it.copy(bio = value, fieldErrors = it.fieldErrors - ProfileFieldError.BIO_TOO_LONG)
    }

    fun onInterestToggle(id: Int) = _state.update {
        when {
            id in it.selectedInterests -> it.copy(selectedInterests = it.selectedInterests - id)
            it.selectedInterests.size < ProfileValidator.MAX_INTERESTS -> it.copy(selectedInterests = it.selectedInterests + id)
            else -> it
        }
    }

    fun onAvatarPicked(uri: String?) {
        if (uri != null) _state.update { it.copy(pendingAvatarUri = uri) }
    }

    fun onSave() {
        val current = _state.value
        if (current.isSaving) return
        _state.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            try {
                val draft = ProfileDraft(current.name, current.birthYear.toIntOrNull(), current.bio, current.selectedInterests)
                when (val result = saveProfile(draft, current.pendingAvatarUri)) {
                    is SaveProfileResult.Invalid -> _state.update { it.copy(fieldErrors = result.errors) }
                    is SaveProfileResult.Saved -> _state.update { it.copy(isSaved = true) }
                }
            } catch (e: AppException) {
                _state.update { it.copy(error = e.error) }
            } finally {
                _state.update { it.copy(isSaving = false) }
            }
        }
    }
}
