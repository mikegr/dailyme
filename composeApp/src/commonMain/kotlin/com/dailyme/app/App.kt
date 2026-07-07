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
import com.dailyme.app.screens.FileBrowserScreen
import com.dailyme.app.screens.FileViewScreen
import com.dailyme.app.screens.JournalScreen
import com.dailyme.app.screens.LoginScreen
import com.dailyme.app.screens.PendingChangesScreen
import com.dailyme.app.screens.SettingsScreen
import com.dailyme.app.screens.StartScreen
import com.dailyme.app.theme.DailyMeTheme

@Composable
fun App() {
    val credentialsStore = remember { createCredentialsStore() }
    val client = remember { createHttpClient() }
    val api = remember { GitHubApi(client) { credentialsStore.load()?.token } }
    val keyValueStore = remember { createKeyValueStore() }
    val offlineCache = remember { OfflineCache(keyValueStore) }
    val pendingChangeQueue = remember { PendingChangeQueue(keyValueStore) }
    val repositoryClient = remember { RepositoryClient(api, offlineCache, pendingChangeQueue) }
    val appSettings = remember { AppSettings(keyValueStore) }
    val networkMonitor = remember { createNetworkMonitor() }
    val state = remember { AppState() }
    var isRestoringSession by remember { mutableStateOf(true) }

    val isOnline by networkMonitor.isOnline.collectAsState()
    val pendingList by repositoryClient.pendingChangeList.collectAsState()
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
            repositoryClient.refreshPendingChanges()
            appSettings.ensureLoaded()
            isRestoringSession = false
        }

        LaunchedEffect(isOnline) {
            if (isOnline) repositoryClient.syncPendingChanges()
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
                    } else if (pendingList.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            Text(
                                "Syncing ${pendingList.size} pending change(s)…",
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                        when (val screen = state.current) {
                            is Screen.Start -> StartScreen(state, credentialsStore, repositoryClient)

                            is Screen.Login -> LoginScreen(state, credentialsStore) {
                                state.isLoggedIn = true
                                state.pop()
                            }

                            is Screen.PendingChanges -> PendingChangesScreen(state, repositoryClient)

                            is Screen.Journal -> JournalScreen(state, repositoryClient)

                            is Screen.Settings -> SettingsScreen(state, appSettings)

                            is Screen.Browser -> FileBrowserScreen(state, repositoryClient, credentialsStore, screen.path)

                            is Screen.FileView -> FileViewScreen(state, repositoryClient, appSettings, screen.path, screen.startInEditMode)
                        }
                    }
                }
            }
        }
    }
}
