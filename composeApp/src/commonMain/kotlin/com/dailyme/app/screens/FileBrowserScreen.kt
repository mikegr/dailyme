@file:OptIn(ExperimentalMaterial3Api::class)

package com.dailyme.app.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dailyme.app.AppState
import com.dailyme.app.GitHubApi
import com.dailyme.app.GitHubApiException
import com.dailyme.app.GitHubContentItem
import com.dailyme.app.Screen

@Composable
fun FileBrowserScreen(state: AppState, api: GitHubApi, path: String) {
    var items by remember(path) { mutableStateOf<List<GitHubContentItem>?>(null) }
    var error by remember(path) { mutableStateOf<String?>(null) }

    LaunchedEffect(path, state.owner, state.repo, state.branch) {
        error = null
        items = null
        try {
            val result = api.listContents(state.owner, state.repo, path, state.branch)
                .sortedWith(compareBy({ it.type != "dir" }, { it.name.lowercase() }))
            items = result
        } catch (e: GitHubApiException) {
            error = e.message
        } catch (e: Exception) {
            error = e.message ?: "Failed to load repository contents."
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "${state.owner}/${state.repo}" + if (path.isNotEmpty()) " / $path" else "",
                        maxLines = 1,
                    )
                },
                navigationIcon = {
                    if (state.backStack.size > 1) {
                        IconButton(onClick = { state.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { state.reset() }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Disconnect")
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

                items == null -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                items!!.isEmpty() -> Text("This folder is empty.", modifier = Modifier.padding(16.dp))

                else -> LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(items!!, key = { it.path }) { item ->
                        val isMarkdown = item.type == "file" && item.name.endsWith(".md", ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = item.type == "dir" || isMarkdown) {
                                    if (item.type == "dir") {
                                        state.push(Screen.Browser(item.path))
                                    } else if (isMarkdown) {
                                        state.push(Screen.FileView(item.path))
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                imageVector = if (item.type == "dir") Icons.Default.Folder else Icons.Default.Description,
                                contentDescription = null,
                                tint = if (item.type == "dir" || isMarkdown) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                },
                            )
                            Text(item.name)
                        }
                    }
                }
            }
        }
    }
}
