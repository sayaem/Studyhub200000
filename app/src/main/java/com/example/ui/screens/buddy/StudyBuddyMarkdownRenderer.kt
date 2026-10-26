package com.example.ui.screens.buddy

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Established StudyHub design tokens
private val DarkTextColor = Color(0xFF0F172A)
private val SubtitleColor = Color(0xFF64748B)
private val PrimaryTeal = Color(0xFF0796A3)
private val CodeBackground = Color(0xFFF1F5F9)
private val DarkCodeBackground = Color(0xFF1E293B)
private val CalloutBg = Color(0xFFF0FDF4)
private val CalloutBorder = Color(0xFF86EFAC)
private val PriorityBg = Color(0xFFF0F9FF)
private val PriorityBorder = Color(0xFF7DD3FC)

/**
 * Parsed visual block for rendering formatted Markdown.
 */
sealed interface MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
    data class BulletItem(val text: String) : MarkdownBlock
    data class NumberedItem(val number: String, val text: String) : MarkdownBlock
    data class CodeBlock(val code: String, val language: String? = null) : MarkdownBlock
    data class MathDisplay(val mathContent: String) : MarkdownBlock
    data class Callout(val text: String, val iconType: String = "INFO") : MarkdownBlock
    data class Table(val headers: List<String>, val rows: List<List<String>>) : MarkdownBlock
    data object Divider : MarkdownBlock
}

/**
 * Master parser that converts raw AI Markdown text into structured blocks.
 */
object StudyBuddyMarkdownParser {

    fun parse(rawContent: String): List<MarkdownBlock> {
        val lines = rawContent.lines()
        val blocks = mutableListOf<MarkdownBlock>()
        var i = 0

        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trim()

            // 1. Code blocks (```)
            if (trimmed.startsWith("```")) {
                val language = trimmed.removePrefix("```").trim()
                val codeLines = mutableListOf<String>()
                i++
                while (i < lines.size && !lines[i].trim().startsWith("```")) {
                    codeLines.add(lines[i])
                    i++
                }
                blocks.add(MarkdownBlock.CodeBlock(codeLines.joinToString("\n"), language.ifBlank { null }))
                i++
                continue
            }

            // 1.1 Display Math blocks: $$ ... $$ or \[ ... \]
            if (trimmed.startsWith("$$") || trimmed.startsWith("\\[")) {
                val isBracket = trimmed.startsWith("\\[")
                val endMarker = if (isBracket) "\\]" else "$$"
                if (trimmed.endsWith(endMarker) && trimmed.length > 4) {
                    val mathContent = trimmed.removePrefix(if (isBracket) "\\[" else "$$")
                        .removeSuffix(endMarker).trim()
                    blocks.add(MarkdownBlock.MathDisplay(mathContent))
                    i++
                    continue
                } else {
                    val mathLines = mutableListOf<String>()
                    val firstLine = trimmed.removePrefix(if (isBracket) "\\[" else "$$").trim()
                    if (firstLine.isNotBlank()) mathLines.add(firstLine)
                    i++
                    while (i < lines.size && !lines[i].trim().contains(endMarker)) {
                        mathLines.add(lines[i])
                        i++
                    }
                    if (i < lines.size) {
                        val lastLine = lines[i].trim().substringBefore(endMarker).trim()
                        if (lastLine.isNotBlank()) mathLines.add(lastLine)
                        i++
                    }
                    blocks.add(MarkdownBlock.MathDisplay(mathLines.joinToString("\n").trim()))
                    continue
                }
            }

            // 2. Horizontal divider (--- or ***)
            if (trimmed == "---" || trimmed == "***" || trimmed == "___") {
                blocks.add(MarkdownBlock.Divider)
                i++
                continue
            }

            // 3. Headings (#, ##, ###)
            if (trimmed.startsWith("### ")) {
                blocks.add(MarkdownBlock.Heading(3, trimmed.removePrefix("### ").trim()))
                i++
                continue
            }
            if (trimmed.startsWith("## ")) {
                blocks.add(MarkdownBlock.Heading(2, trimmed.removePrefix("## ").trim()))
                i++
                continue
            }
            if (trimmed.startsWith("# ")) {
                blocks.add(MarkdownBlock.Heading(1, trimmed.removePrefix("# ").trim()))
                i++
                continue
            }

            // 4. Blockquotes or Callouts (> or icons)
            if (trimmed.startsWith("> ")) {
                val quoteText = trimmed.removePrefix("> ").trim()
                blocks.add(MarkdownBlock.Callout(quoteText, iconType = "QUOTE"))
                i++
                continue
            }

            // 5. Tables (| col1 | col2 |)
            if (trimmed.startsWith("|") && trimmed.endsWith("|") && trimmed.count { it == '|' } >= 2) {
                val tableLines = mutableListOf<String>()
                while (i < lines.size && lines[i].trim().startsWith("|") && lines[i].trim().endsWith("|")) {
                    tableLines.add(lines[i].trim())
                    i++
                }
                val tableBlock = parseTable(tableLines)
                if (tableBlock != null) {
                    blocks.add(tableBlock)
                }
                continue
            }

            // 6. Bullet lists (*, -, •, +)
            val bulletMatch = Regex("""^([*•\-+])\s+(.*)""").find(trimmed)
            if (bulletMatch != null) {
                val itemText = bulletMatch.groupValues[2]
                blocks.add(MarkdownBlock.BulletItem(itemText))
                i++
                continue
            }

            // 7. Numbered lists (1., 2.)
            val numberedMatch = Regex("""^(\d+)\.\s+(.*)""").find(trimmed)
            if (numberedMatch != null) {
                val num = numberedMatch.groupValues[1]
                val itemText = numberedMatch.groupValues[2]
                blocks.add(MarkdownBlock.NumberedItem(num, itemText))
                i++
                continue
            }

            // 8. Standalone bold labels acting as subheaders (e.g. "**Revisions Due Today:**")
            val boldHeaderMatch = Regex("""^\*\*([^*]+)\*\*:?$""").find(trimmed)
            if (boldHeaderMatch != null) {
                val headingText = boldHeaderMatch.groupValues[1].trim().removeSuffix(":")
                blocks.add(MarkdownBlock.Heading(3, headingText))
                i++
                continue
            }

            // 9. Regular text paragraphs (accumulate consecutive non-empty lines)
            if (trimmed.isNotEmpty()) {
                val paraLines = mutableListOf<String>()
                while (i < lines.size) {
                    val nextLine = lines[i]
                    val nextTrimmed = nextLine.trim()
                    if (nextTrimmed.isEmpty() ||
                        nextTrimmed.startsWith("```") ||
                        nextTrimmed.startsWith("$$") ||
                        nextTrimmed.startsWith("\\[") ||
                        nextTrimmed.startsWith("#") ||
                        nextTrimmed.startsWith("> ") ||
                        nextTrimmed.startsWith("|") ||
                        nextTrimmed.matches(Regex("""^([*•\-+]|\d+\.)\s+.*""")) ||
                        Regex("""^\*\*([^*]+)\*\*:?$""").matches(nextTrimmed)
                    ) {
                        break
                    }
                    paraLines.add(nextLine)
                    i++
                }
                if (paraLines.isNotEmpty()) {
                    blocks.add(MarkdownBlock.Paragraph(paraLines.joinToString("\n")))
                }
                continue
            }

            i++
        }

        return blocks
    }

    private fun parseTable(lines: List<String>): MarkdownBlock.Table? {
        if (lines.isEmpty()) return null
        val headers = lines[0].split("|").map { it.trim() }.filter { it.isNotEmpty() }
        val rows = mutableListOf<List<String>>()

        val startIdx = if (lines.size > 1 && lines[1].contains("---")) 2 else 1
        for (rowIdx in startIdx until lines.size) {
            val cells = lines[rowIdx].split("|").map { it.trim() }.filter { it.isNotEmpty() }
            if (cells.isNotEmpty()) {
                rows.add(cells)
            }
        }
        return MarkdownBlock.Table(headers, rows)
    }

    /**
     * Converts inline Markdown formatting into a rich AnnotatedString.
     * Safely parses **bold**, *italic*, ***bold-italic***, `code`, ~~strike~~, and [links](url).
     */
    fun parseInlineMarkdown(text: String): AnnotatedString {
        return buildAnnotatedString {
            var index = 0
            val length = text.length

            while (index < length) {
                // Inline Math: \( ... \)
                if (index + 1 < length && text.substring(index, index + 2) == "\\(") {
                    val endIdx = text.indexOf("\\)", index + 2)
                    if (endIdx != -1) {
                        val rawLatex = text.substring(index + 2, endIdx)
                        val formattedMath = StudyBuddyMathEngine.formatLatexToUnicode(rawLatex)
                        pushStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Serif,
                                fontStyle = FontStyle.Normal,
                                fontWeight = FontWeight.SemiBold,
                                color = DarkTextColor,
                                fontSize = 14.5.sp
                            )
                        )
                        append(formattedMath)
                        pop()
                        index = endIdx + 2
                        continue
                    }
                }

                // Inline Display Math: \[ ... \]
                if (index + 1 < length && text.substring(index, index + 2) == "\\[") {
                    val endIdx = text.indexOf("\\]", index + 2)
                    if (endIdx != -1) {
                        val rawLatex = text.substring(index + 2, endIdx)
                        val formattedMath = StudyBuddyMathEngine.formatLatexToUnicode(rawLatex)
                        pushStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Serif,
                                fontStyle = FontStyle.Normal,
                                fontWeight = FontWeight.Bold,
                                color = DarkTextColor,
                                fontSize = 15.sp
                            )
                        )
                        append(" $formattedMath ")
                        pop()
                        index = endIdx + 2
                        continue
                    }
                }

                // Double dollar display math in line: $$ ... $$
                if (index + 1 < length && text.substring(index, index + 2) == "$$") {
                    val endIdx = text.indexOf("$$", index + 2)
                    if (endIdx != -1) {
                        val rawLatex = text.substring(index + 2, endIdx)
                        val formattedMath = StudyBuddyMathEngine.formatLatexToUnicode(rawLatex)
                        pushStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Serif,
                                fontStyle = FontStyle.Normal,
                                fontWeight = FontWeight.Bold,
                                color = DarkTextColor,
                                fontSize = 15.sp
                            )
                        )
                        append(" $formattedMath ")
                        pop()
                        index = endIdx + 2
                        continue
                    }
                }

                // Single dollar inline math: $ ... $ (exclude currency like $10 or $20)
                if (text[index] == '$' && (index + 1 < length && !text[index + 1].isWhitespace() && !text[index + 1].isDigit())) {
                    val endIdx = text.indexOf('$', index + 1)
                    if (endIdx != -1 && endIdx > index + 1 && !text[endIdx - 1].isWhitespace()) {
                        val rawLatex = text.substring(index + 1, endIdx)
                        val formattedMath = StudyBuddyMathEngine.formatLatexToUnicode(rawLatex)
                        pushStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Serif,
                                fontStyle = FontStyle.Normal,
                                fontWeight = FontWeight.SemiBold,
                                color = DarkTextColor,
                                fontSize = 14.5.sp
                            )
                        )
                        append(formattedMath)
                        pop()
                        index = endIdx + 1
                        continue
                    }
                }

                // Inline code: `code`
                if (text[index] == '`') {
                    val endIdx = text.indexOf('`', index + 1)
                    if (endIdx != -1) {
                        val codeContent = text.substring(index + 1, endIdx)
                        val start = length
                        pushStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = CodeBackground,
                                color = DarkTextColor,
                                fontSize = 13.sp
                            )
                        )
                        append(codeContent)
                        pop()
                        index = endIdx + 1
                        continue
                    }
                }

                // Bold Italic: ***text***
                if (index + 2 < length && text.substring(index, index + 3) == "***") {
                    val endIdx = text.indexOf("***", index + 3)
                    if (endIdx != -1) {
                        val content = text.substring(index + 3, endIdx)
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic, color = DarkTextColor))
                        append(content)
                        pop()
                        index = endIdx + 3
                        continue
                    }
                }

                // Bold: **text** or __text__
                if (index + 1 < length && (text.substring(index, index + 2) == "**" || text.substring(index, index + 2) == "__")) {
                    val delimiter = text.substring(index, index + 2)
                    val endIdx = text.indexOf(delimiter, index + 2)
                    if (endIdx != -1) {
                        val content = text.substring(index + 2, endIdx)
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = DarkTextColor))
                        append(content)
                        pop()
                        index = endIdx + 2
                        continue
                    }
                }

                // Italic: *text* or _text_
                if (text[index] == '*' || text[index] == '_') {
                    val delimiter = text[index]
                    val endIdx = text.indexOf(delimiter, index + 1)
                    if (endIdx != -1 && endIdx > index + 1 && (index == 0 || text[index - 1].isWhitespace())) {
                        val content = text.substring(index + 1, endIdx)
                        pushStyle(SpanStyle(fontStyle = FontStyle.Italic, color = DarkTextColor))
                        append(content)
                        pop()
                        index = endIdx + 1
                        continue
                    }
                }

                // Strikethrough: ~~text~~
                if (index + 1 < length && text.substring(index, index + 2) == "~~") {
                    val endIdx = text.indexOf("~~", index + 2)
                    if (endIdx != -1) {
                        val content = text.substring(index + 2, endIdx)
                        pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough, color = SubtitleColor))
                        append(content)
                        pop()
                        index = endIdx + 2
                        continue
                    }
                }

                // Links: [text](url)
                if (text[index] == '[') {
                    val closeBracket = text.indexOf(']', index + 1)
                    if (closeBracket != -1 && closeBracket + 1 < length && text[closeBracket + 1] == '(') {
                        val closeParen = text.indexOf(')', closeBracket + 2)
                        if (closeParen != -1) {
                            val linkText = text.substring(index + 1, closeBracket)
                            val linkUrl = text.substring(closeBracket + 2, closeParen)
                            pushStringAnnotation(tag = "URL", annotation = linkUrl)
                            pushStyle(
                                SpanStyle(
                                    color = PrimaryTeal,
                                    fontWeight = FontWeight.SemiBold,
                                    textDecoration = TextDecoration.Underline
                                )
                            )
                            append(linkText)
                            pop()
                            pop()
                            index = closeParen + 1
                            continue
                        }
                    }
                }

                // Plain character
                append(text[index])
                index++
            }
        }
    }
}

/**
 * Premium native Jetpack Compose Markdown & rich response renderer.
 */
@Composable
fun StudyBuddyMarkdownRenderer(
    content: String,
    modifier: Modifier = Modifier
) {
    val blocks = remember(content) { StudyBuddyMarkdownParser.parse(content) }
    val context = LocalContext.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Heading -> {
                    val style = when (block.level) {
                        1 -> MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = DarkTextColor,
                            fontSize = 17.sp
                        )
                        2 -> MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = DarkTextColor,
                            fontSize = 15.sp
                        )
                        else -> MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = PrimaryTeal,
                            fontSize = 14.sp
                        )
                    }
                    Text(
                        text = StudyBuddyMarkdownParser.parseInlineMarkdown(block.text),
                        style = style,
                        modifier = Modifier.padding(top = if (block.level <= 2) 4.dp else 2.dp)
                    )
                }

                is MarkdownBlock.Paragraph -> {
                    val annotated = remember(block.text) { StudyBuddyMarkdownParser.parseInlineMarkdown(block.text) }
                    Text(
                        text = annotated,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = DarkTextColor,
                            lineHeight = 22.sp,
                            fontSize = 14.sp
                        )
                    )
                }

                is MarkdownBlock.BulletItem -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 1.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 7.dp, start = 4.dp, end = 8.dp)
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(PrimaryTeal)
                        )
                        val annotated = remember(block.text) { StudyBuddyMarkdownParser.parseInlineMarkdown(block.text) }
                        Text(
                            text = annotated,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = DarkTextColor,
                                lineHeight = 21.sp,
                                fontSize = 13.5.sp
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                is MarkdownBlock.NumberedItem -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = PriorityBg,
                            modifier = Modifier
                                .padding(top = 1.dp, end = 8.dp)
                                .size(20.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = block.number,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryTeal,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        val annotated = remember(block.text) { StudyBuddyMarkdownParser.parseInlineMarkdown(block.text) }
                        Text(
                            text = annotated,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = DarkTextColor,
                                lineHeight = 21.sp,
                                fontSize = 13.5.sp
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                is MarkdownBlock.MathDisplay -> {
                    DisplayMathCard(mathLatex = block.mathContent)
                }

                is MarkdownBlock.Callout -> {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = CalloutBg,
                        border = BorderStroke(1.dp, CalloutBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = PrimaryTeal,
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            val annotated = remember(block.text) { StudyBuddyMarkdownParser.parseInlineMarkdown(block.text) }
                            Text(
                                text = annotated,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = DarkTextColor,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 19.sp
                                )
                            )
                        }
                    }
                }

                is MarkdownBlock.CodeBlock -> {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DarkCodeBackground,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            if (!block.language.isNullOrBlank()) {
                                Text(
                                    text = block.language.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                            Text(
                                text = block.code,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFFE2E8F0),
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp
                                ),
                                modifier = Modifier.horizontalScroll(rememberScrollState())
                            )
                        }
                    }
                }

                is MarkdownBlock.Table -> {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            // Headers
                            Row(modifier = Modifier.fillMaxWidth()) {
                                block.headers.forEach { h ->
                                    Text(
                                        text = h,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = DarkTextColor,
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(4.dp)
                                    )
                                }
                            }
                            HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)
                            // Rows
                            block.rows.forEachIndexed { rIdx, row ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (rIdx % 2 == 1) Color(0xFFF8FAFC) else Color.White)
                                ) {
                                    row.forEach { cell ->
                                        Text(
                                            text = StudyBuddyMarkdownParser.parseInlineMarkdown(cell),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = DarkTextColor,
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                is MarkdownBlock.Divider -> {
                    HorizontalDivider(
                        color = Color(0xFFE2E8F0),
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}
