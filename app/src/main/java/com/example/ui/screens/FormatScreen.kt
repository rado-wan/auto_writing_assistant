package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.preferences.AppSettings
import com.example.ui.viewmodel.MainViewModel
import com.example.util.FormatEngine
import com.example.util.FormatOptions
import com.example.util.FormattingHelper
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormatScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Formats",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("formats_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to home"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Unified English / Bangla Toggle Card
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (settings.isBangla) "বাংলা (Bangla)" else "English",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (settings.isBangla) "Outputs বাংলা দিন, মাস ও সংখ্যা (০-৯)" else "Outputs standard English date & time",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = settings.isBangla,
                            onCheckedChange = { viewModel.setIsBangla(it) },
                            modifier = Modifier.testTag("bangla_language_switch")
                        )
                    }
                }
            }

            // Group 1: DATE
            FormatGroupCard(
                title = "Date Formats",
                icon = Icons.Default.CalendarMonth,
                currentFormat = settings.defaultDateFormat,
                presets = listOf(
                    "dd/MM/yyyy",
                    "dd-MM-yyyy",
                    "dd MMMM yyyy",
                    "yyyy-MM-dd",
                    "dd-MM-yyyy (EEEE)"
                ),
                options = settings.getDateFormatOptions(),
                tokens = listOf("dd", "d", "MM", "M", "MMM", "MMMM", "yy", "yyyy", "'/'", "'-'"),
                defaultFallback = "dd/MM/yyyy",
                customFormatValue = settings.customDateFormat,
                settings = settings,
                onSelectFormat = { viewModel.setDefaultDateFormat(it) },
                onSaveCustom = { viewModel.setCustomDateFormat(it) },
                onToggleBold = { viewModel.togglePresetBold(it) },
                onToggleUnderline = { viewModel.togglePresetUnderline(it) }
            )

            // Group 2: TIME
            FormatGroupCard(
                title = "Time Formats",
                icon = Icons.Default.Schedule,
                currentFormat = settings.defaultTimeFormat,
                presets = listOf(
                    "hh:mm a",
                    "HH:mm",
                    "hh:mm:ss a",
                    "HH:mm:ss"
                ),
                options = settings.getTimeFormatOptions(),
                tokens = listOf("hh", "h", "HH", "H", "mm", "ss", "a", "':'"),
                defaultFallback = "hh:mm a",
                customFormatValue = settings.customTimeFormat,
                settings = settings,
                onSelectFormat = { viewModel.setDefaultTimeFormat(it) },
                onSaveCustom = { viewModel.setCustomTimeFormat(it) },
                onToggleBold = { viewModel.togglePresetBold(it) },
                onToggleUnderline = { viewModel.togglePresetUnderline(it) }
            )

            // Group 3: DAY
            FormatGroupCard(
                title = "Day Formats",
                icon = Icons.Default.Today,
                currentFormat = settings.defaultDayFormat,
                presets = listOf(
                    "EEEE",
                    "EEE",
                    "'('EEEE')'"
                ),
                options = settings.getDayFormatOptions(),
                tokens = listOf("EEEE", "EEE", "'('", "')'"),
                defaultFallback = "EEEE",
                customFormatValue = settings.customDayFormat,
                settings = settings,
                onSelectFormat = { viewModel.setDefaultDayFormat(it) },
                onSaveCustom = { viewModel.setCustomDayFormat(it) },
                onToggleBold = { viewModel.togglePresetBold(it) },
                onToggleUnderline = { viewModel.togglePresetUnderline(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FormatGroupCard(
    title: String,
    icon: ImageVector,
    currentFormat: String,
    presets: List<String>,
    options: FormatOptions,
    tokens: List<String>,
    defaultFallback: String,
    customFormatValue: String,
    settings: AppSettings,
    onSelectFormat: (String) -> Unit,
    onSaveCustom: (String) -> Unit,
    onToggleBold: (String) -> Unit,
    onToggleUnderline: (String) -> Unit
) {
    var isCustomEditorExpanded by remember { mutableStateOf(false) }
    var customInputValue by remember(customFormatValue) {
        mutableStateOf(TextFieldValue(customFormatValue, selection = TextRange(customFormatValue.length)))
    }

    val isCustomBold = settings.isPresetBold(customInputValue.text)
    val isCustomUnderline = settings.isPresetUnderline(customInputValue.text)

    val customFormattedPreviewHtml = remember(
        customInputValue.text,
        options,
        isCustomBold,
        isCustomUnderline,
        settings.formatBoldPresets,
        settings.formatUnderlinePresets
    ) {
        FormatEngine.formatWithStyle(
            pattern = customInputValue.text,
            calendar = Calendar.getInstance(),
            options = options,
            boldPresets = settings.formatBoldPresets,
            underlinePresets = settings.formatUnderlinePresets
        )
    }
    val customAnnotatedPreview = remember(customFormattedPreviewHtml) {
        FormattingHelper.renderHtmlToAnnotatedString(customFormattedPreviewHtml)
    }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Presets List with per-preset B and U buttons
            presets.forEach { pattern ->
                val isSelected = currentFormat == pattern
                val isBold = settings.isPresetBold(pattern)
                val isUnderline = settings.isPresetUnderline(pattern)

                val previewHtml = remember(pattern, options, isBold, isUnderline, settings.formatBoldPresets, settings.formatUnderlinePresets) {
                    FormatEngine.formatWithStyle(
                        pattern = pattern,
                        calendar = Calendar.getInstance(),
                        options = options,
                        boldPresets = settings.formatBoldPresets,
                        underlinePresets = settings.formatUnderlinePresets
                    )
                }
                val annotatedPreview = remember(previewHtml) {
                    FormattingHelper.renderHtmlToAnnotatedString(previewHtml)
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectFormat(pattern) }
                        .padding(vertical = 3.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = annotatedPreview,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected && !isBold) FontWeight.SemiBold else if (isBold) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = pattern,
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Bold toggle button (B)
                            FilledTonalIconButton(
                                onClick = { onToggleBold(pattern) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("preset_bold_${pattern}"),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = if (isBold) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    contentColor = if (isBold) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FormatBold,
                                    contentDescription = "Toggle bold for $pattern",
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Underline toggle button (U)
                            FilledTonalIconButton(
                                onClick = { onToggleUnderline(pattern) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("preset_underline_${pattern}"),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = if (isUnderline) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    contentColor = if (isUnderline) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FormatUnderlined,
                                    contentDescription = "Toggle underline for $pattern",
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            if (isSelected) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Custom format row
            val isCustomActive = currentFormat == customInputValue.text && customInputValue.text.isNotBlank() && !presets.contains(customInputValue.text)

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isCustomActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        isCustomEditorExpanded = !isCustomEditorExpanded
                        if (customInputValue.text.isNotBlank()) {
                            onSelectFormat(customInputValue.text)
                        }
                    }
                    .padding(vertical = 3.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Custom format",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isCustomActive) FontWeight.Bold else FontWeight.SemiBold
                            )
                            if (customInputValue.text.isBlank()) {
                                Text(
                                    text = "Tap to create custom pattern",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    text = customAnnotatedPreview,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (customInputValue.text.isNotBlank()) {
                            // Row B and U buttons for custom pattern
                            FilledTonalIconButton(
                                onClick = { onToggleBold(customInputValue.text) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("custom_bold_toggle"),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = if (isCustomBold) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    contentColor = if (isCustomBold) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FormatBold,
                                    contentDescription = "Toggle bold for custom format",
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            FilledTonalIconButton(
                                onClick = { onToggleUnderline(customInputValue.text) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("custom_underline_toggle"),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = if (isCustomUnderline) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    contentColor = if (isCustomUnderline) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FormatUnderlined,
                                    contentDescription = "Toggle underline for custom format",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        if (isCustomActive) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }

                        Icon(
                            imageVector = if (isCustomEditorExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isCustomEditorExpanded) "Collapse" else "Expand",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Expandable Custom Format Editor
            AnimatedVisibility(visible = isCustomEditorExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    // Live preview banner (renders B and U formatting immediately)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Preview: ",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            if (customInputValue.text.isBlank()) {
                                Text(
                                    text = "—",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            } else {
                                Text(
                                    text = customAnnotatedPreview,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Formatting Toolbar for Custom Format (B and U for selection or token)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalIconButton(
                            onClick = {
                                val (newText, newRange) = FormattingHelper.toggleTag(
                                    text = customInputValue.text,
                                    start = customInputValue.selection.start,
                                    end = customInputValue.selection.end,
                                    tag = "b"
                                )
                                customInputValue = TextFieldValue(
                                    text = newText,
                                    selection = TextRange(newRange.first, newRange.last)
                                )
                                onSaveCustom(newText)
                                onSelectFormat(newText)
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .testTag("custom_editor_bold_button"),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatBold,
                                contentDescription = "Bold selection or pattern",
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        FilledTonalIconButton(
                            onClick = {
                                val (newText, newRange) = FormattingHelper.toggleTag(
                                    text = customInputValue.text,
                                    start = customInputValue.selection.start,
                                    end = customInputValue.selection.end,
                                    tag = "u"
                                )
                                customInputValue = TextFieldValue(
                                    text = newText,
                                    selection = TextRange(newRange.first, newRange.last)
                                )
                                onSaveCustom(newText)
                                onSelectFormat(newText)
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .testTag("custom_editor_underline_button"),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatUnderlined,
                                contentDescription = "Underline selection or pattern",
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (customInputValue.selection.start != customInputValue.selection.end) "Styles selection" else "Styles whole format",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Input Field
                    OutlinedTextField(
                        value = customInputValue,
                        onValueChange = {
                            customInputValue = it
                            onSaveCustom(it.text)
                            onSelectFormat(it.text)
                        },
                        label = { Text("Custom Pattern") },
                        placeholder = { Text("e.g. $defaultFallback") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_format_input_${title.lowercase().replace(" ", "_")}")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Tappable tokens
                    Text(
                        text = "Tap tokens to insert:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tokens.forEach { token ->
                            val cleanToken = token.removeSurrounding("'")
                            AssistChip(
                                onClick = {
                                    val currentText = customInputValue.text
                                    val start = customInputValue.selection.start.coerceAtLeast(0)
                                    val end = customInputValue.selection.end.coerceAtLeast(0)
                                    val updated = if (start != end) {
                                        currentText.replaceRange(start, end, cleanToken)
                                    } else {
                                        val prefix = currentText.substring(0, start)
                                        val suffix = currentText.substring(start)
                                        "$prefix$cleanToken$suffix"
                                    }
                                    val newPos = (start + cleanToken.length).coerceAtMost(updated.length)
                                    customInputValue = TextFieldValue(updated, selection = TextRange(newPos))
                                    onSaveCustom(updated)
                                    onSelectFormat(updated)
                                },
                                label = {
                                    Text(
                                        text = token,
                                        fontFamily = FontFamily.Monospace,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Actions: Set as Default & Reset to Default
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                onSelectFormat(customInputValue.text)
                                onSaveCustom(customInputValue.text)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Use Custom")
                        }

                        OutlinedButton(
                            onClick = {
                                customInputValue = TextFieldValue(defaultFallback, selection = TextRange(defaultFallback.length))
                                onSaveCustom(defaultFallback)
                                onSelectFormat(defaultFallback)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restore,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset")
                        }
                    }
                }
            }
        }
    }
}

