package com.dailyme.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.dailyme.app.screens.FileBrowserScreen
import com.dailyme.app.screens.FileViewScreen
import com.dailyme.app.screens.LoginScreen

@Composable
fun App() {
    MaterialTheme {
        val tokenStore = remember { createTokenStore() }
        val client = remember { createHttpClient() }
        val api = remember { GitHubApi(client) { tokenStore.getToken() } }
        val state = remember { AppState() }

        Surface(modifier = Modifier.fillMaxSize()) {
            when (val screen = state.current) {
                is Screen.Login -> LoginScreen(state, tokenStore) {
                    state.push(Screen.Browser(""))
                }

                is Screen.Browser -> FileBrowserScreen(state, api, screen.path)

                is Screen.FileView -> FileViewScreen(state, api, screen.path)
            }
        }
    }
}
