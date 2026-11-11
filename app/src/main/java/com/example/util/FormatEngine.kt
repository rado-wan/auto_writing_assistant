package com.example.util

import java.util.Calendar

enum class BanglaPeriodStyle {
    PERIOD, // সকাল, দুপুর, বিকাল, সন্ধ্যা, রাত
    AMPM    // পূর্বাহ্ন, অপরাহ্ন
}

data class FormatOptions(
    val isBanglaText: Boolean = false,
    val isBanglaDigits: Boolean = false,
    val banglaPeriodStyle: BanglaPeriodStyle = BanglaPeriodStyle.PERIOD
)

object FormatEngine {

    val DATE_PRESETS = listOf(
        "dd/MM/yyyy",
        "dd-MM-yyyy",
        "yyyy-MM-dd",
        "d MMMM yyyy",
        "dd-MMMM-yyyy",
        "MMM d, yyyy",
        "dd/MM/yy",
        "dd-MM-yyyy (EEEE)"
    )

    val DAY_PRESETS = listOf(
        "EEEE",
        "EEE"
    )

    val TIME_PRESETS_12H = listOf(
        "hh:mm a",
        "h:mm a",
        "hh:mm:ss a",
        "h:mm:ss a"
    )

    val TIME_PRESETS_24H = listOf(
        "HH:mm",
        "H:mm",
        "HH:mm:ss",
        "H:mm:ss"
    )

    private val BANGLA_MONTHS_FULL = arrayOf(
        "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
        "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
    )

    private val BANGLA_MONTHS_SHORT = arrayOf(
        "জানু", "ফেব", "মার্চ", "এপ্রি", "মে", "জুন",
        "জুলা", "আগ", "সেপ্টে", "অক্টো", "নভে", "ডিসে"
    )

    private val ENGLISH_MONTHS_FULL = arrayOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    private val ENGLISH_MONTHS_SHORT = arrayOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )

    // Calendar.SUNDAY = 1, MONDAY = 2, ..., SATURDAY = 7
    private val BANGLA_DAYS_FULL = arrayOf(
        "", "রবিবার", "সোমবার", "মঙ্গলবার", "বুধবার", "বৃহস্পতিবার", "শুক্রবার", "শনিবার"
    )

    private val BANGLA_DAYS_SHORT = arrayOf(
        "", "রবি", "সোম", "মঙ্গল", "বুধ", "বৃহস্পতি", "শুক্র", "শনি"
    )

    private val ENGLISH_DAYS_FULL = arrayOf(
        "", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
    )

    private val ENGLISH_DAYS_SHORT = arrayOf(
        "", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"
    )

    fun formatWithStyle(
        pattern: String,
        calendar: Calendar = Calendar.getInstance(),
        options: FormatOptions = FormatOptions(),
        boldPresets: Set<String> = emptySet(),
        underlinePresets: Set<String> = emptySet()
    ): String {
        if (pattern.isEmpty()) return ""
        val formatted = format(pattern, calendar, options)
        val isBold = boldPresets.contains(pattern)
        val isUnderline = underlinePresets.contains(pattern)

        var result = formatted
        if (isUnderline && !result.startsWith("<u>", ignoreCase = true)) {
            result = "<u>$result</u>"
        }
        if (isBold && !result.startsWith("<b>", ignoreCase = true)) {
            result = "<b>$result</b>"
        }
        return result
    }

    fun format(
        pattern: String,
        calendar: Calendar = Calendar.getInstance(),
        options: FormatOptions = FormatOptions()
    ): String {
        if (pattern.isEmpty()) return ""

        val year = calendar.get(Calendar.YEAR)
        val month0 = calendar.get(Calendar.MONTH) // 0-based
        val month1 = month0 + 1
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) // 1-7
        val hour24 = calendar.get(Calendar.HOUR_OF_DAY) // 0-23
        val hour12 = calendar.get(Calendar.HOUR).let { if (it == 0) 12 else it } // 1-12
        val minute = calendar.get(Calendar.MINUTE)
        val second = calendar.get(Calendar.SECOND)
        val isAm = calendar.get(Calendar.AM_PM) == Calendar.AM

        val sb = StringBuilder()
        var i = 0
        val len = pattern.length

        while (i < len) {
            val c = pattern[i]
            if (c == '\'') {
                // Literal inside single quotes
                i++
                val start = i
                while (i < len && pattern[i] != '\'') {
                    i++
                }
                sb.append(pattern.substring(start, i))
                if (i < len && pattern[i] == '\'') {
                    i++
                }
            } else if (pattern.startsWith("yyyy", i)) {
                sb.append(String.format("%04d", year))
                i += 4
            } else if (pattern.startsWith("yy", i)) {
                sb.append(String.format("%02d", year % 100))
                i += 2
            } else if (pattern.startsWith("MMMM", i)) {
                val name = if (options.isBanglaText) {
                    BANGLA_MONTHS_FULL.getOrElse(month0) { "" }
                } else {
                    ENGLISH_MONTHS_FULL.getOrElse(month0) { "" }
                }
                sb.append(name)
                i += 4
            } else if (pattern.startsWith("MMM", i)) {
                val name = if (options.isBanglaText) {
                    BANGLA_MONTHS_SHORT.getOrElse(month0) { "" }
                } else {
                    ENGLISH_MONTHS_SHORT.getOrElse(month0) { "" }
                }
                sb.append(name)
                i += 3
            } else if (pattern.startsWith("MM", i)) {
                sb.append(String.format("%02d", month1))
                i += 2
            } else if (pattern.startsWith("M", i)) {
                sb.append(month1.toString())
                i += 1
            } else if (pattern.startsWith("dd", i)) {
                sb.append(String.format("%02d", dayOfMonth))
                i += 2
            } else if (pattern.startsWith("d", i)) {
                sb.append(dayOfMonth.toString())
                i += 1
            } else if (pattern.startsWith("EEEE", i)) {
                val name = if (options.isBanglaText) {
                    BANGLA_DAYS_FULL.getOrElse(dayOfWeek) { "" }
                } else {
                    ENGLISH_DAYS_FULL.getOrElse(dayOfWeek) { "" }
                }
                sb.append(name)
                i += 4
            } else if (pattern.startsWith("EEE", i)) {
                val name = if (options.isBanglaText) {
                    BANGLA_DAYS_SHORT.getOrElse(dayOfWeek) { "" }
                } else {
                    ENGLISH_DAYS_SHORT.getOrElse(dayOfWeek) { "" }
                }
                sb.append(name)
                i += 3
            } else if (pattern.startsWith("HH", i)) {
                sb.append(String.format("%02d", hour24))
                i += 2
            } else if (pattern.startsWith("H", i)) {
                sb.append(hour24.toString())
                i += 1
            } else if (pattern.startsWith("hh", i)) {
                sb.append(String.format("%02d", hour12))
                i += 2
            } else if (pattern.startsWith("h", i)) {
                sb.append(hour12.toString())
                i += 1
            } else if (pattern.startsWith("mm", i)) {
                sb.append(String.format("%02d", minute))
                i += 2
            } else if (pattern.startsWith("ss", i)) {
                sb.append(String.format("%02d", second))
                i += 2
            } else if (pattern.startsWith("a", i)) {
                val ampm = if (options.isBanglaText) {
                    if (options.banglaPeriodStyle == BanglaPeriodStyle.PERIOD) {
                        getBanglaPeriodOfDay(hour24)
                    } else {
                        if (isAm) "পূর্বাহ্ন" else "অপরাহ্ন"
                    }
                } else {
                    if (isAm) "AM" else "PM"
                }
                sb.append(ampm)
                i += 1
            } else {
                sb.append(c)
                i += 1
            }
        }

        var result = sb.toString()
        if (options.isBanglaDigits) {
            result = toBanglaDigits(result)
        }
        return result
    }

    private fun getBanglaPeriodOfDay(hour24: Int): String {
        return when (hour24) {
            in 4..11 -> "সকাল"
            in 12..15 -> "দুপুর"
            in 16..17 -> "বিকাল"
            in 18..19 -> "সন্ধ্যা"
            else -> "রাত"
        }
    }

    fun toBanglaDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            val mapped = when (ch) {
                '0' -> '০'
                '1' -> '১'
                '2' -> '২'
                '3' -> '৩'
                '4' -> '৪'
                '5' -> '৫'
                '6' -> '৬'
                '7' -> '৭'
                '8' -> '৮'
                '9' -> '৯'
                else -> ch
            }
            sb.append(mapped)
        }
        return sb.toString()
    }
}
