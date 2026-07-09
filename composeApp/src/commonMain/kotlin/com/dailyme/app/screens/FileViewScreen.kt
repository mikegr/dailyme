@file:OptIn(ExperimentalMaterial3Api::class)

package com.dailyme.app.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.dailyme.app.AppSettings
import com.dailyme.app.AppState
import com.dailyme.app.GitHubApiException
import com.dailyme.app.LinkTrigger
import com.dailyme.app.MarkdownView
import com.dailyme.app.RepositoryClient
import com.dailyme.app.SaveOutcome
import com.dailyme.app.Screen
import com.dailyme.app.applySuggestion
import com.dailyme.app.findActiveTrigger
import com.dailyme.app.theme.cyanicTopAppBarColors
import kotlinx.coroutines.launch

private data class LoadedFile(
    val sha: String?,
    val content: String,
    val isFromCache: Boolean,
    val hasPendingChange: Boolean,
)

@Composable
fun FileViewScreen(
    state: AppState,
    repositoryClient: RepositoryClient,
    appSettings: AppSettings,
    path: String,
    startInEditMode: Boolean = false,
) {
    var loaded by remember(path) { mutableStateOf<LoadedFile?>(null) }
    var error by remember(path) { mutableStateOf<String?>(null) }
    var isEditing by remember(path) { mutableStateOf(startInEditMode) }
    var editedText by remember(path) { mutableStateOf(TextFieldValue("")) }
    var isSaving by remember(path) { mutableStateOf(false) }
    var showDiscardConfirm by remember(path) { mutableStateOf(false) }
    var pageNames by remember(path) { mutableStateOf<List<String>>(emptyList()) }
    var activeTrigger by remember(path) { mutableStateOf<LinkTrigger?>(null) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(path, state.owner, state.repo, state.branch) {
        error = null
        loaded = null
        try {
            val result = repositoryClient.getFile(state.owner, state.repo, state.branch, path)
            loaded = LoadedFile(result.sha, result.content, result.isFromCache, result.hasPendingChange)
            editedText = TextFieldValue(result.content)
        } catch (e: GitHubApiException) {
            error = e.message
        } catch (e: Exception) {
            error = e.message ?: "Failed to load file."
        }
    }

    LaunchedEffect(state.owner, state.repo, state.branch) {
        pageNames = try {
            repositoryClient.listPageNames(state.owner, state.repo, state.branch)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun performSave() {
        val current = loaded ?: return
        val message = appSettings.buildCommitMessage(path.substringAfterLast('/'))
        isSaving = true
        scope.launch {
            try {
                val outcome = repositoryClient.saveFile(
                    owner = state.owner,
                    repo = state.repo,
                    branch = state.branch,
                    path = path,
                    newContent = editedText.text,
                    baseSha = current.sha,
                    commitMessage = message,
                )
                when (outcome) {
                    is SaveOutcome.Saved -> {
                        loaded = LoadedFile(outcome.sha, editedText.text, isFromCache = false, hasPendingChange = false)
                        snackbarHostState.showSnackbar("Saved to GitHub")
                    }

                    SaveOutcome.Queued -> {
                        loaded = LoadedFile(current.sha, editedText.text, isFromCache = false, hasPendingChange = true)
                        snackbarHostState.showSnackbar("Offline — change queued, will sync when back online")
                    }
                }
                isEditing = false
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Save failed: ${e.message}")
            } finally {
                isSaving = false
            }
        }
    }

    fun discardEdits() {
        editedText = TextFieldValue(loaded?.content ?: editedText.text)
        isEditing = false
    }

    if (showDiscardConfirm) {
        ConfirmDialog(
            title = "Discard changes?",
            text = "Your edits haven't been saved. Switching to preview will throw them away.",
            confirmLabel = "Discard",
            onConfirm = {
                showDiscardConfirm = false
                discardEdits()
            },
            onDismiss = { showDiscardConfirm = false },
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
                            IconButton(onClick = {
                                if (editedText.text != loaded?.content) {
                                    showDiscardConfirm = true
                                } else {
                                    discardEdits()
                                }
                            }) {
                                Icon(Icons.Default.Visibility, contentDescription = "Preview")
                            }
                            IconButton(onClick = { performSave() }) {
                                Icon(Icons.Default.Save, contentDescription = "Save")
                            }
                        } else {
                            IconButton(onClick = { isEditing = true; activeTrigger = null }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }
                        }
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

                loaded == null || isSaving -> Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CircularProgressIndicator()
                    if (isSaving) Text("Saving...")
                }

                isEditing -> {
                    val suggestions = remember(activeTrigger, pageNames) {
                        val trigger = activeTrigger
                        if (trigger == null) {
                            emptyList()
                        } else {
                            pageNames.filter { it.contains(trigger.query, ignoreCase = true) }
                        }
                    }
                    Box(modifier = Modifier.fillMaxSize().padding(8.dp).imePadding()) {
                        OutlinedTextField(
                            value = editedText,
                            onValueChange = { new ->
                                editedText = new
                                activeTrigger = if (new.selection.collapsed) {
                                    findActiveTrigger(new.text, new.selection.end)
                                } else {
                                    null
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                        )
                        if (activeTrigger != null && suggestions.isNotEmpty()) {
                            LinkSuggestionPopup(
                                suggestions = suggestions,
                                onSelect = { name ->
                                    activeTrigger?.let { trigger ->
                                        editedText = applySuggestion(editedText.text, trigger, name)
                                        activeTrigger = null
                                    }
                                },
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth(),
                            )
                        }
                    }
                }

                else -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(onClick = { isEditing = true; activeTrigger = null }),
                ) {
                    val statusText = when {
                        loaded?.hasPendingChange == true -> "Not yet synced to GitHub"
                        loaded?.isFromCache == true -> "Showing cached copy"
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
                        markdown = editedText.text,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        onLinkClick = { name ->
                            scope.launch {
                                val resolved = repositoryClient.resolveWikiLink(
                                    state.owner,
                                    state.repo,
                                    state.branch,
                                    name,
                                )
                                if (resolved != null) state.push(Screen.FileView(resolved))
                            }
                        },
                        onContentClick = { isEditing = true; activeTrigger = null },
                    )
                }
            }
        }
    }
}
