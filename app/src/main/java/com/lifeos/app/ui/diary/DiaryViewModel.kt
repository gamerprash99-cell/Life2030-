package com.lifeos.app.ui.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeos.app.core.intelligence.AnswerResult
import com.lifeos.app.core.intelligence.DiaryConnections
import com.lifeos.app.core.intelligence.DiaryGraph
import com.lifeos.app.core.intelligence.DiaryInsights
import com.lifeos.app.core.intelligence.LifeOSIntelligenceEngine
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.data.db.entities.DiaryEntity
import com.lifeos.app.data.repository.DiaryRepository
import com.lifeos.app.domain.usecase.BuildTimelineUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Diary screen state. Everything flows from `DiaryRepository.observeAll()` and,
 * for the "connections" radar, `BuildTimelineUseCase` — the same deterministic,
 * offline aggregation the Home timeline uses. The Analytics card is computed by
 * the existing LifeOS Intelligence Engine (`diaryInsights()`), never fabricated.
 */
class DiaryViewModel(
    private val diaryRepository: DiaryRepository,
    private val intelligenceEngine: LifeOSIntelligenceEngine,
    private val buildTimelineUseCase: BuildTimelineUseCase
) : ViewModel() {

    val entries: StateFlow<List<DiaryEntity>> = diaryRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedDay = MutableStateFlow<Long?>(null)
    val selectedDay: StateFlow<Long?> = _selectedDay

    val filteredEntries: StateFlow<List<DiaryEntity>> =
        combine(entries, _selectedDay) { all, day ->
            (if (day == null) all else all.filter { it.dateEpochDay == day })
                .sortedWith(compareByDescending<DiaryEntity> { it.dateEpochDay }.thenByDescending { it.timeMinutes })
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _insights = MutableStateFlow<DiaryInsights?>(null)
    val insights: StateFlow<DiaryInsights?> = _insights
    private val _insightsLoading = MutableStateFlow(true)
    val insightsLoading: StateFlow<Boolean> = _insightsLoading

    private val _connections = MutableStateFlow<DiaryGraph?>(null)
    val connections: StateFlow<DiaryGraph?> = _connections
    private val _connectionsLoading = MutableStateFlow(false)
    val connectionsLoading: StateFlow<Boolean> = _connectionsLoading
    private val _connectionsDay = MutableStateFlow<Long?>(null)
    val connectionsDay: StateFlow<Long?> = _connectionsDay

    private val _showEditor = MutableStateFlow(false)
    val showEditor: StateFlow<Boolean> = _showEditor
    private val _editingEntry = MutableStateFlow<DiaryEntity?>(null)
    val editingEntry: StateFlow<DiaryEntity?> = _editingEntry

    private val _entryToDelete = MutableStateFlow<DiaryEntity?>(null)
    val entryToDelete: StateFlow<DiaryEntity?> = _entryToDelete

    private val _question = MutableStateFlow<AnswerResult?>(null)
    val question: StateFlow<AnswerResult?> = _question
    private val _asking = MutableStateFlow(false)
    val asking: StateFlow<Boolean> = _asking

    init {
        loadInsights()
        observeConnections()
    }

    fun selectDay(day: Long?) {
        _selectedDay.value = day
    }

    fun startNewEntry() {
        _editingEntry.value = null
        _showEditor.value = true
    }

    fun startEdit(entry: DiaryEntity) {
        _editingEntry.value = entry
        _showEditor.value = true
    }

    fun dismissEditor() {
        _showEditor.value = false
        _editingEntry.value = null
    }

    fun saveEntry(content: String, mood: String?) {
        if (content.isBlank()) return
        viewModelScope.launch {
            val editing = _editingEntry.value
            if (editing != null) {
                diaryRepository.updateEntry(
                    editing.id, editing.title, content, mood, splitTags(editing.tagsCsv)
                )
            } else {
                val now = DateTimeUtils.today()
                val minutes = java.time.LocalTime.now().let { it.hour * 60 + it.minute }
                diaryRepository.createEntry(
                    title = null, content = content, mood = mood, tags = emptyList(),
                    dateEpochDay = now.toEpochDay(), timeMinutes = minutes
                )
            }
            dismissEditor()
        }
    }

    fun requestDelete(entry: DiaryEntity) {
        _entryToDelete.value = entry
    }

    fun dismissDelete() {
        _entryToDelete.value = null
    }

    fun deleteEntry(id: String) {
        _entryToDelete.value = null
        viewModelScope.launch { diaryRepository.delete(id) }
    }

    fun askDiary(questionText: String) {
        if (questionText.isBlank()) return
        viewModelScope.launch {
            _asking.value = true
            _question.value = try {
                intelligenceEngine.answerQuestion(questionText)
            } catch (_: Throwable) {
                AnswerResult("Couldn't answer that on-device right now. Try again in a moment.")
            }
            _asking.value = false
        }
    }

    private fun loadInsights() {
        viewModelScope.launch {
            _insightsLoading.value = true
            _insights.value = try {
                intelligenceEngine.diaryInsights()
            } catch (_: Throwable) {
                null
            }
            _insightsLoading.value = false
        }
    }

    /**
     * The connections radar reacts to the selected day (or, when "All" is
     * chosen, the most recent day that actually has an entry) and to every
     * repository change, so the graph is always derived from live data.
     */
    private fun observeConnections() {
        viewModelScope.launch {
            combine(entries, _selectedDay) { all, day -> (all to day) }.collect { (all, day) ->
                val focusDay = day ?: all.maxOfOrNull { it.dateEpochDay }
                    ?: DateTimeUtils.today().toEpochDay()
                _connectionsLoading.value = true
                _connectionsDay.value = focusDay
                val dayEntries = all.filter { it.dateEpochDay == focusDay }
                val timeline = runCatching { buildTimelineUseCase(focusDay) }.getOrDefault(emptyList())
                _connections.value = DiaryConnections.build(dayEntries, timeline)
                _connectionsLoading.value = false
            }
        }
    }

    private fun splitTags(csv: String): List<String> = csv.split(',')
        .map { it.trim() }
        .filter { it.isNotBlank() }
}