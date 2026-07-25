package com.dailyme.app

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.dailyme.app.screens.FileBrowserScreen
import com.dailyme.app.screens.FileViewScreen
import com.dailyme.app.screens.JournalScreen
import com.dailyme.app.screens.LoginScreen
import com.dailyme.app.screens.SettingsScreen
import com.dailyme.app.screens.StartScreen
import com.dailyme.app.screens.SyncLogScreen
import com.dailyme.app.theme.DailyMeTheme

@Composable
fun App() {
    val credentialsStore = remember { createCredentialsStore() }
    val keyValueStore = remember { createKeyValueStore() }
    val localGit = remember { createLocalGitRepository { credentialsStore.load()?.token } }
    val callLog = remember { CallLog(keyValueStore) }
    val repositoryClient = remember { RepositoryClient(localGit, callLog) }
    val appSettings = remember { AppSettings(keyValueStore) }
    val networkMonitor = remember { createNetworkMonitor() }
    val state = remember { AppState() }
    var isRestoringSession by remember { mutableStateOf(true) }

    val isOnline by networkMonitor.isOnline.collectAsState()
    val unpushedCommits by repositoryClient.unpushedCommits.collectAsState()
    val themeMode by appSettings.themeMode.collectAsState()

    DailyMeTheme(
        darkTheme = when (themeMode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
        },
    ) {
        LaunchedEffect(Unit) {
            val stored = credentialsStore.load()
            if (stored != null) {
                state.owner = stored.owner
                state.repo = stored.repo
                state.branch = stored.branch
                state.isLoggedIn = true
            }
            appSettings.ensureLoaded()
            if (stored != null) {
                try {
                    repositoryClient.ensureRepositoryReady(stored.owner, stored.repo, stored.branch)
                } catch (e: Exception) {
                    AppLog.e("Failed to prepare local repository at startup", e)
                }
            }
            isRestoringSession = false
            repositoryClient.retryPush()
        }

        LaunchedEffect(isOnline) {
            if (isOnline) repositoryClient.retryPush()
        }

        Surface(modifier = Modifier.fillMaxSize()) {
            when {
                isRestoringSession -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                else -> Column(modifier = Modifier.fillMaxSize()) {
                    if (!isOnline) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.errorContainer)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            Text(
                                "You're offline — showing cached content. Edits will sync once you're back online.",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    } else if (unpushedCommits.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            Text(
                                "Syncing ${unpushedCommits.size} commit(s)…",
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }

                    NavDisplay(
                        modifier = Modifier.weight(1f).fillMaxSize(),
                        backStack = state.backStack,
                        onBack = { state.pop() },
                        entryProvider = { screen ->
                            when (screen) {
                                is Screen.Start -> NavEntry(screen) {
                                    StartScreen(state, credentialsStore, repositoryClient)
                                }

                                is Screen.Login -> NavEntry(screen) {
                                    LoginScreen(state, credentialsStore, repositoryClient) {
                                        state.isLoggedIn = true
                                        state.pop()
                                    }
                                }

                                is Screen.SyncLog -> NavEntry(screen) {
                                    SyncLogScreen(state, repositoryClient)
                                }

                                is Screen.Journal -> NavEntry(screen) {
                                    JournalScreen(state, repositoryClient, appSettings)
                                }

                                is Screen.Settings -> NavEntry(screen) {
                                    SettingsScreen(state, appSettings)
                                }

                                is Screen.Browser -> NavEntry(screen) {
                                    FileBrowserScreen(state, repositoryClient, credentialsStore, screen.path)
                                }

                                is Screen.FileView -> NavEntry(screen) {
                                    FileViewScreen(state, repositoryClient, appSettings, screen.path, screen.startInEditMode)
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}
