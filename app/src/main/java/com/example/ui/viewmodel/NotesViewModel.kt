package com.example.ui.viewmodel

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Note
import com.example.data.repository.NoteRepository
import com.example.native.NativeEngine
import com.example.ui.markdown.MarkdownParser
import com.example.ui.theme.AppFontTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class EditorMode {
    EDIT,
    PREVIEW
}

class NotesViewModel(
    private val repository: NoteRepository,
    context: Context? = null
) : ViewModel() {

    private val prefs: SharedPreferences? =
        context?.applicationContext?.getSharedPreferences("vaultnotes_prefs", Context.MODE_PRIVATE)

    // Estado reactivo para la tipografía seleccionada (1 de 5 disponibles)
    private val _selectedFont = MutableStateFlow(
        AppFontTheme.fromId(prefs?.getString("selected_font_theme", AppFontTheme.DEFAULT.id))
    )
    val selectedFont: StateFlow<AppFontTheme> = _selectedFont.asStateFlow()

    /**
     * Actualiza la tipografía activa de la aplicación y la almacena de forma persistente
     * en SharedPreferences para que sobreviva a reinicios sin requerir internet.
     */
    fun setFontTheme(fontTheme: AppFontTheme) {
        _selectedFont.value = fontTheme
        prefs?.edit()?.putString("selected_font_theme", fontTheme.id)?.apply()
    }

    private var saveJob: Job? = null

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

    // All tags aggregated from all notes on background thread
    val allTags: StateFlow<List<String>> = repository.allNotes
        .combine(_selectedTag) { notes, _ ->
            notes.flatMap { it.tagList }.distinct().sorted()
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Lista reactiva de notas filtradas, calculada en un hilo de fondo (Dispatchers.Default)
     * utilizando el motor de búsqueda nativo en Rust para acelerar coincidencias en títulos,
     * contenido y etiquetas sin bloquear la interfaz de usuario.
     */
    val filteredNotes: StateFlow<List<Note>> = combine(
        repository.allNotes,
        _searchQuery,
        _selectedTag
    ) { notes, query, tag ->
        val q = query.trim()
        if (q.isBlank() && tag == null) {
            notes
        } else {
            notes.filter { note ->
                // Motor nativo de Rust acelerando la búsqueda insensible a mayúsculas
                val matchesQuery = q.isBlank() || NativeEngine.matchNote(
                    query = q,
                    title = note.title,
                    content = note.content,
                    tags = note.tags
                )

                val matchesTag = tag == null || note.tagList.any { it.equals(tag, ignoreCase = true) }

                matchesQuery && matchesTag
            }
        }
    }
    .flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
            fontTheme = "default",
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

    fun updateActiveNoteFontTheme(fontThemeId: String) {
        _activeNote.value?.let { current ->
            val updated = current.copy(fontTheme = fontThemeId, updatedAt = System.currentTimeMillis())
            _activeNote.value = updated
            saveNoteAsync(updated, debounce = false)
        }
    }

    fun updateActiveNoteTitle(newTitle: String) {
        _activeNote.value?.let { current ->
            val updated = current.copy(title = newTitle, updatedAt = System.currentTimeMillis())
            _activeNote.value = updated
            saveNoteAsync(updated, debounce = true)
        }
    }

    fun updateActiveNoteContent(newContent: String) {
        _activeNote.value?.let { current ->
            val updated = current.copy(content = newContent, updatedAt = System.currentTimeMillis())
            _activeNote.value = updated
            saveNoteAsync(updated, debounce = true)
        }
    }

    fun updateActiveNoteIcon(newIcon: String) {
        _activeNote.value?.let { current ->
            val updated = current.copy(icon = newIcon, updatedAt = System.currentTimeMillis())
            _activeNote.value = updated
            saveNoteAsync(updated, debounce = false)
        }
    }

    fun updateActiveNoteTags(newTags: String) {
        _activeNote.value?.let { current ->
            val updated = current.copy(tags = newTags, updatedAt = System.currentTimeMillis())
            _activeNote.value = updated
            saveNoteAsync(updated, debounce = true)
        }
    }

    fun toggleActiveNotePin() {
        _activeNote.value?.let { current ->
            val updated = current.copy(isPinned = !current.isPinned, updatedAt = System.currentTimeMillis())
            _activeNote.value = updated
            saveNoteAsync(updated, debounce = false)
        }
    }

    fun toggleTaskAtLine(lineIndex: Int) {
        _activeNote.value?.let { current ->
            val newContent = MarkdownParser.toggleChecklistAt(current.content, lineIndex)
            val updated = current.copy(content = newContent, updatedAt = System.currentTimeMillis())
            _activeNote.value = updated
            saveNoteAsync(updated, debounce = false)
        }
    }

    fun deleteActiveNote() {
        _activeNote.value?.let { current ->
            saveJob?.cancel()
            viewModelScope.launch(Dispatchers.IO) {
                repository.delete(current)
                _activeNote.value = null
            }
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(note)
            if (_activeNote.value?.id == note.id) {
                _activeNote.value = null
            }
        }
    }

    fun closeActiveNote() {
        _activeNote.value?.let { current ->
            saveJob?.cancel()
            viewModelScope.launch(Dispatchers.IO) {
                repository.update(current)
            }
        }
        _activeNote.value = null
    }

    private fun saveNoteAsync(note: Note, debounce: Boolean = true) {
        saveJob?.cancel()
        if (debounce) {
            saveJob = viewModelScope.launch(Dispatchers.IO) {
                delay(500L)
                repository.update(note)
            }
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                repository.update(note)
            }
        }
    }

    class Factory(
        private val repository: NoteRepository,
        private val context: Context? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(NotesViewModel::class.java)) {
                return NotesViewModel(repository, context) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
