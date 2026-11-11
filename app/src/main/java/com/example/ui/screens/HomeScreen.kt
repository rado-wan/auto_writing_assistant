package com.example.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.ui.viewmodel.MainViewModel

sealed class AppDestination {
    data object Main : AppDestination()
    data object Formats : AppDestination()
    data object Settings : AppDestination()
    data class SnippetEditor(val snippetId: Long) : AppDestination()
}

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var currentDestination by remember { mutableStateOf<AppDestination>(AppDestination.Main) }

    when (val destination = currentDestination) {
        is AppDestination.Main -> {
            MainScreen(
                viewModel = viewModel,
                onNavigateToFormats = { currentDestination = AppDestination.Formats },
                onNavigateToSettings = { currentDestination = AppDestination.Settings },
                onNavigateToEditor = { snippetId ->
                    currentDestination = AppDestination.SnippetEditor(snippetId)
                },
                modifier = modifier
            )
        }
        is AppDestination.Formats -> {
            FormatScreen(
                viewModel = viewModel,
                onBack = { currentDestination = AppDestination.Main },
                modifier = modifier
            )
        }
        is AppDestination.Settings -> {
            SettingsScreen(
                viewModel = viewModel,
                onBack = { currentDestination = AppDestination.Main },
                modifier = modifier
            )
        }
        is AppDestination.SnippetEditor -> {
            SnippetEditorScreen(
                snippetId = destination.snippetId,
                viewModel = viewModel,
                onBack = { currentDestination = AppDestination.Main },
                modifier = modifier
            )
        }
    }
}
