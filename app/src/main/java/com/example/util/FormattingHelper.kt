package com.example.util

import android.content.ClipData
import androidx.core.text.HtmlCompat

object FormattingHelper {

    /**
     * Toggles the given tag (e.g. "b" or "u") on the text.
     * If start == end (no selection), toggles the tag on the entire text.
     * If start < end, toggles the tag on the selected substring.
     */
    fun toggleTag(text: String, start: Int, end: Int, tag: String): Pair<String, IntRange> {
        val openTag = "<$tag>"
        val closeTag = "</$tag>"

        if (start < 0 || end < 0 || start > text.length || end > text.length) {
            return Pair(text, 0..text.length)
        }

        // Whole text formatting if no selection
        if (start == end) {
            val trimmed = text.trim()
            return if (trimmed.startsWith(openTag, ignoreCase = true) && trimmed.endsWith(closeTag, ignoreCase = true)) {
                // Unwrap outer tag
                val stripped = trimmed.substring(openTag.length, trimmed.length - closeTag.length)
                Pair(stripped, 0..stripped.length)
            } else {
                val wrapped = "$openTag$text$closeTag"
                Pair(wrapped, 0..wrapped.length)
            }
        }

        val selStart = start.coerceAtMost(end)
        val selEnd = start.coerceAtLeast(end)
        val selectedText = text.substring(selStart, selEnd)
        val before = text.substring(0, selStart)
        val after = text.substring(selEnd)

        val trimmedSel = selectedText.trim()
        val resultText: String
        val newEnd: Int

        if (trimmedSel.startsWith(openTag, ignoreCase = true) && trimmedSel.endsWith(closeTag, ignoreCase = true)) {
            // Remove tag from selection
            val unwrapped = trimmedSel.substring(openTag.length, trimmedSel.length - closeTag.length)
            resultText = before + unwrapped + after
            newEnd = selStart + unwrapped.length
        } else {
            // Wrap selection
            val wrapped = "$openTag$selectedText$closeTag"
            resultText = before + wrapped + after
            newEnd = selStart + wrapped.length
        }

        return Pair(resultText, selStart..newEnd)
    }

    /**
     * Strips HTML tags, leaving clean plain text.
     */
    fun stripHtml(html: String): String {
        return html.replace(Regex("<[^>]*>"), "").trimEnd('\n')
    }

    /**
     * Converts HTML bold and underline tags into Unicode styled text.
     * - Bold uses Mathematical Bold characters for ASCII letters and numbers (bold doesn't exist for Bangla).
     * - Underline uses the combining mark U+0332, which works for both English and Bangla.
     */
    fun toUnicodeStyled(html: String): String {
        val result = StringBuilder()
        var isBold = false
        var isUnderline = false
        var i = 0

        while (i < html.length) {
            if (html.startsWith("<b>", i, ignoreCase = true)) {
                isBold = true
                i += 3
            } else if (html.startsWith("</b>", i, ignoreCase = true)) {
                isBold = false
                i += 4
            } else if (html.startsWith("<u>", i, ignoreCase = true)) {
                isUnderline = true
                i += 3
            } else if (html.startsWith("</u>", i, ignoreCase = true)) {
                isUnderline = false
                i += 4
            } else if (html[i] == '<') {
                // Skip other HTML tags
                val endIdx = html.indexOf('>', i)
                if (endIdx != -1) {
                    i = endIdx + 1
                } else {
                    result.append(html[i])
                    i++
                }
            } else {
                val c = html[i]
                val styledChar = if (isBold) toBoldChar(c) else c.toString()
                result.append(styledChar)
                if (isUnderline && c != '\n' && c != '\r') {
                    result.append('\u0332') // Combining low line
                }
                i++
            }
        }

        return result.toString()
    }

    private fun toBoldChar(c: Char): String {
        return when (c) {
            in 'A'..'Z' -> String(Character.toChars(0x1D400 + (c - 'A')))
            in 'a'..'z' -> String(Character.toChars(0x1D41A + (c - 'a')))
            in '0'..'9' -> String(Character.toChars(0x1D7CE + (c - '0')))
            else -> c.toString()
        }
    }

    /**
     * Prepares appropriate ClipData based on formatting settings.
     */
    fun createClipData(
        label: String = "Writing Assistant",
        bodyWithVariables: String,
        isFormattingEnabled: Boolean,
        formattingStyle: String // "html" or "unicode"
    ): ClipData {
        if (!isFormattingEnabled) {
            val plain = stripHtml(bodyWithVariables)
            return ClipData.newPlainText(label, plain)
        }

        return if (formattingStyle == "unicode") {
            val unicodeStyled = toUnicodeStyled(bodyWithVariables)
            ClipData.newPlainText(label, unicodeStyled)
        } else {
            // Default: Rich (HTML)
            val plain = stripHtml(bodyWithVariables)
            ClipData.newHtmlText(label, plain, bodyWithVariables)
        }
    }

    /**
     * Converts an HTML string with <b> and <u> tags into a styled Compose AnnotatedString.
     */
    fun renderHtmlToAnnotatedString(html: String): androidx.compose.ui.text.AnnotatedString {
        return try {
            val spanned = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT)
            val text = spanned.toString().trimEnd('\n')
            val builder = androidx.compose.ui.text.AnnotatedString.Builder(text)

            val boldSpans = spanned.getSpans(0, spanned.length, android.text.style.StyleSpan::class.java)
            for (span in boldSpans) {
                if (span.style == android.graphics.Typeface.BOLD) {
                    val start = spanned.getSpanStart(span).coerceIn(0, text.length)
                    val end = spanned.getSpanEnd(span).coerceIn(0, text.length)
                    if (start < end) {
                        builder.addStyle(
                            androidx.compose.ui.text.SpanStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                            start,
                            end
                        )
                    }
                }
            }

            val underlineSpans = spanned.getSpans(0, spanned.length, android.text.style.UnderlineSpan::class.java)
            for (span in underlineSpans) {
                val start = spanned.getSpanStart(span).coerceIn(0, text.length)
                val end = spanned.getSpanEnd(span).coerceIn(0, text.length)
                if (start < end) {
                    builder.addStyle(
                        androidx.compose.ui.text.SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline),
                        start,
                        end
                    )
                }
            }

            builder.toAnnotatedString()
        } catch (_: Exception) {
            androidx.compose.ui.text.AnnotatedString(stripHtml(html))
        }
    }
}
