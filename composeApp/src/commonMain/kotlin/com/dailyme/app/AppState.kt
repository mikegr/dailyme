package com.dailyme.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

sealed class Screen {
    data object Start : Screen()
    data object Login : Screen()
    data class Browser(val path: String) : Screen()
    data class FileView(val path: String) : Screen()
}

class AppState {
    var owner by mutableStateOf("mikegr")
    var repo by mutableStateOf("logseq")
    var branch by mutableStateOf("master")
    var isLoggedIn by mutableStateOf(false)
    var sortDescending by mutableStateOf(false)

    val backStack = mutableStateListOf<Screen>(Screen.Start)
    val current: Screen get() = backStack.last()

    fun push(screen: Screen) {
        backStack.add(screen)
    }

    fun pop() {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }

    fun reset() {
        backStack.clear()
        backStack.add(Screen.Start)
    }
}
