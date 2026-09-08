package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.AppDatabase
import com.example.data.repository.NoteRepository
import com.example.ui.screens.NoteEditorScreen
import com.example.ui.screens.NotesListScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.NotesViewModel

class MainActivity : ComponentActivity() {

  private val viewModel: NotesViewModel by viewModels {
    val database = AppDatabase.getDatabase(applicationContext)
    val repository = NoteRepository(database.noteDao())
    NotesViewModel.Factory(repository, applicationContext)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      // Observa la tipografía persistida en vivo para propagarla a todo el árbol de Compose
      val selectedFont by viewModel.selectedFont.collectAsStateWithLifecycle()

      MyApplicationTheme(fontTheme = selectedFont) {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          VaultNotesApp(viewModel = viewModel)
        }
      }
    }
  }
}

@Composable
fun VaultNotesApp(viewModel: NotesViewModel) {
  val notes by viewModel.filteredNotes.collectAsStateWithLifecycle()
  val tags by viewModel.allTags.collectAsStateWithLifecycle()
  val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
  val selectedTag by viewModel.selectedTag.collectAsStateWithLifecycle()
  val activeNote by viewModel.activeNote.collectAsStateWithLifecycle()
  val editorMode by viewModel.editorMode.collectAsStateWithLifecycle()
  val isCompactView by viewModel.isCompactView.collectAsStateWithLifecycle()
  val selectedFont by viewModel.selectedFont.collectAsStateWithLifecycle()

  AnimatedContent(
    targetState = activeNote,
    transitionSpec = { fadeIn() togetherWith fadeOut() },
    label = "ScreenTransition"
  ) { currentActiveNote ->
    if (currentActiveNote != null) {
      NoteEditorScreen(
        note = currentActiveNote,
        editorMode = editorMode,
        selectedFont = selectedFont,
        onTitleChange = viewModel::updateActiveNoteTitle,
        onContentChange = viewModel::updateActiveNoteContent,
        onIconChange = viewModel::updateActiveNoteIcon,
        onTagsChange = viewModel::updateActiveNoteTags,
        onTogglePin = viewModel::toggleActiveNotePin,
        onToggleTask = viewModel::toggleTaskAtLine,
        onModeChange = viewModel::setEditorMode,
        onFontSelected = viewModel::setFontTheme,
        onDeleteNote = viewModel::deleteActiveNote,
        onBackClick = viewModel::closeActiveNote
      )
    } else {
      NotesListScreen(
        notes = notes,
        tags = tags,
        searchQuery = searchQuery,
        selectedTag = selectedTag,
        isCompactView = isCompactView,
        selectedFont = selectedFont,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onTagSelect = viewModel::onTagSelect,
        onToggleViewMode = viewModel::toggleViewMode,
        onNoteClick = viewModel::openNote,
        onCreateNoteClick = viewModel::createNewNote,
        onDeleteNote = viewModel::deleteNote,
        onFontSelected = viewModel::setFontTheme
      )
    }
  }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
  Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
  MyApplicationTheme { Greeting("Android") }
}

