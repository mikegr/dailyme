package com.dailyme.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dailyme.app.screens.FileBrowserScreen
import com.dailyme.app.screens.FileViewScreen
import com.dailyme.app.screens.LoginScreen
import com.dailyme.app.screens.StartScreen

@Composable
fun App() {
    MaterialTheme {
        val credentialsStore = remember { createCredentialsStore() }
        val client = remember { createHttpClient() }
        val api = remember { GitHubApi(client) { credentialsStore.load()?.token } }
        val state = remember { AppState() }
        var isRestoringSession by remember { mutableStateOf(true) }

        LaunchedEffect(Unit) {
            val stored = credentialsStore.load()
            if (stored != null) {
                state.owner = stored.owner
                state.repo = stored.repo
                state.branch = stored.branch
                state.isLoggedIn = true
            }
            isRestoringSession = false
        }

        Surface(modifier = Modifier.fillMaxSize()) {
            when {
                isRestoringSession -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                else -> when (val screen = state.current) {
                    is Screen.Start -> StartScreen(state, credentialsStore)

                    is Screen.Login -> LoginScreen(state, credentialsStore) {
                        state.isLoggedIn = true
                        state.pop()
                    }

                    is Screen.Browser -> FileBrowserScreen(state, api, credentialsStore, screen.path)

                    is Screen.FileView -> FileViewScreen(state, api, screen.path)
                }
            }
        }
    }
}
