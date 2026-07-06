package com.dailyme.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

sealed class Screen {
    data object Login : Screen()
    data class Browser(val path: String) : Screen()
    data class FileView(val path: String) : Screen()
}

class AppState {
    var owner by mutableStateOf("mikegr")
    var repo by mutableStateOf("logseq")
    var branch by mutableStateOf("main")

    val backStack = mutableStateListOf<Screen>(Screen.Login)
    val current: Screen get() = backStack.last()

    fun push(screen: Screen) {
        backStack.add(screen)
    }

    fun pop() {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }

    fun reset() {
        backStack.clear()
        backStack.add(Screen.Login)
    }
}
