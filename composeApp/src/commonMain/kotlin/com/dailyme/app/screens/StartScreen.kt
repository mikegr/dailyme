package com.dailyme.app.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dailyme.app.AppState
import com.dailyme.app.CredentialsStore
import com.dailyme.app.RepositoryClient
import com.dailyme.app.Screen
import kotlinx.coroutines.launch

@Composable
fun StartScreen(state: AppState, credentialsStore: CredentialsStore, repositoryClient: RepositoryClient) {
    val scope = rememberCoroutineScope()
    var showLogoutConfirm by remember { mutableStateOf(false) }
    val unpushedCommits by repositoryClient.unpushedCommits.collectAsState()

    if (showLogoutConfirm) {
        ConfirmDialog(
            title = "Log out?",
            text = "You'll need to enter your personal access token again to reconnect.",
            confirmLabel = "Log out",
            onConfirm = {
                showLogoutConfirm = false
                scope.launch {
                    credentialsStore.clear()
                    state.isLoggedIn = false
                }
            },
            onDismiss = { showLogoutConfirm = false },
        )
    }

    Scaffold(
        bottomBar = {
            if (state.isLoggedIn) {
                BottomAppBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    TextButton(
                        onClick = { state.push(Screen.SyncLog) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            Icons.Default.Sync,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                        Text(
                            if (unpushedCommits.isNotEmpty()) {
                                "Sync status — ${unpushedCommits.size} commit(s) waiting"
                            } else {
                                "Sync status"
                            },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(modifier = Modifier.align(Alignment.TopEnd)) {
                if (state.isLoggedIn) {
                    IconButton(onClick = { showLogoutConfirm = true }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Log out")
                    }
                }
                IconButton(onClick = { state.push(Screen.Settings) }) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            }

            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            ) {
                Text("DailyMe", style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = if (state.isLoggedIn) {
                        "Connected to ${state.owner}/${state.repo}"
                    } else {
                        "Log in to browse and edit a GitHub repository's markdown files."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )

                Button(
                    onClick = { state.push(Screen.Browser("")) },
                    enabled = state.isLoggedIn,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Open file browser")
                }

                Button(
                    onClick = { state.push(Screen.Journal) },
                    enabled = state.isLoggedIn,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Open journal")
                }

                if (!state.isLoggedIn) {
                    Button(
                        onClick = { state.push(Screen.Login) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Log in")
                    }
                }
            }
        }
    }
}
