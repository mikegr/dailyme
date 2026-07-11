package com.dailyme.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Tag used on [AnnotatedString] spans for `#tag` and `[[wiki link]]` references. */
private const val WIKI_LINK_TAG = "wiki_link"

/** What counts as part of a `#tag` name; reused by the editor's link autocomplete. */
fun isWikiLinkChar(c: Char) = c.isLetterOrDigit() || c == '_' || c == '-'

/**
 * Every `#tag`/`[[wiki link]]` name referenced anywhere in [markdown], deduplicated. Mirrors
 * the link recognition rules in [renderInline] (`[[name]]`, trimmed, or `#name` where name is
 * a run of [isWikiLinkChar]), but as a plain scan with no Compose dependency, so it can also
 * be used off the UI thread to find backlinks.
 */
fun extractWikiLinkNames(markdown: String): Set<String> {
    val names = mutableSetOf<String>()
    var i = 0
    while (i < markdown.length) {
        when {
            markdown.startsWith("[[", i) -> {
                val close = markdown.indexOf("]]", i + 2)
                if (close == -1) {
                    i++
                } else {
                    names.add(markdown.substring(i + 2, close).trim())
                    i = close + 2
                }
            }

            markdown[i] == '#' && i + 1 < markdown.length && isWikiLinkChar(markdown[i + 1]) -> {
                var end = i + 1
                while (end < markdown.length && isWikiLinkChar(markdown[end])) end++
                names.add(markdown.substring(i + 1, end))
                i = end
            }

            else -> i++
        }
    }
    return names
}

private sealed class MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock()
    data class Paragraph(val text: String) : MdBlock()
    data class CodeBlock(val code: String) : MdBlock()
    data class ListItem(val text: String, val ordered: Boolean, val index: Int) : MdBlock()
    data class Quote(val text: String) : MdBlock()
    data object Divider : MdBlock()
}

private fun parseMarkdown(markdown: String): List<MdBlock> {
    val lines = markdown.replace("\r\n", "\n").split("\n")
    val blocks = mutableListOf<MdBlock>()
    var i = 0
    var paragraphBuffer = StringBuilder()
    var orderedIndex = 1

    fun flushParagraph() {
        if (paragraphBuffer.isNotBlank()) {
            blocks.add(MdBlock.Paragraph(paragraphBuffer.toString().trim()))
        }
        paragraphBuffer = StringBuilder()
    }

    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trim()
        val headingHashes = trimmed.takeWhile { it == '#' }
        // A heading is "#" (up to 6) followed by a space; "#tag" with no space is a wiki-link tag.
        val isHeading = headingHashes.isNotEmpty() && headingHashes.length <= 6 &&
            (headingHashes.length == trimmed.length || trimmed[headingHashes.length] == ' ')

        when {
            trimmed.startsWith("```") -> {
                flushParagraph()
                val code = StringBuilder()
                i++
                while (i < lines.size && !lines[i].trim().startsWith("```")) {
                    code.appendLine(lines[i])
                    i++
                }
                blocks.add(MdBlock.CodeBlock(code.toString().trimEnd('\n')))
            }

            isHeading -> {
                flushParagraph()
                blocks.add(MdBlock.Heading(headingHashes.length, trimmed.drop(headingHashes.length).trim()))
                orderedIndex = 1
            }

            trimmed == "---" || trimmed == "***" -> {
                flushParagraph()
                blocks.add(MdBlock.Divider)
            }

            trimmed.startsWith("> ") -> {
                flushParagraph()
                blocks.add(MdBlock.Quote(trimmed.removePrefix("> ")))
            }

            trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                flushParagraph()
                blocks.add(MdBlock.ListItem(trimmed.drop(2).trim(), ordered = false, index = 0))
            }

            Regex("^\\d+\\.\\s").containsMatchIn(trimmed) -> {
                flushParagraph()
                val text = trimmed.replaceFirst(Regex("^\\d+\\.\\s"), "")
                blocks.add(MdBlock.ListItem(text, ordered = true, index = orderedIndex))
                orderedIndex++
            }

            trimmed.isEmpty() -> {
                flushParagraph()
                orderedIndex = 1
            }

            else -> {
                if (paragraphBuffer.isNotEmpty()) paragraphBuffer.append(" ")
                paragraphBuffer.append(trimmed)
            }
        }
        i++
    }
    flushParagraph()
    return blocks
}

private fun renderInline(text: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        when {
            text.startsWith("**", i) -> {
                val end = text.indexOf("**", i + 2)
                if (end == -1) {
                    append(text.substring(i))
                    i = text.length
                } else {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(renderInline(text.substring(i + 2, end)))
                    }
                    i = end + 2
                }
            }

            text.startsWith("`", i) -> {
                val end = text.indexOf("`", i + 1)
                if (end == -1) {
                    append(text.substring(i))
                    i = text.length
                } else {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = Color(0x22808080),
                        )
                    ) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                }
            }

            text.startsWith("*", i) || text.startsWith("_", i) -> {
                val marker = text[i]
                val end = text.indexOf(marker, i + 1)
                if (end == -1) {
                    append(text.substring(i))
                    i = text.length
                } else {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(renderInline(text.substring(i + 1, end)))
                    }
                    i = end + 1
                }
            }

            text.startsWith("[[", i) -> {
                val close = text.indexOf("]]", i + 2)
                if (close == -1) {
                    append(text[i])
                    i++
                } else {
                    val name = text.substring(i + 2, close).trim()
                    pushStringAnnotation(WIKI_LINK_TAG, name)
                    withStyle(
                        SpanStyle(color = Color(0xFF3873E8), textDecoration = TextDecoration.Underline)
                    ) {
                        append(name)
                    }
                    pop()
                    i = close + 2
                }
            }

            text.startsWith("[", i) -> {
                val closeBracket = text.indexOf("]", i + 1)
                val openParen = if (closeBracket != -1) closeBracket + 1 else -1
                val closeParen = if (openParen != -1 && openParen < text.length && text[openParen] == '(') {
                    text.indexOf(")", openParen)
                } else -1
                if (closeBracket == -1 || closeParen == -1) {
                    append(text[i])
                    i++
                } else {
                    val label = text.substring(i + 1, closeBracket)
                    withStyle(
                        SpanStyle(
                            color = Color(0xFF3873E8),
                            fontWeight = FontWeight.Medium,
                        )
                    ) {
                        append(label)
                    }
                    i = closeParen + 1
                }
            }

            text[i] == '#' && i + 1 < text.length && isWikiLinkChar(text[i + 1]) -> {
                var end = i + 1
                while (end < text.length && isWikiLinkChar(text[end])) end++
                val name = text.substring(i + 1, end)
                pushStringAnnotation(WIKI_LINK_TAG, name)
                withStyle(
                    SpanStyle(color = Color(0xFF3873E8), textDecoration = TextDecoration.Underline)
                ) {
                    append("#$name")
                }
                pop()
                i = end
            }

            else -> {
                append(text[i])
                i++
            }
        }
    }
}

/**
 * [Text] replacement that dispatches taps on `#tag`/`[[wiki link]]` spans to [onLinkClick], and
 * any other tap within the text to [onContentClick] (so callers can still treat the text as one
 * big "open"/"edit" target without it being swallowed by the link's own tap handling).
 */
@Composable
private fun MarkdownInlineText(
    text: AnnotatedString,
    style: TextStyle,
    onLinkClick: (String) -> Unit,
    onContentClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ClickableText(
        text = text,
        style = style,
        modifier = modifier,
        onClick = { offset ->
            val link = text.getStringAnnotations(WIKI_LINK_TAG, offset, offset).firstOrNull()
            if (link != null) onLinkClick(link.item) else onContentClick()
        },
    )
}

@Composable
fun MarkdownView(
    markdown: String,
    modifier: Modifier = Modifier,
    onLinkClick: (String) -> Unit = {},
    onContentClick: () -> Unit = {},
) {
    val blocks = remember(markdown) { parseMarkdown(markdown) }
    val baseColor = LocalContentColor.current
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (block in blocks) {
            when (block) {
                is MdBlock.Heading -> {
                    val size = when (block.level) {
                        1 -> 28.sp
                        2 -> 24.sp
                        3 -> 20.sp
                        else -> 18.sp
                    }
                    MarkdownInlineText(
                        text = renderInline(block.text),
                        style = TextStyle(fontSize = size, fontWeight = FontWeight.Bold, color = baseColor),
                        onLinkClick = onLinkClick,
                        onContentClick = onContentClick,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                    )
                }

                is MdBlock.Paragraph -> MarkdownInlineText(
                    text = renderInline(block.text),
                    style = TextStyle(fontSize = 16.sp, color = baseColor),
                    onLinkClick = onLinkClick,
                    onContentClick = onContentClick,
                )

                is MdBlock.CodeBlock -> Text(
                    text = block.code,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x14000000))
                        .padding(8.dp),
                )

                is MdBlock.ListItem -> Row(modifier = Modifier.padding(start = 8.dp)) {
                    Text(
                        text = if (block.ordered) "${block.index}." else "•",
                        modifier = Modifier.width(24.dp),
                        fontSize = 16.sp,
                    )
                    MarkdownInlineText(
                        text = renderInline(block.text),
                        style = TextStyle(fontSize = 16.sp, color = baseColor),
                        onLinkClick = onLinkClick,
                        onContentClick = onContentClick,
                    )
                }

                is MdBlock.Quote -> Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x14000000))
                        .padding(8.dp),
                ) {
                    MarkdownInlineText(
                        text = renderInline(block.text),
                        style = TextStyle(
                            fontStyle = FontStyle.Italic,
                            color = baseColor.copy(alpha = 0.8f),
                        ),
                        onLinkClick = onLinkClick,
                        onContentClick = onContentClick,
                    )
                }

                MdBlock.Divider -> Text(text = "─".repeat(40), color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}
