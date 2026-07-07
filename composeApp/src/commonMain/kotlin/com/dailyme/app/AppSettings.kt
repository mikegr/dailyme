package com.dailyme.app

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

const val DEFAULT_COMMIT_MESSAGE_TEMPLATE = "Update {file} via DailyMe"

private const val COMMIT_MESSAGE_TEMPLATE_KEY = "commit_message_template"

class AppSettings(private val store: KeyValueStore) {
    private val _commitMessageTemplate = MutableStateFlow(DEFAULT_COMMIT_MESSAGE_TEMPLATE)
    val commitMessageTemplate: StateFlow<String> = _commitMessageTemplate
    private var loaded = false

    suspend fun ensureLoaded() {
        if (loaded) return
        _commitMessageTemplate.value = store.get(COMMIT_MESSAGE_TEMPLATE_KEY)?.takeIf { it.isNotBlank() }
            ?: DEFAULT_COMMIT_MESSAGE_TEMPLATE
        loaded = true
    }

    suspend fun setCommitMessageTemplate(template: String) {
        val value = template.ifBlank { DEFAULT_COMMIT_MESSAGE_TEMPLATE }
        _commitMessageTemplate.value = value
        store.put(COMMIT_MESSAGE_TEMPLATE_KEY, value)
    }

    fun buildCommitMessage(fileName: String): String {
        val template = _commitMessageTemplate.value
        return if (template.contains("{file}")) template.replace("{file}", fileName) else template
    }
}
