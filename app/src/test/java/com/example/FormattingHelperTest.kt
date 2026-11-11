package com.example

import com.example.util.FormattingHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FormattingHelperTest {

    @Test
    fun testToggleBoldOnWholeText() {
        val original = "Hello World"
        val (wrapped, _) = FormattingHelper.toggleTag(original, 0, 0, "b")
        assertEquals("<b>Hello World</b>", wrapped)

        val (unwrapped, _) = FormattingHelper.toggleTag(wrapped, 0, 0, "b")
        assertEquals("Hello World", unwrapped)
    }

    @Test
    fun testToggleUnderlineOnSelection() {
        val original = "Hello World"
        val start = 6
        val end = 11 // "World"
        val (wrapped, range) = FormattingHelper.toggleTag(original, start, end, "u")
        assertEquals("Hello <u>World</u>", wrapped)
        assertEquals(6, range.first)
        assertEquals(18, range.last)
    }

    @Test
    fun testStripHtml() {
        val html = "<b>Hello</b> <u>World</u>\n"
        val plain = FormattingHelper.stripHtml(html)
        assertEquals("Hello World", plain)
    }

    @Test
    fun testUnicodeBoldConversion() {
        val html = "<b>ABC</b>"
        val unicode = FormattingHelper.toUnicodeStyled(html)
        // 0x1D400 (𝐀), 0x1D401 (𝐁), 0x1D402 (𝐂)
        val expected = "${String(Character.toChars(0x1D400))}${String(Character.toChars(0x1D401))}${String(Character.toChars(0x1D402))}"
        assertEquals(expected, unicode)
    }

    @Test
    fun testUnicodeUnderlineBanglaAndEnglish() {
        val html = "<u>বাংলা</u>"
        val unicode = FormattingHelper.toUnicodeStyled(html)
        assertTrue(unicode.contains("\u0332"))
    }
}
