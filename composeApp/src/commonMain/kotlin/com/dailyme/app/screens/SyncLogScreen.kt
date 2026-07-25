@file:OptIn(ExperimentalMaterial3Api::class)

package com.dailyme.app.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dailyme.app.AppState
import com.dailyme.app.CallLogEntry
import com.dailyme.app.CallOutcome
import com.dailyme.app.CommitInfo
import com.dailyme.app.RepositoryClient
import com.dailyme.app.theme.cyanicTopAppBarColors
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalTime::class)
private fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()

private fun formatRelative(nowMillis: Long, targetMillis: Long): String {
    val diffSeconds = (targetMillis - nowMillis) / 1000
    val isFuture = diffSeconds > 0
    val absSeconds = kotlin.math.abs(diffSeconds)
    return when {
        absSeconds < 5 -> "now"
        absSeconds < 60 -> if (isFuture) "in ${absSeconds}s" else "${absSeconds}s ago"
        absSeconds < 3600 -> if (isFuture) "in ${absSeconds / 60}m" else "${absSeconds / 60}m ago"
        else -> if (isFuture) "in ${absSeconds / 3600}h" else "${absSeconds / 3600}h ago"
    }
}

@Composable
fun SyncLogScreen(state: AppState, repositoryClient: RepositoryClient) {
    val unpushed by repositoryClient.unpushedCommits.collectAsState()
    val log by repositoryClient.callLogEntries.collectAsState()
    val scope = rememberCoroutineScope()
    var now by remember { mutableLongStateOf(nowMillis()) }
    var showDiscardConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            now = nowMillis()
        }
    }

    if (showDiscardConfirm) {
        ConfirmDialog(
            title = "Discard all local changes?",
            text = "This resets to match the last-pushed version, discarding every commit still waiting to sync.",
            confirmLabel = "Discard",
            onConfirm = {
                showDiscardConfirm = false
                scope.launch { repositoryClient.discardAllUnpushedChanges() }
            },
            onDismiss = { showDiscardConfirm = false },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sync activity") },
                navigationIcon = {
                    IconButton(onClick = { state.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = cyanicTopAppBarColors(),
            )
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Pending", style = MaterialTheme.typography.titleMedium)
                    if (unpushed.isNotEmpty()) {
                        TextButton(onClick = { showDiscardConfirm = true }) {
                            Text("Discard all")
                        }
                    }
                }
            }

            if (unpushed.isEmpty()) {
                item {
                    Text(
                        "Nothing waiting to sync.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            } else {
                items(unpushed, key = { it.sha }) { commit ->
                    UnpushedCommitRow(commit, now)
                    HorizontalDivider()
                }
            }

            item {
                Text(
                    "Recent activity",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp),
                )
            }

            if (log.isEmpty()) {
                item {
                    Text(
                        "No calls yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            } else {
                items(log, key = { "${it.timestampMillis}/${it.path}" }) { entry ->
                    CallLogRow(entry, now)
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun UnpushedCommitRow(commit: CommitInfo, now: Long) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text(commit.message, style = MaterialTheme.typography.titleMedium)
        Text(
            "Committed ${formatRelative(now, commit.timestampMillis)} — not yet pushed",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CallLogRow(entry: CallLogEntry, now: Long) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.path, style = MaterialTheme.typography.titleMedium)
            Text(entry.message, style = MaterialTheme.typography.bodySmall)
        }
        Column {
            Text(
                if (entry.outcome == CallOutcome.SUCCESS) "Success" else "Failed",
                style = MaterialTheme.typography.bodySmall,
                color = if (entry.outcome == CallOutcome.SUCCESS) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
            Text(
                formatRelative(now, entry.timestampMillis),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
