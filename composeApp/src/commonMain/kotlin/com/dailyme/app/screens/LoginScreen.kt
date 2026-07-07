package com.dailyme.app.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.dailyme.app.AppState
import com.dailyme.app.CredentialsStore
import com.dailyme.app.StoredCredentials
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(state: AppState, credentialsStore: CredentialsStore, onConnected: () -> Unit) {
    var token by remember { mutableStateOf("") }
    var owner by remember { mutableStateOf(state.owner) }
    var repo by remember { mutableStateOf(state.repo) }
    var branch by remember { mutableStateOf(state.branch) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("DailyMe", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Connect to a GitHub repository to browse and edit its markdown files.",
            style = MaterialTheme.typography.bodyMedium,
        )

        OutlinedTextField(
            value = owner,
            onValueChange = { owner = it },
            label = { Text("Repository owner") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = repo,
            onValueChange = { repo = it },
            label = { Text("Repository name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = branch,
            onValueChange = { branch = it },
            label = { Text("Branch") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = token,
            onValueChange = { token = it },
            label = { Text("Personal access token") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )

        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Button(
            onClick = {
                if (owner.isBlank() || repo.isBlank() || token.isBlank()) {
                    error = "Owner, repository, and token are required."
                    return@Button
                }
                error = null
                val trimmedOwner = owner.trim()
                val trimmedRepo = repo.trim()
                val trimmedBranch = branch.ifBlank { "main" }
                state.owner = trimmedOwner
                state.repo = trimmedRepo
                state.branch = trimmedBranch
                scope.launch {
                    credentialsStore.save(
                        StoredCredentials(
                            token = token,
                            owner = trimmedOwner,
                            repo = trimmedRepo,
                            branch = trimmedBranch,
                        )
                    )
                    onConnected()
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Connect")
        }

        Text(
            "Your token is stored locally on this device only and is never sent anywhere except to api.github.com.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
