@file:OptIn(ExperimentalMaterial3Api::class)

package com.dailyme.app.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dailyme.app.AppState
import com.dailyme.app.PendingChange
import com.dailyme.app.RepositoryClient
import kotlinx.coroutines.launch

@Composable
fun PendingChangesScreen(state: AppState, repositoryClient: RepositoryClient) {
    var pending by remember { mutableStateOf<List<PendingChange>>(emptyList()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        pending = repositoryClient.listPendingChanges()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pending changes") },
                navigationIcon = {
                    IconButton(onClick = { state.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (pending.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No pending changes. Everything is synced.")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(pending, key = { "${it.owner}/${it.repo}/${it.branch}/${it.path}" }) { change ->
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("${change.owner}/${change.repo} — ${change.path}", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Not yet synced. Will retry automatically once online.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(change.commitMessage, style = MaterialTheme.typography.bodySmall)
                        TextButton(
                            onClick = {
                                scope.launch {
                                    repositoryClient.discardPendingChange(change)
                                    pending = repositoryClient.listPendingChanges()
                                }
                            },
                        ) {
                            Text("Discard change")
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}
