package com.example.util

import java.util.Calendar
import java.util.regex.Pattern

data class ParsedTemplateResult(
    val text: String,
    val cursorOffset: Int = -1
)

object VariableParser {

    val AVAILABLE_VARIABLES = listOf(
        VariableChipInfo("{date}", "Current Date", "Default date format"),
        VariableChipInfo("{time}", "Current Time", "Default time format"),
        VariableChipInfo("{day}", "Current Day", "Default day of week"),
        VariableChipInfo("{date:dd/MM/yyyy}", "Custom Date", "Inline date pattern"),
        VariableChipInfo("{date:dd-MMMM-yyyy}", "Full Date", "Inline full date pattern"),
        VariableChipInfo("{time:hh:mm a}", "12h Time", "Inline 12h time with AM/PM"),
        VariableChipInfo("{time:HH:mm}", "24h Time", "Inline 24h pattern"),
        VariableChipInfo("{day:EEEE}", "Full Day", "Inline full day name"),
        VariableChipInfo("{day:EEE}", "Short Day", "Inline short day pattern")
    )

    data class VariableChipInfo(
        val token: String,
        val label: String,
        val description: String
    )

    private val VARIABLE_REGEX = Pattern.compile("\\{([^{}]+)\\}")

    fun parse(
        template: String,
        defaultDateFormat: String = "dd/MM/yyyy",
        defaultDayFormat: String = "EEEE",
        defaultTimeFormat: String = "hh:mm a",
        dateOptions: FormatOptions = FormatOptions(),
        dayOptions: FormatOptions = FormatOptions(),
        timeOptions: FormatOptions = FormatOptions(),
        boldPresets: Set<String> = emptySet(),
        underlinePresets: Set<String> = emptySet(),
        calendar: Calendar = Calendar.getInstance()
    ): ParsedTemplateResult {
        if (template.isEmpty()) {
            return ParsedTemplateResult(text = "", cursorOffset = -1)
        }

        val matcher = VARIABLE_REGEX.matcher(template)
        val sb = StringBuilder()

        var lastEnd = 0
        while (matcher.find()) {
            sb.append(template.substring(lastEnd, matcher.start()))
            val varContent = matcher.group(1) ?: ""

            when {
                varContent == "date" -> {
                    sb.append(FormatEngine.formatWithStyle(defaultDateFormat, calendar, dateOptions, boldPresets, underlinePresets))
                }
                varContent.startsWith("date:") -> {
                    val pattern = varContent.substringAfter("date:")
                    sb.append(FormatEngine.formatWithStyle(pattern, calendar, dateOptions, boldPresets, underlinePresets))
                }
                varContent == "time" -> {
                    sb.append(FormatEngine.formatWithStyle(defaultTimeFormat, calendar, timeOptions, boldPresets, underlinePresets))
                }
                varContent.startsWith("time:") -> {
                    val pattern = varContent.substringAfter("time:")
                    sb.append(FormatEngine.formatWithStyle(pattern, calendar, timeOptions, boldPresets, underlinePresets))
                }
                varContent == "day" -> {
                    sb.append(FormatEngine.formatWithStyle(defaultDayFormat, calendar, dayOptions, boldPresets, underlinePresets))
                }
                varContent.startsWith("day:") -> {
                    val pattern = varContent.substringAfter("day:")
                    sb.append(FormatEngine.formatWithStyle(pattern, calendar, dayOptions, boldPresets, underlinePresets))
                }
                else -> {
                    // Unknown variable stays as plain text
                    sb.append("{").append(varContent).append("}")
                }
            }
            lastEnd = matcher.end()
        }
        sb.append(template.substring(lastEnd))

        return ParsedTemplateResult(
            text = sb.toString(),
            cursorOffset = -1
        )
    }
}
