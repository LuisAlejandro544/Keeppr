package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Note
import com.example.data.repository.NoteRepository
import com.example.ui.markdown.MarkdownParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class EditorMode {
    EDIT,
    PREVIEW
}

class NotesViewModel(private val repository: NoteRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag: StateFlow<String?> = _selectedTag.asStateFlow()

    private val _activeNote = MutableStateFlow<Note?>(null)
    val activeNote: StateFlow<Note?> = _activeNote.asStateFlow()

    private val _editorMode = MutableStateFlow(EditorMode.EDIT)
    val editorMode: StateFlow<EditorMode> = _editorMode.asStateFlow()

    private val _isCompactView = MutableStateFlow(false)
    val isCompactView: StateFlow<Boolean> = _isCompactView.asStateFlow()

    // All tags aggregated from all notes
    val allTags: StateFlow<List<String>> = repository.allNotes
        .combine(_selectedTag) { notes, _ ->
            notes.flatMap { it.tagList }.distinct().sorted()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered notes list
    val filteredNotes: StateFlow<List<Note>> = combine(
        repository.allNotes,
        _searchQuery,
        _selectedTag
    ) { notes, query, tag ->
        notes.filter { note ->
            val matchesQuery = query.isBlank() ||
                note.title.contains(query, ignoreCase = true) ||
                note.content.contains(query, ignoreCase = true) ||
                note.tags.contains(query, ignoreCase = true)

            val matchesTag = tag == null || note.tagList.any { it.equals(tag, ignoreCase = true) }

            matchesQuery && matchesTag
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onTagSelect(tag: String?) {
        _selectedTag.value = if (_selectedTag.value == tag) null else tag
    }

    fun toggleViewMode() {
        _isCompactView.value = !_isCompactView.value
    }

    fun openNote(note: Note, initialMode: EditorMode = EditorMode.EDIT) {
        _activeNote.value = note
        _editorMode.value = initialMode
    }

    fun createNewNote() {
        val newNote = Note(
            title = "",
            content = "",
            icon = "📝",
            tags = "",
            isPinned = false
        )
        viewModelScope.launch {
            val generatedId = repository.insert(newNote)
            val insertedNote = newNote.copy(id = generatedId)
            _activeNote.value = insertedNote
            _editorMode.value = EditorMode.EDIT
        }
    }

    fun setEditorMode(mode: EditorMode) {
        _editorMode.value = mode
    }

    fun updateActiveNoteTitle(newTitle: String) {
        _activeNote.value?.let { current ->
            val updated = current.copy(title = newTitle, updatedAt = System.currentTimeMillis())
            _activeNote.value = updated
            saveNoteAsync(updated)
        }
    }

    fun updateActiveNoteContent(newContent: String) {
        _activeNote.value?.let { current ->
            val updated = current.copy(content = newContent, updatedAt = System.currentTimeMillis())
            _activeNote.value = updated
            saveNoteAsync(updated)
        }
    }

    fun updateActiveNoteIcon(newIcon: String) {
        _activeNote.value?.let { current ->
            val updated = current.copy(icon = newIcon, updatedAt = System.currentTimeMillis())
            _activeNote.value = updated
            saveNoteAsync(updated)
        }
    }

    fun updateActiveNoteTags(newTags: String) {
        _activeNote.value?.let { current ->
            val updated = current.copy(tags = newTags, updatedAt = System.currentTimeMillis())
            _activeNote.value = updated
            saveNoteAsync(updated)
        }
    }

    fun toggleActiveNotePin() {
        _activeNote.value?.let { current ->
            val updated = current.copy(isPinned = !current.isPinned, updatedAt = System.currentTimeMillis())
            _activeNote.value = updated
            saveNoteAsync(updated)
        }
    }

    fun toggleTaskAtLine(lineIndex: Int) {
        _activeNote.value?.let { current ->
            val newContent = MarkdownParser.toggleChecklistAt(current.content, lineIndex)
            val updated = current.copy(content = newContent, updatedAt = System.currentTimeMillis())
            _activeNote.value = updated
            saveNoteAsync(updated)
        }
    }

    fun deleteActiveNote() {
        _activeNote.value?.let { current ->
            viewModelScope.launch {
                repository.delete(current)
                _activeNote.value = null
            }
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            repository.delete(note)
            if (_activeNote.value?.id == note.id) {
                _activeNote.value = null
            }
        }
    }

    fun closeActiveNote() {
        _activeNote.value?.let { current ->
            saveNoteAsync(current)
        }
        _activeNote.value = null
    }

    private fun saveNoteAsync(note: Note) {
        viewModelScope.launch {
            repository.update(note)
        }
    }

    class Factory(private val repository: NoteRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(NotesViewModel::class.java)) {
                return NotesViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
