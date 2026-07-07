@file:OptIn(ExperimentalMaterial3Api::class)

package com.dailyme.app.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.dailyme.app.AppState
import com.dailyme.app.GitHubApiException
import com.dailyme.app.MarkdownView
import com.dailyme.app.RepositoryClient
import com.dailyme.app.SaveOutcome
import kotlinx.coroutines.launch

private data class LoadedFile(
    val sha: String,
    val content: String,
    val isFromCache: Boolean,
    val hasPendingChange: Boolean,
)

@Composable
fun FileViewScreen(state: AppState, repositoryClient: RepositoryClient, path: String) {
    var loaded by remember(path) { mutableStateOf<LoadedFile?>(null) }
    var error by remember(path) { mutableStateOf<String?>(null) }
    var isEditing by remember(path) { mutableStateOf(false) }
    var editedText by remember(path) { mutableStateOf("") }
    var isSaving by remember(path) { mutableStateOf(false) }
    var showCommitDialog by remember(path) { mutableStateOf(false) }
    var commitMessage by remember(path) { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(path, state.owner, state.repo, state.branch) {
        error = null
        loaded = null
        try {
            val result = repositoryClient.getFile(state.owner, state.repo, state.branch, path)
            loaded = LoadedFile(result.sha, result.content, result.isFromCache, result.hasPendingChange)
            editedText = result.content
        } catch (e: GitHubApiException) {
            error = e.message
        } catch (e: Exception) {
            error = e.message ?: "Failed to load file."
        }
    }

    fun performSave() {
        val current = loaded ?: return
        val message = commitMessage.ifBlank { "Update ${path.substringAfterLast('/')} via DailyMe" }
        isSaving = true
        showCommitDialog = false
        scope.launch {
            try {
                val outcome = repositoryClient.saveFile(
                    owner = state.owner,
                    repo = state.repo,
                    branch = state.branch,
                    path = path,
                    newContent = editedText,
                    baseSha = current.sha,
                    commitMessage = message,
                )
                when (outcome) {
                    is SaveOutcome.Saved -> {
                        loaded = LoadedFile(outcome.sha, editedText, isFromCache = false, hasPendingChange = false)
                        snackbarHostState.showSnackbar("Saved to GitHub")
                    }

                    SaveOutcome.Queued -> {
                        loaded = LoadedFile(current.sha, editedText, isFromCache = false, hasPendingChange = true)
                        snackbarHostState.showSnackbar("Offline — change queued, will sync when back online")
                    }
                }
                isEditing = false
                commitMessage = ""
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Save failed: ${e.message}")
            } finally {
                isSaving = false
            }
        }
    }

    if (showCommitDialog) {
        AlertDialog(
            onDismissRequest = { showCommitDialog = false },
            title = { Text("Commit changes") },
            text = {
                OutlinedTextField(
                    value = commitMessage,
                    onValueChange = { commitMessage = it },
                    label = { Text("Commit message") },
                    placeholder = { Text("Update ${path.substringAfterLast('/')} via DailyMe") },
                    singleLine = false,
                    keyboardOptions = KeyboardOptions.Default,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = { performSave() }) { Text("Commit") }
            },
            dismissButton = {
                TextButton(onClick = { showCommitDialog = false }) { Text("Cancel") }
            },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(path.substringAfterLast('/'), maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = { state.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (loaded != null && !isSaving) {
                        if (isEditing) {
                            IconButton(onClick = { showCommitDialog = true }) {
                                Icon(Icons.Default.Save, contentDescription = "Save")
                            }
                            IconButton(onClick = {
                                editedText = loaded?.content ?: editedText
                                isEditing = false
                            }) {
                                Icon(Icons.Default.Visibility, contentDescription = "Preview")
                            }
                        } else {
                            IconButton(onClick = { isEditing = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }
                        }
                    }
                },
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

                loaded == null || isSaving -> Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CircularProgressIndicator()
                    if (isSaving) Text("Saving...")
                }

                isEditing -> OutlinedTextField(
                    value = editedText,
                    onValueChange = { editedText = it },
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                )

                else -> Column(modifier = Modifier.fillMaxSize()) {
                    val statusText = when {
                        loaded?.hasPendingChange == true -> "Not yet synced to GitHub"
                        loaded?.isFromCache == true -> "Showing cached copy (offline)"
                        else -> null
                    }
                    if (statusText != null) {
                        Text(
                            statusText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    MarkdownView(
                        markdown = editedText,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                    )
                }
            }
        }
    }
}
