package com.example.taskcore.ui.registration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.taskcore.App
import com.example.taskcore.data.security.PasswordHasher
import com.example.taskcore.data.TaskCoreDB
import com.example.taskcore.data.tables.Users
import com.example.taskcore.ui.common.dbViewModelFactory
import com.example.taskcore.ui.task.TaskViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class RegistrationState(
    val login: String = "",
    val email: String = "",
    val password: String = "",
    val passwordRepeat: String = "",
    val isLoading: Boolean = false,
    val isRegistered: Boolean = false,
    val error: String? = null
) {
    val canSubmit: Boolean
        get() = login.isNotBlank() &&
                email.isNotBlank() &&
                password.isNotBlank() &&
                passwordRepeat.isNotBlank()
}

@Suppress("UNCHECKED_CAST")
class RegistrationViewModel(
    private val database: TaskCoreDB
) : ViewModel() {

    private val usersDao = database.userDao

    private val _state = MutableStateFlow(RegistrationState())
    val state: StateFlow<RegistrationState> = _state

    fun onNameChanged(v: String) = _state.update { it.copy(login = v, error = null) }
    fun onEmailChanged(v: String) = _state.update { it.copy(email = v.trim(), error = null) }
    fun onPasswordChanged(v: String) = _state.update { it.copy(password = v, error = null) }
    fun onPasswordRepeatChanged(v: String) = _state.update { it.copy(passwordRepeat = v, error = null) }

    fun onRegisterClick() {
        val s = _state.value

        if (!s.canSubmit) return

        if (s.password != s.passwordRepeat) {
            _state.update { it.copy(error = "Пароли не совпадают") }
            return
        }

        if (s.password.length < 6) {
            _state.update { it.copy(error = "Пароль должен быть не короче 6 символов") }
            return
        }

        _state.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                val email = s.email.trim()
                val login = s.login.trim()

                // Проверяем, существует ли пользователь
                val exists = withContext(Dispatchers.IO) {
                    usersDao.existsByLogin(email)
                }

                if (exists) {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = "Пользователь с таким email уже существует"
                        )
                    }
                    return@launch
                }

                // Генерация salt и hash
                val salt = PasswordHasher.generateSalt()
                val hash = PasswordHasher.hashPassword(s.password, salt)

                val user = Users(
                    email = email,
                    login = login,
                    passwordHash = hash,
                    salt = salt,
                    createdAtTimestamp = System.currentTimeMillis()
                )

                withContext(Dispatchers.IO) {
                    usersDao.insert(user)
                }

                _state.update {
                    it.copy(
                        isLoading = false,
                        isRegistered = true
                    )
                }

            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Ошибка регистрации"
                    )
                }
            }
        }
    }

    companion object {
        val factory = dbViewModelFactory { RegistrationViewModel(it) }
    }
}