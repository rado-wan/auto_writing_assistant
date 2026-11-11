package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.SavedText
import com.example.data.preferences.AppPreferences
import com.example.data.preferences.AppSettings
import com.example.data.repository.SavedTextRepository
import com.example.service.AccessibilityUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = SavedTextRepository(database.savedTextDao())
    private val preferences = AppPreferences.getInstance(application)

    val settings: StateFlow<AppSettings> = preferences.settingsState

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _isServiceEnabled = MutableStateFlow(false)
    val isServiceEnabled = _isServiceEnabled.asStateFlow()

    val filteredSavedTexts: StateFlow<List<SavedText>> = combine(
        repository.allTextsFlow,
        _searchQuery
    ) { texts, query ->
        if (query.isBlank()) {
            texts
        } else {
            texts.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.body.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        checkServiceStatus()
    }

    fun checkServiceStatus() {
        _isServiceEnabled.value = AccessibilityUtils.isAccessibilityServiceEnabled(getApplication())
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    suspend fun saveSnippet(title: String, body: String, isFormattingEnabled: Boolean = false, id: Long): Long {
        return withContext(Dispatchers.IO) {
            if (id > 0) {
                val current = database.savedTextDao().getById(id)
                if (current != null) {
                    repository.update(current.copy(title = title, body = body, isFormattingEnabled = isFormattingEnabled))
                    id
                } else {
                    val count = database.savedTextDao().getCount()
                    repository.insert(SavedText(title = title, body = body, isFormattingEnabled = isFormattingEnabled, orderIndex = count))
                }
            } else {
                val count = database.savedTextDao().getCount()
                repository.insert(SavedText(title = title, body = body, isFormattingEnabled = isFormattingEnabled, orderIndex = count))
            }
        }
    }

    fun deleteText(item: SavedText) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(item)
        }
    }

    fun restoreText(item: SavedText) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insert(item)
        }
    }

    fun moveItemUp(item: SavedText) {
        viewModelScope.launch(Dispatchers.IO) {
            val list = repository.getAll().toMutableList()
            val index = list.indexOfFirst { it.id == item.id }
            if (index > 0) {
                val temp = list[index - 1]
                list[index - 1] = list[index]
                list[index] = temp
                repository.reorder(list)
            }
        }
    }

    fun moveItemDown(item: SavedText) {
        viewModelScope.launch(Dispatchers.IO) {
            val list = repository.getAll().toMutableList()
            val index = list.indexOfFirst { it.id == item.id }
            if (index in 0 until list.size - 1) {
                val temp = list[index + 1]
                list[index + 1] = list[index]
                list[index] = temp
                repository.reorder(list)
            }
        }
    }

    fun moveToTop(item: SavedText) {
        viewModelScope.launch(Dispatchers.IO) {
            val list = repository.getAll().toMutableList()
            val index = list.indexOfFirst { it.id == item.id }
            if (index > 0) {
                val removed = list.removeAt(index)
                list.add(0, removed)
                repository.reorder(list)
            }
        }
    }

    fun moveToBottom(item: SavedText) {
        viewModelScope.launch(Dispatchers.IO) {
            val list = repository.getAll().toMutableList()
            val index = list.indexOfFirst { it.id == item.id }
            if (index in 0 until list.size - 1) {
                val removed = list.removeAt(index)
                list.add(removed)
                repository.reorder(list)
            }
        }
    }

    fun sortAlphabetical(ascending: Boolean = true) {
        viewModelScope.launch(Dispatchers.IO) {
            val list = repository.getAll()
            val sorted = if (ascending) {
                list.sortedBy { it.title.lowercase() }
            } else {
                list.sortedByDescending { it.title.lowercase() }
            }
            repository.reorder(sorted)
        }
    }

    fun sortByDate(newestFirst: Boolean = true) {
        viewModelScope.launch(Dispatchers.IO) {
            val list = repository.getAll()
            val sorted = if (newestFirst) {
                list.sortedWith(compareByDescending<SavedText> { it.createdAt }.thenByDescending { it.id })
            } else {
                list.sortedWith(compareBy<SavedText> { it.createdAt }.thenBy { it.id })
            }
            repository.reorder(sorted)
        }
    }

    fun exportSavedTextsJson(): String {
        val list = filteredSavedTexts.value
        return repository.exportToJson(list)
    }

    suspend fun importSavedTextsJson(jsonString: String, replace: Boolean): Result<Int> {
        return withContext(Dispatchers.IO) {
            repository.importFromJson(jsonString, replace)
        }
    }

    // Settings actions
    fun setMasterEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { preferences.setMasterEnabled(enabled) }
    }

    fun setButtonPositionRatioY(ratio: Float) {
        viewModelScope.launch(Dispatchers.IO) { preferences.setButtonPositionRatioY(ratio.coerceIn(0f, 1f)) }
    }

    fun setButtonSize(sizeDp: Int) {
        viewModelScope.launch(Dispatchers.IO) { preferences.setButtonSize(sizeDp) }
    }

    fun setButtonOpacity(opacity: Float) {
        viewModelScope.launch(Dispatchers.IO) { preferences.setButtonOpacity(opacity) }
    }

    fun setButtonSide(side: String) {
        viewModelScope.launch(Dispatchers.IO) { preferences.setButtonSide(side) }
    }

    fun setDefaultDateFormat(format: String) {
        viewModelScope.launch(Dispatchers.IO) { preferences.setDefaultDateFormat(format) }
    }

    fun setDefaultDayFormat(format: String) {
        viewModelScope.launch(Dispatchers.IO) { preferences.setDefaultDayFormat(format) }
    }

    fun setDefaultTimeFormat(format: String) {
        viewModelScope.launch(Dispatchers.IO) { preferences.setDefaultTimeFormat(format) }
    }

    fun setCustomDateFormat(format: String) {
        viewModelScope.launch(Dispatchers.IO) { preferences.setCustomDateFormat(format) }
    }

    fun setCustomDayFormat(format: String) {
        viewModelScope.launch(Dispatchers.IO) { preferences.setCustomDayFormat(format) }
    }

    fun setCustomTimeFormat(format: String) {
        viewModelScope.launch(Dispatchers.IO) { preferences.setCustomTimeFormat(format) }
    }

    fun setIsBangla(isBangla: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { preferences.setIsBangla(isBangla) }
    }

    fun setSavedTextsCollapsed(collapsed: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { preferences.setSavedTextsCollapsed(collapsed) }
    }

    fun setMenuMaxHeightPercent(percent: Int) {
        viewModelScope.launch(Dispatchers.IO) { preferences.setMenuMaxHeightPercent(percent) }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch(Dispatchers.IO) { preferences.setThemeMode(mode) }
    }

    fun setFormattingStyle(style: String) {
        viewModelScope.launch(Dispatchers.IO) { preferences.setFormattingStyle(style) }
    }

    fun togglePresetBold(pattern: String) {
        viewModelScope.launch(Dispatchers.IO) { preferences.togglePresetBold(pattern) }
    }

    fun togglePresetUnderline(pattern: String) {
        viewModelScope.launch(Dispatchers.IO) { preferences.togglePresetUnderline(pattern) }
    }

    fun setStartOnBoot(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { preferences.setStartOnBoot(enabled) }
    }
}
