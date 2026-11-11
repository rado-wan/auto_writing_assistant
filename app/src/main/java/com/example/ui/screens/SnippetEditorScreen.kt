package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.text.HtmlCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.MainViewModel
import com.example.util.FormattingHelper
import com.example.util.VariableParser
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnippetEditorScreen(
    snippetId: Long,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val coroutineScope = rememberCoroutineScope()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val allSnippets by viewModel.filteredSavedTexts.collectAsStateWithLifecycle()

    var currentId by remember { mutableLongStateOf(snippetId) }
    var title by remember { mutableStateOf("") }
    var bodyValue by remember { mutableStateOf(TextFieldValue("")) }
    var isFormattingEnabled by remember { mutableStateOf(false) }
    var isInitialized by remember { mutableStateOf(false) }
    var showVariableSheet by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var lastSavedText by remember { mutableStateOf("Saved") }

    // Initialize from existing snippet if editing
    LaunchedEffect(snippetId, allSnippets) {
        if (!isInitialized) {
            if (snippetId > 0) {
                val existing = allSnippets.find { it.id == snippetId }
                if (existing != null) {
                    title = existing.title
                    bodyValue = TextFieldValue(existing.body, selection = TextRange(existing.body.length))
                    isFormattingEnabled = existing.isFormattingEnabled
                }
            }
            isInitialized = true
        }
    }

    // Auto-save logic with debounce
    LaunchedEffect(title, bodyValue.text, isFormattingEnabled, isInitialized) {
        if (isInitialized && (title.isNotBlank() || bodyValue.text.isNotBlank())) {
            isSaving = true
            delay(400) // 400ms debounce
            val effectiveTitle = if (title.isBlank()) "Untitled Snippet" else title.trim()
            val newId = viewModel.saveSnippet(
                title = effectiveTitle,
                body = bodyValue.text,
                isFormattingEnabled = isFormattingEnabled,
                id = currentId
            )
            currentId = newId
            isSaving = false
            lastSavedText = "Saved"
        }
    }

    val livePreviewText by remember(bodyValue.text, settings) {
        derivedStateOf {
            VariableParser.parse(
                template = bodyValue.text,
                defaultDateFormat = settings.defaultDateFormat,
                defaultDayFormat = settings.defaultDayFormat,
                defaultTimeFormat = settings.defaultTimeFormat,
                dateOptions = settings.getDateFormatOptions(),
                dayOptions = settings.getDayFormatOptions(),
                timeOptions = settings.getTimeFormatOptions(),
                calendar = Calendar.getInstance(),
                boldPresets = settings.formatBoldPresets,
                underlinePresets = settings.formatUnderlinePresets
            ).text
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (snippetId > 0) "Edit Snippet" else "New Snippet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isSaving) {
                                Text(
                                    text = "Saving...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = lastSavedText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("snippet_editor_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to list"
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
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Title Input
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                placeholder = { Text("e.g. Email Sign-off, Meeting Template") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("snippet_title_input")
            )

            // Formatting Toggle Card
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Formatting",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Apply bold and underline to text",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isFormattingEnabled,
                            onCheckedChange = { isFormattingEnabled = it },
                            modifier = Modifier.testTag("snippet_formatting_switch")
                        )
                    }

                    AnimatedVisibility(visible = isFormattingEnabled) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            // Formatting Toolbar (B and U)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Bold Toggle Button (B)
                                FilledTonalIconButton(
                                    onClick = {
                                        val (newText, newRange) = FormattingHelper.toggleTag(
                                            text = bodyValue.text,
                                            start = bodyValue.selection.start,
                                            end = bodyValue.selection.end,
                                            tag = "b"
                                        )
                                        bodyValue = TextFieldValue(
                                            text = newText,
                                            selection = TextRange(newRange.first, newRange.last)
                                        )
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("format_bold_button"),
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FormatBold,
                                        contentDescription = "Bold",
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                // Underline Toggle Button (U)
                                FilledTonalIconButton(
                                    onClick = {
                                        val (newText, newRange) = FormattingHelper.toggleTag(
                                            text = bodyValue.text,
                                            start = bodyValue.selection.start,
                                            end = bodyValue.selection.end,
                                            tag = "u"
                                        )
                                        bodyValue = TextFieldValue(
                                            text = newText,
                                            selection = TextRange(newRange.first, newRange.last)
                                        )
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("format_underline_button"),
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FormatUnderlined,
                                        contentDescription = "Underline",
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                Text(
                                    text = if (bodyValue.selection.start != bodyValue.selection.end) "Applies to selection" else "Applies to whole text",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Some apps ignore formatting.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }

            // Body Input
            OutlinedTextField(
                value = bodyValue,
                onValueChange = { bodyValue = it },
                label = { Text("Template Body") },
                placeholder = { Text("Write your snippet here. Use the button below to insert variables.") },
                minLines = 6,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("snippet_body_input")
            )

            // Insert Variable Button
            OutlinedButton(
                onClick = { showVariableSheet = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("insert_variable_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Insert variable")
            }

            // Live Preview Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isFormattingEnabled) "Live Preview (Formatted)" else "Live Preview (Plain Text)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (livePreviewText.isBlank()) {
                        Text(
                            text = "Preview will appear here as you type...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    } else if (isFormattingEnabled || livePreviewText.contains("<b>") || livePreviewText.contains("<u>")) {
                        val annotated = remember(livePreviewText) {
                            FormattingHelper.renderHtmlToAnnotatedString(livePreviewText)
                        }
                        Text(
                            text = annotated,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else {
                        Text(
                            text = livePreviewText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Bottom Sheet for Variables
    if (showVariableSheet) {
        ModalBottomSheet(
            onDismissRequest = { showVariableSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DataObject,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Insert Variable",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Variables automatically resolve to live values when pasted into any app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(VariableParser.AVAILABLE_VARIABLES, key = { it.token }) { varInfo ->
                        val preview = remember(varInfo.token, settings) {
                            VariableParser.parse(
                                template = varInfo.token,
                                defaultDateFormat = settings.defaultDateFormat,
                                defaultDayFormat = settings.defaultDayFormat,
                                defaultTimeFormat = settings.defaultTimeFormat,
                                dateOptions = settings.getDateFormatOptions(),
                                dayOptions = settings.getDayFormatOptions(),
                                timeOptions = settings.getTimeFormatOptions(),
                                calendar = Calendar.getInstance(),
                                boldPresets = settings.formatBoldPresets,
                                underlinePresets = settings.formatUnderlinePresets
                            ).text
                        }

                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val currentText = bodyValue.text
                                    val start = bodyValue.selection.start.coerceAtLeast(0)
                                    val end = bodyValue.selection.end.coerceAtLeast(0)
                                    val newText = if (start != end) {
                                        currentText.replaceRange(start, end, varInfo.token)
                                    } else {
                                        val prefix = currentText.substring(0, start)
                                        val suffix = currentText.substring(start)
                                        val space = if (prefix.isNotEmpty() && !prefix.endsWith(" ")) " " else ""
                                        "$prefix$space${varInfo.token}$suffix"
                                    }
                                    val newCursorPos = (start + varInfo.token.length + (if (currentText.isNotEmpty() && !currentText.endsWith(" ")) 1 else 0))
                                        .coerceAtMost(newText.length)
                                    bodyValue = TextFieldValue(
                                        text = newText,
                                        selection = TextRange(newCursorPos)
                                    )

                                    coroutineScope.launch {
                                        sheetState.hide()
                                        showVariableSheet = false
                                    }
                                },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                text = varInfo.token,
                                                fontFamily = FontFamily.Monospace,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = varInfo.label,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = varInfo.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                ) {
                                    Text(
                                        text = preview,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Converts simple HTML string with <b> and <u> into Compose AnnotatedString.
 */
private fun renderHtmlToAnnotatedString(html: String): AnnotatedString {
    return try {
        val spanned = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT)
        val text = spanned.toString().trimEnd('\n')
        val builder = AnnotatedString.Builder(text)

        val boldSpans = spanned.getSpans(0, spanned.length, android.text.style.StyleSpan::class.java)
        for (span in boldSpans) {
            if (span.style == android.graphics.Typeface.BOLD) {
                val start = spanned.getSpanStart(span).coerceIn(0, text.length)
                val end = spanned.getSpanEnd(span).coerceIn(0, text.length)
                if (start < end) {
                    builder.addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, end)
                }
            }
        }

        val underlineSpans = spanned.getSpans(0, spanned.length, android.text.style.UnderlineSpan::class.java)
        for (span in underlineSpans) {
            val start = spanned.getSpanStart(span).coerceIn(0, text.length)
            val end = spanned.getSpanEnd(span).coerceIn(0, text.length)
            if (start < end) {
                builder.addStyle(SpanStyle(textDecoration = TextDecoration.Underline), start, end)
            }
        }

        builder.toAnnotatedString()
    } catch (_: Exception) {
        AnnotatedString(FormattingHelper.stripHtml(html))
    }
}
