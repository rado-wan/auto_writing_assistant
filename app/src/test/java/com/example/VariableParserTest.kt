package com.example

import com.example.util.FormatOptions
import com.example.util.VariableParser
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class VariableParserTest {

    private lateinit var calendar: Calendar

    @Before
    fun setUp() {
        calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.NOVEMBER)
            set(Calendar.DAY_OF_MONTH, 9)
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
        }
    }

    @Test
    fun testDefaultVariables() {
        val template = "Today is {date} on a {day} at {time}."
        val result = VariableParser.parse(
            template = template,
            defaultDateFormat = "dd/MM/yyyy",
            defaultDayFormat = "EEEE",
            defaultTimeFormat = "HH:mm",
            calendar = calendar
        )

        assertEquals("Today is 09/11/2026 on a Monday at 14:30.", result.text)
        assertEquals(-1, result.cursorOffset)
    }

    @Test
    fun testInlineCustomPatterns() {
        val template = "Logged: {date:yyyy-MM-dd} ({day:EEE}) at {time:hh:mm a}"
        val result = VariableParser.parse(
            template = template,
            calendar = calendar
        )

        assertEquals("Logged: 2026-11-09 (Mon) at 02:30 PM", result.text)
    }

    @Test
    fun testBanglaVariables() {
        val banglaOptions = FormatOptions(
            isBanglaDigits = true,
            isBanglaText = true
        )
        val template = "{date:dd MMMM yyyy} {day:EEEE}"
        val result = VariableParser.parse(
            template = template,
            dateOptions = banglaOptions,
            dayOptions = banglaOptions,
            calendar = calendar
        )

        assertEquals("০৯ নভেম্বর ২০২৬ সোমবার", result.text)
    }

    @Test
    fun testUnknownVariablesStayPlainText() {
        val template = "Hello {name}, your code is {code}."
        val result = VariableParser.parse(
            template = template,
            calendar = calendar
        )

        assertEquals("Hello {name}, your code is {code}.", result.text)
    }

    @Test
    fun testStyledPresetsInVariableParser() {
        val template = "Date: {date}, Day: {day}, Time: {time}"
        val result = VariableParser.parse(
            template = template,
            defaultDateFormat = "dd/MM/yyyy",
            defaultDayFormat = "EEEE",
            defaultTimeFormat = "hh:mm a",
            calendar = calendar,
            boldPresets = setOf("dd/MM/yyyy"), // Bold date
            underlinePresets = setOf("EEEE")   // Underline day, normal time
        )

        assertEquals("Date: <b>09/11/2026</b>, Day: <u>Monday</u>, Time: 02:30 PM", result.text)
    }
}
