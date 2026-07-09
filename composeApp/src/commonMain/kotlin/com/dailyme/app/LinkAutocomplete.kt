package com.dailyme.app

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/** An in-progress `#tag` or `[[wiki link]]` trigger at the cursor, ready for autocompletion. */
data class LinkTrigger(
    val kind: Kind,
    val query: String,
    val startIndex: Int,
    val endIndex: Int,
) {
    enum class Kind { TAG, WIKI_LINK }
}

/**
 * Scans backward from [caret] (never crossing a newline, since a trigger can't span lines —
 * matching Markdown.kt's own line-scoped inline parsing) for an active, unclosed `#tag` or
 * `[[wiki link]]` trigger ending exactly at the cursor. Returns null if there isn't one.
 */
fun findActiveTrigger(text: String, caret: Int): LinkTrigger? {
    if (caret <= 0 || caret > text.length) return null

    val lineStart = text.lastIndexOf('\n', caret - 1) + 1

    val bracketIdx = text.lastIndexOf("[[", caret - 1)
    if (bracketIdx != -1 && bracketIdx >= lineStart) {
        val between = text.substring(bracketIdx + 2, caret)
        if (!between.contains("]]") && !between.contains('\n')) {
            return LinkTrigger(LinkTrigger.Kind.WIKI_LINK, between, bracketIdx, caret)
        }
    }

    var i = caret
    while (i > lineStart && isWikiLinkChar(text[i - 1])) i--
    if (i < caret && i > lineStart && text[i - 1] == '#') {
        return LinkTrigger(LinkTrigger.Kind.TAG, text.substring(i, caret), i - 1, caret)
    }

    return null
}

/** Splices [name] into [text] at [trigger]'s span, closing `[[ ]]` if that was the trigger kind. */
fun applySuggestion(text: String, trigger: LinkTrigger, name: String): TextFieldValue {
    val prefix = text.substring(0, trigger.startIndex)
    val suffix = text.substring(trigger.endIndex)
    val insertion = when (trigger.kind) {
        LinkTrigger.Kind.TAG -> "#$name"
        LinkTrigger.Kind.WIKI_LINK -> "[[$name]]"
    }
    return TextFieldValue(prefix + insertion + suffix, TextRange(prefix.length + insertion.length))
}
