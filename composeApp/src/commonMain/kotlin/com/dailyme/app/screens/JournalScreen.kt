@file:OptIn(ExperimentalMaterial3Api::class)

package com.dailyme.app.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dailyme.app.AppLog
import com.dailyme.app.AppSettings
import com.dailyme.app.AppState
import com.dailyme.app.GitHubContentItem
import com.dailyme.app.MarkdownView
import com.dailyme.app.RepositoryClient
import com.dailyme.app.Screen
import com.dailyme.app.theme.cyanicTopAppBarColors
import com.dailyme.app.todayJournalFileName
import kotlinx.coroutines.launch

private const val JOURNALS_PATH = "journals"

@Composable
fun JournalScreen(state: AppState, repositoryClient: RepositoryClient, appSettings: AppSettings) {
    var entries by remember { mutableStateOf<List<GitHubContentItem>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var isCreating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.owner, state.repo, state.branch) {
        error = null
        entries = null
        try {
            val result = repositoryClient.listContents(state.owner, state.repo, state.branch, JOURNALS_PATH)
            entries = result.items
                .filter { it.type == "file" && it.name.endsWith(".md", ignoreCase = true) }
                .sortedByDescending { it.name }
        } catch (e: Exception) {
            error = e.message ?: "Failed to load journal entries."
            AppLog.e("Failed to load journal entries", e)
        }
    }

    fun openToday() {
        val fileName = "${todayJournalFileName()}.md"
        val existingPath = entries?.find { it.name.equals(fileName, ignoreCase = true) }?.path
        if (existingPath != null) {
            state.push(Screen.FileView(existingPath, startInEditMode = true))
            return
        }
        isCreating = true
        scope.launch {
            try {
                val path = "$JOURNALS_PATH/$fileName"
                repositoryClient.saveFile(
                    owner = state.owner,
                    repo = state.repo,
                    branch = state.branch,
                    path = path,
                    newContent = "",
                    commitMessage = appSettings.buildCommitMessage(fileName),
                )
                state.push(Screen.FileView(path, startInEditMode = true))
            } catch (e: Exception) {
                AppLog.e("Failed to create today's journal entry", e)
            } finally {
                isCreating = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Journal") },
                navigationIcon = {
                    IconButton(onClick = { state.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = ::openToday, enabled = !isCreating) {
                        Icon(Icons.Default.Add, contentDescription = "New journal entry")
                    }
                },
                colors = cyanicTopAppBarColors(),
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                error != null -> Text(
                    text = "Error: $error",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp),
                )

                entries == null -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                entries!!.isEmpty() -> Text(
                    "No journal entries found in \"$JOURNALS_PATH\".",
                    modifier = Modifier.padding(16.dp),
                )

                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(entries!!, key = { it.path }) { entry ->
                        JournalEntryItem(
                            state = state,
                            repositoryClient = repositoryClient,
                            appSettings = appSettings,
                            entry = entry,
                            onClick = { state.push(Screen.FileView(entry.path)) },
                            onEditClick = { state.push(Screen.FileView(entry.path, startInEditMode = true)) },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun JournalEntryItem(
    state: AppState,
    repositoryClient: RepositoryClient,
    appSettings: AppSettings,
    entry: GitHubContentItem,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
) {
    var content by remember(entry.path) { mutableStateOf<String?>(null) }
    var error by remember(entry.path) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(entry.path, state.owner, state.repo, state.branch) {
        error = null
        try {
            val result = repositoryClient.getFile(state.owner, state.repo, state.branch, entry.path)
            content = result.content
        } catch (e: Exception) {
            error = e.message ?: "Failed to load entry."
            AppLog.e("Failed to load journal entry \"${entry.path}\"", e)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = entry.name.removeSuffix(".md").removeSuffix(".MD"),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onEditClick) {
                Icon(Icons.Default.Edit, contentDescription = "Edit ${entry.name}")
            }
        }
        when {
            error != null -> Text(
                "Error: $error",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp),
            )

            content == null -> CircularProgressIndicator(modifier = Modifier.padding(top = 8.dp))

            else -> MarkdownView(
                markdown = content!!,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                onLinkClick = { name ->
                    scope.launch {
                        val resolution = repositoryClient.resolveOrCreateWikiLink(
                            state.owner,
                            state.repo,
                            state.branch,
                            name,
                            appSettings.buildCommitMessage("$name.md"),
                        )
                        state.push(Screen.FileView(resolution.path, startInEditMode = resolution.created))
                    }
                },
                onContentClick = onClick,
            )
        }
    }
}
