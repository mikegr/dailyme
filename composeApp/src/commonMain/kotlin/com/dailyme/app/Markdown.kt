package com.dailyme.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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

            trimmed.startsWith("#") -> {
                flushParagraph()
                val level = trimmed.takeWhile { it == '#' }.length.coerceIn(1, 6)
                blocks.add(MdBlock.Heading(level, trimmed.dropWhile { it == '#' }.trim()))
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

            else -> {
                append(text[i])
                i++
            }
        }
    }
}

@Composable
fun MarkdownView(markdown: String, modifier: Modifier = Modifier) {
    val blocks = remember(markdown) { parseMarkdown(markdown) }
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
                    Text(
                        text = renderInline(block.text),
                        fontSize = size,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                    )
                }

                is MdBlock.Paragraph -> Text(text = renderInline(block.text), fontSize = 16.sp)

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
                    Text(text = renderInline(block.text), fontSize = 16.sp)
                }

                is MdBlock.Quote -> Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x14000000))
                        .padding(8.dp),
                ) {
                    Text(
                        text = renderInline(block.text),
                        fontStyle = FontStyle.Italic,
                        color = LocalContentColor.current.copy(alpha = 0.8f),
                    )
                }

                MdBlock.Divider -> Text(text = "─".repeat(40), color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}
