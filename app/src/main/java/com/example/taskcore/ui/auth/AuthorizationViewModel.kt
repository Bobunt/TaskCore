package com.example.taskcore.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.taskcore.App
import com.example.taskcore.PasswordHasher
import com.example.taskcore.data.TaskCoreDB
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AuthorizationState(
    val login: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isAuthorized: Boolean = false,
    val error: String? = null
) {
    val canSubmit: Boolean
        get() = login.isNotBlank() && password.isNotBlank()
}
@Suppress("UNCHECKED_CAST")
class AuthorizationViewModel(
    private val database: TaskCoreDB
) : ViewModel() {

    private val usersDao = database.userDao

    private val _state = MutableStateFlow(AuthorizationState())
    val state: StateFlow<AuthorizationState> = _state

    fun onLoginChanged(v: String) = _state.update { it.copy(login = v.trim(), error = null) }
    fun onPasswordChanged(v: String) = _state.update { it.copy(password = v, error = null) }

    fun onLoginClick() {
        val s = _state.value
        if (!s.canSubmit) return

        _state.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                val login = s.login.trim()

                val user = withContext(Dispatchers.IO) {
                    usersDao.getByLogin(login)
                }

                if (user == null) {
                    _state.update { it.copy(isLoading = false, error = "Неверный логин или пароль") }
                    return@launch
                }

                val expectedHash = user.passwordHash
                val actualHash = PasswordHasher.hashPassword(s.password, user.salt)

                if (actualHash != expectedHash) {
                    _state.update { it.copy(isLoading = false, error = "Неверный логин или пароль") }
                    return@launch
                }

                _state.update { it.copy(isLoading = false, isAuthorized = true) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Ошибка авторизации") }
            }
        }
    }

    fun consumeAuthorized() {
        _state.update { it.copy(isAuthorized = false) }
    }

    companion object {
        val factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                val database = (checkNotNull(extras[APPLICATION_KEY]) as App).database
                return AuthorizationViewModel(database) as T
            }
        }
    }
}