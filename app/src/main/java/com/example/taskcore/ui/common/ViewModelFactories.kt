package com.example.taskcore.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.taskcore.App
import com.example.taskcore.data.TaskCoreDB

inline fun <reified VM : ViewModel> dbViewModelFactory(
    crossinline creator: (TaskCoreDB) -> VM
): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val db = (checkNotNull(extras[APPLICATION_KEY]) as App).database
        return creator(db) as T
    }
}