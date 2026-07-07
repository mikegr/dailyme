package com.dailyme.app

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

const val DEFAULT_COMMIT_MESSAGE_TEMPLATE = "Update {file} via DailyMe"

private const val COMMIT_MESSAGE_TEMPLATE_KEY = "commit_message_template"
private const val THEME_MODE_KEY = "theme_mode"

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

class AppSettings(private val store: KeyValueStore) {
    private val _commitMessageTemplate = MutableStateFlow(DEFAULT_COMMIT_MESSAGE_TEMPLATE)
    val commitMessageTemplate: StateFlow<String> = _commitMessageTemplate
    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode
    private var loaded = false

    suspend fun ensureLoaded() {
        if (loaded) return
        _commitMessageTemplate.value = store.get(COMMIT_MESSAGE_TEMPLATE_KEY)?.takeIf { it.isNotBlank() }
            ?: DEFAULT_COMMIT_MESSAGE_TEMPLATE
        _themeMode.value = store.get(THEME_MODE_KEY)?.let { saved ->
            ThemeMode.entries.find { it.name == saved }
        } ?: ThemeMode.SYSTEM
        loaded = true
    }

    suspend fun setCommitMessageTemplate(template: String) {
        val value = template.ifBlank { DEFAULT_COMMIT_MESSAGE_TEMPLATE }
        _commitMessageTemplate.value = value
        store.put(COMMIT_MESSAGE_TEMPLATE_KEY, value)
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        store.put(THEME_MODE_KEY, mode.name)
    }

    fun buildCommitMessage(fileName: String): String {
        val template = _commitMessageTemplate.value
        return if (template.contains("{file}")) template.replace("{file}", fileName) else template
    }
}
