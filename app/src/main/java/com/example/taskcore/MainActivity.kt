package com.example.taskcore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.taskcore.ui.AppView
import com.example.taskcore.ui.theme.TaskCoreTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TaskCoreTheme {
                AppView()
            }
        }
    }
}


@Preview
@Composable
fun GreetingPreview() {
    TaskCoreTheme {
        AppView()
    }
}