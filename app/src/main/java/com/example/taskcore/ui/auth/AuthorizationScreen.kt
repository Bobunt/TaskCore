package com.example.taskcore.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun AuthorizationScreen(
    onLoginSuccess: () -> Unit,
    onGoToRegistration: () -> Unit,
    vm: AuthorizationViewModel = viewModel(factory = AuthorizationViewModel .factory)
) {
    val state by vm.state.collectAsState()

    LaunchedEffect(state.isAuthorized) {
        if (state.isAuthorized) onLoginSuccess()
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Авторизация",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.login,
                onValueChange = vm::onLoginChanged,
                label = { Text("Логин или Email") },
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.password,
                onValueChange = vm::onPasswordChanged,
                label = { Text("Пароль") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )

            Spacer(Modifier.height(16.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = vm::onLoginClick,
                enabled = state.canSubmit && !state.isLoading
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(10.dp))
                }
                Text("Войти")
            }

            Spacer(Modifier.height(12.dp))

            TextButton(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                onClick = onGoToRegistration
            ) {
                Text("Нет аккаунта? Регистрация")
            }

            if (state.error != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = state.error!!,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}