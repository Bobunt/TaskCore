package com.example.taskcore.ui.auth

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class AuthorizationState(
    val login: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val error: String? = null
) {
    val canSubmit: Boolean get() = login.isNotBlank() && password.isNotBlank()
}

class AuthorizationViewModel : ViewModel() {

    private val _state = MutableStateFlow(AuthorizationState())
    val state: StateFlow<AuthorizationState> = _state

    fun onLoginChanged(value: String) {
        _state.update { it.copy(login = value, error = null) }
    }

    fun onPasswordChanged(value: String) {
        _state.update { it.copy(password = value, error = null) }
    }

    fun onLoginClick() {
        val s = _state.value
        if (!s.canSubmit) return

        // Заглушка логики авторизации.
        // Позже заменим на use-case/repository + API.
        _state.update { it.copy(isLoading = true, error = null) }

        // Без корутин пока сделаем простую "проверку"
        val success = (s.login == "admin" && s.password == "admin")

        _state.update {
            it.copy(
                isLoading = false,
                isLoggedIn = success,
                error = if (!success) "Неверный логин или пароль" else null
            )
        }
    }
}