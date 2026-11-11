package com.example

import com.example.util.BanglaPeriodStyle
import com.example.util.FormatEngine
import com.example.util.FormatOptions
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class FormatEngineTest {

    private lateinit var calendar: Calendar

    @Before
    fun setUp() {
        // Fix reference date: 2026-11-09 14:30:45 (Monday)
        calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.NOVEMBER) // 10 (0-based) -> 11 (1-based)
            set(Calendar.DAY_OF_MONTH, 9)
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 45)
            set(Calendar.MILLISECOND, 0)
        }
    }

    @Test
    fun testDateTokens_English() {
        val result1 = FormatEngine.format("dd/MM/yyyy", calendar)
        assertEquals("09/11/2026", result1)

        val result2 = FormatEngine.format("d-MMMM-yyyy", calendar)
        assertEquals("9-November-2026", result2)

        val result3 = FormatEngine.format("MMM d, yy", calendar)
        assertEquals("Nov 9, 26", result3)
    }

    @Test
    fun testDayTokens_English() {
        val fullDay = FormatEngine.format("EEEE", calendar)
        assertEquals("Monday", fullDay)

        val shortDay = FormatEngine.format("EEE", calendar)
        assertEquals("Mon", shortDay)
    }

    @Test
    fun testTimeTokens_12H_and_24H() {
        val time24 = FormatEngine.format("HH:mm:ss", calendar)
        assertEquals("14:30:45", time24)

        val time12 = FormatEngine.format("hh:mm a", calendar)
        assertEquals("02:30 PM", time12)

        val time12NoZero = FormatEngine.format("h:mm a", calendar)
        assertEquals("2:30 PM", time12NoZero)
    }

    @Test
    fun testLiteralSingleQuotes() {
        val result = FormatEngine.format("'Date:' dd/MM/yyyy 'Time:' HH:mm", calendar)
        assertEquals("Date: 09/11/2026 Time: 14:30", result)

        val combinedWithParentheses = FormatEngine.format("dd-MM-yyyy (EEEE)", calendar)
        assertEquals("09-11-2026 (Monday)", combinedWithParentheses)
    }

    @Test
    fun testBanglaText_DateAndDay() {
        val options = FormatOptions(isBanglaText = true, isBanglaDigits = false)

        val monthName = FormatEngine.format("MMMM", calendar, options)
        assertEquals("নভেম্বর", monthName)

        val dayName = FormatEngine.format("EEEE", calendar, options)
        assertEquals("সোমবার", dayName)

        val shortDay = FormatEngine.format("EEE", calendar, options)
        assertEquals("সোম", shortDay)
    }

    @Test
    fun testBanglaDigits() {
        val options = FormatOptions(isBanglaText = false, isBanglaDigits = true)
        val dateWithBanglaDigits = FormatEngine.format("dd/MM/yyyy", calendar, options)
        assertEquals("০৯/১১/২০২৬", dateWithBanglaDigits)
    }

    @Test
    fun testBanglaTime_PeriodStyle() {
        // 14:30 is between 12 and 15 -> দুপুর
        val options = FormatOptions(
            isBanglaText = true,
            isBanglaDigits = true,
            banglaPeriodStyle = BanglaPeriodStyle.PERIOD
        )
        val timeWithPeriod = FormatEngine.format("hh:mm a", calendar, options)
        assertEquals("০২:৩০ দুপুর", timeWithPeriod)

        // Morning test 09:15 -> সকাল
        val morningCal = (calendar.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 15)
        }
        val morningResult = FormatEngine.format("hh:mm a", morningCal, options)
        assertEquals("০৯:১৫ সকাল", morningResult)
    }

    @Test
    fun testBanglaTime_DirectAmPm() {
        val options = FormatOptions(
            isBanglaText = true,
            isBanglaDigits = false,
            banglaPeriodStyle = BanglaPeriodStyle.AMPM
        )
        val timeAmPm = FormatEngine.format("hh:mm a", calendar, options)
        assertEquals("02:30 অপরাহ্ন", timeAmPm)
    }

    @Test
    fun testFormatWithStyle_BoldAndUnderline() {
        val boldPattern = "dd/MM/yyyy"
        val underlinePattern = "EEEE"
        val normalPattern = "hh:mm a"

        val boldResult = FormatEngine.formatWithStyle(
            pattern = boldPattern,
            calendar = calendar,
            boldPresets = setOf(boldPattern),
            underlinePresets = emptySet()
        )
        assertEquals("<b>09/11/2026</b>", boldResult)

        val underlineResult = FormatEngine.formatWithStyle(
            pattern = underlinePattern,
            calendar = calendar,
            boldPresets = emptySet(),
            underlinePresets = setOf(underlinePattern)
        )
        assertEquals("<u>Monday</u>", underlineResult)

        val bothResult = FormatEngine.formatWithStyle(
            pattern = boldPattern,
            calendar = calendar,
            boldPresets = setOf(boldPattern),
            underlinePresets = setOf(boldPattern)
        )
        assertEquals("<b><u>09/11/2026</u></b>", bothResult)

        val normalResult = FormatEngine.formatWithStyle(
            pattern = normalPattern,
            calendar = calendar,
            boldPresets = emptySet(),
            underlinePresets = emptySet()
        )
        assertEquals("02:30 PM", normalResult)
    }

    @Test
    fun testFormatWithStyle_PartialCustomStyling() {
        // Custom format: <b>{day}</b>, {date} or <b>EEEE</b>, dd/MM/yyyy
        val customPattern = "<b>EEEE</b>, dd/MM/yyyy"
        val result = FormatEngine.formatWithStyle(
            pattern = customPattern,
            calendar = calendar,
            boldPresets = emptySet(),
            underlinePresets = emptySet()
        )
        assertEquals("<b>Monday</b>, 09/11/2026", result)
    }
}
