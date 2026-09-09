package com.example.ui.viewmodel

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Note
import com.example.data.repository.NoteRepository
import com.example.data.util.VaultPackageHelper
import com.example.native.NativeEngine
import com.example.ui.markdown.MarkdownParser
import com.example.ui.theme.AppAccentPalette
import com.example.ui.theme.AppFontTheme
import com.example.ui.theme.AppThemeMode
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

    // Estado reactivo para el modo de tema (Sistema, Claro, Oscuro)
    private val _themeMode = MutableStateFlow(
        AppThemeMode.fromId(prefs?.getString("selected_theme_mode", AppThemeMode.SYSTEM.id))
    )
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    // Estado reactivo para Material You (activado por defecto)
    private val _dynamicColor = MutableStateFlow(
        prefs?.getBoolean("use_dynamic_color", true) ?: true
    )
    val dynamicColor: StateFlow<Boolean> = _dynamicColor.asStateFlow()

    // Estado reactivo para la paleta de acento (cuando dynamicColor está desactivado)
    private val _accentPalette = MutableStateFlow(
        AppAccentPalette.fromId(prefs?.getString("selected_accent_palette", AppAccentPalette.PURPLE.id))
    )
    val accentPalette: StateFlow<AppAccentPalette> = _accentPalette.asStateFlow()

    /**
     * Actualiza la tipografía activa de la aplicación y la almacena de forma persistente
     * en SharedPreferences para que sobreviva a reinicios sin requerir internet.
     */
    fun setFontTheme(fontTheme: AppFontTheme) {
        _selectedFont.value = fontTheme
        prefs?.edit()?.putString("selected_font_theme", fontTheme.id)?.apply()
    }

    /**
     * Actualiza el modo de tema visual (Sistema, Claro u Oscuro).
     */
    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
        prefs?.edit()?.putString("selected_theme_mode", mode.id)?.apply()
    }

    /**
     * Activa o desactiva la extracción de colores de Material You.
     */
    fun setDynamicColor(enabled: Boolean) {
        _dynamicColor.value = enabled
        prefs?.edit()?.putBoolean("use_dynamic_color", enabled)?.apply()
    }

    /**
     * Selecciona la paleta de acento cuando no se usa Material You.
     */
    fun setAccentPalette(palette: AppAccentPalette) {
        _accentPalette.value = palette
        prefs?.edit()?.putString("selected_accent_palette", palette.id)?.apply()
    }

    private var saveJob: Job? = null

    // Contraseña en memoria para la sesión activa de la nota abierta
    private var activeNoteSessionPassword: String? = null

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
                // Si la nota está cifrada, comparamos solo en título y etiquetas para no comparar con el payload cifrado
                val matchesQuery = q.isBlank() || if (note.isEncrypted) {
                    NativeEngine.matchNote(
                        query = q,
                        title = note.title,
                        content = "",
                        tags = note.tags
                    )
                } else {
                    NativeEngine.matchNote(
                        query = q,
                        title = note.title,
                        content = note.content,
                        tags = note.tags
                    )
                }

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
        activeNoteSessionPassword = null
        _activeNote.value = note
        _editorMode.value = initialMode
    }

    /**
     * Intenta desbloquear una nota protegida con la contraseña provista.
     * Retorna true si la contraseña es correcta y la abre en el editor, o false en caso de error.
     */
    fun unlockAndOpenNote(note: Note, password: String): Boolean {
        val decrypted = NativeEngine.decryptNote(note.content, password)
        return if (decrypted != null) {
            activeNoteSessionPassword = password
            _activeNote.value = note.copy(content = decrypted)
            _editorMode.value = EditorMode.EDIT
            true
        } else {
            false
        }
    }

    /**
     * Protege y cifra la nota actualmente activa con una nueva contraseña.
     */
    fun encryptActiveNote(password: String) {
        val current = _activeNote.value ?: return
        activeNoteSessionPassword = password
        val updated = current.copy(isEncrypted = true, updatedAt = System.currentTimeMillis())
        _activeNote.value = updated
        saveNoteAsync(updated, debounce = false)
    }

    /**
     * Remueve la protección por contraseña de la nota activa y la guarda en texto plano.
     */
    fun removeActiveNoteEncryption() {
        val current = _activeNote.value ?: return
        activeNoteSessionPassword = null
        val updated = current.copy(isEncrypted = false, updatedAt = System.currentTimeMillis())
        _activeNote.value = updated
        saveNoteAsync(updated, debounce = false)
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

    /**
     * Cierra la nota activa de forma completamente no bloqueante.
     * Cancela tareas diferidas pendientes y persiste en Dispatchers.IO liberando
     * el estado activo al instante para que la animación de regreso comience en el frame 0.
     */
    fun closeActiveNote() {
        val current = _activeNote.value
        val sessionPassword = activeNoteSessionPassword
        _activeNote.value = null
        activeNoteSessionPassword = null
        if (current != null) {
            saveJob?.cancel()
            viewModelScope.launch(Dispatchers.IO) {
                val toPersist = if (current.isEncrypted && !sessionPassword.isNullOrEmpty()) {
                    val encryptedPayload = NativeEngine.encryptNote(current.content, sessionPassword)
                    current.copy(content = encryptedPayload)
                } else {
                    current
                }
                repository.update(toPersist)
            }
        }
    }

    private fun saveNoteAsync(note: Note, debounce: Boolean = true) {
        saveJob?.cancel()
        val sessionPassword = activeNoteSessionPassword
        val prepareNoteForPersistence = {
            if (note.isEncrypted && !sessionPassword.isNullOrEmpty()) {
                val encrypted = NativeEngine.encryptNote(note.content, sessionPassword)
                note.copy(content = encrypted)
            } else {
                note
            }
        }

        if (debounce) {
            saveJob = viewModelScope.launch(Dispatchers.IO) {
                delay(500L)
                repository.update(prepareNoteForPersistence())
            }
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                repository.update(prepareNoteForPersistence())
            }
        }
    }

    /**
     * Importa una nota a partir de un archivo .md, .txt o paquete .zip de VaultNotes.
     * La verificación criptográfica se realiza de forma transparente en el motor Rust.
     */
    fun importNoteFromUri(context: Context, uri: Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = VaultPackageHelper.importFromUri(context, uri)) {
                is VaultPackageHelper.ImportResult.Success -> {
                    val newId = repository.insert(result.note)
                    onResult(true, result.message)
                }
                is VaultPackageHelper.ImportResult.Error -> {
                    onResult(false, result.error)
                }
            }
        }
    }

    /**
     * Exporta una nota a formato Markdown plano (.md) universal.
     */
    fun exportNoteToMarkdown(context: Context, uri: Uri, note: Note): Boolean {
        return VaultPackageHelper.exportMarkdown(context, uri, note)
    }

    /**
     * Exporta una nota a un paquete seguro .zip firmado con SHA-256 en Rust.
     */
    fun exportNoteToVaultZip(context: Context, uri: Uri, note: Note): Boolean {
        return VaultPackageHelper.exportVaultZip(context, uri, note)
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
