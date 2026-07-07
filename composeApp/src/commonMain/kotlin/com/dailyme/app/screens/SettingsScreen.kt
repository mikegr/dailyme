@file:OptIn(ExperimentalMaterial3Api::class)

package com.dailyme.app.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dailyme.app.AppSettings
import com.dailyme.app.AppState
import com.dailyme.app.DEFAULT_COMMIT_MESSAGE_TEMPLATE
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(state: AppState, appSettings: AppSettings) {
    val currentTemplate by appSettings.commitMessageTemplate.collectAsState()
    var template by remember(currentTemplate) { mutableStateOf(currentTemplate) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { state.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Auto-commit message", style = MaterialTheme.typography.titleMedium)
            Text(
                "Used automatically whenever you save a file. Use {file} as a placeholder for the file name.",
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedTextField(
                value = template,
                onValueChange = { template = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
            )
            Button(
                onClick = {
                    scope.launch {
                        appSettings.setCommitMessageTemplate(template)
                        snackbarHostState.showSnackbar("Saved")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save")
            }
            OutlinedButton(
                onClick = { template = DEFAULT_COMMIT_MESSAGE_TEMPLATE },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Reset to default")
            }
        }
    }
}
