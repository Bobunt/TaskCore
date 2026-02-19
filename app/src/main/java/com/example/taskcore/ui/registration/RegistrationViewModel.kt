package com.example.taskcore.ui.registration

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class RegistrationState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val passwordRepeat: String = "",
    val isLoading: Boolean = false,
    val isRegistered: Boolean = false,
    val error: String? = null
) {
    val canSubmit: Boolean
        get() = name.isNotBlank() &&
                email.isNotBlank() &&
                password.isNotBlank() &&
                passwordRepeat.isNotBlank()
}

class RegistrationViewModel : ViewModel() {

    private val _state = MutableStateFlow(RegistrationState())
    val state: StateFlow<RegistrationState> = _state

    fun onNameChanged(v: String) = _state.update { it.copy(name = v, error = null) }
    fun onEmailChanged(v: String) = _state.update { it.copy(email = v, error = null) }
    fun onPasswordChanged(v: String) = _state.update { it.copy(password = v, error = null) }
    fun onPasswordRepeatChanged(v: String) = _state.update { it.copy(passwordRepeat = v, error = null) }

    fun onRegisterClick() {
        val s = _state.value

        if (!s.canSubmit) return
        if (s.password != s.passwordRepeat) {
            _state.update { it.copy(error = "Пароли не совпадают") }
            return
        }

        // Заглушка: регистрация "успешна" всегда
        _state.update { it.copy(isLoading = true, error = null) }

        _state.update {
            it.copy(
                isLoading = false,
                isRegistered = true
            )
        }
    }
}