package com.example.taskcore.ui
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.taskcore.ui.auth.AuthorizationScreen
import com.example.taskcore.ui.registration.RegistrationScreen
import com.example.taskcore.ui.task.TaskScreen
import com.example.taskcore.ui.taskslist.TaskListScreen

object Routes {
    const val Authorization = "authorization"
    const val Registration = "registration"
    const val TaskList = "task_list"

    const val Task = "task"
    const val TaskWithId = "task/{taskId}"
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppView() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.Authorization
    ) {
        composable(Routes.Registration) {
            RegistrationScreen(
                onBackToAuthorization = { navController.popBackStack() },
                onRegistrationSuccess = {
                    navController.navigate(Routes.TaskList) {
                        popUpTo(Routes.Authorization) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.TaskList) {
            TaskListScreen(
                onTaskClick = { taskId -> navController.navigate("task/$taskId") },
                onCreateTaskClick = { navController.navigate("task") }
            )
        }

        composable(Routes.Task) {

            // Создание новой задачи
            TaskScreen(
                taskId = null,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.TaskWithId,
            arguments = listOf(navArgument("taskId") { type = NavType.StringType })
        ) { entry ->
            val taskId = entry.arguments?.getString("taskId")
            TaskScreen(
                taskId = taskId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.Authorization) {
            AuthorizationScreen(
                onLoginSuccess = { navController.navigate(Routes.TaskList) },
                onGoToRegistration = { navController.navigate(Routes.Registration) }
            )
        }
    }
}