package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.AppDatabase
import com.example.data.repository.NoteRepository
import com.example.debug.InAppLogCollector
import com.example.debug.PerformanceMonitor
import com.example.ui.components.SettingsDialog
import com.example.ui.debug.DebugDashboardDialog
import com.example.ui.debug.PerformanceFloatingHud
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
      // Observa la configuración visual y tipografía persistida para propagarla a todo el árbol de Compose
      val selectedFont by viewModel.selectedFont.collectAsStateWithLifecycle()
      val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
      val dynamicColor by viewModel.dynamicColor.collectAsStateWithLifecycle()
      val accentPalette by viewModel.accentPalette.collectAsStateWithLifecycle()

      MyApplicationTheme(
        themeMode = themeMode,
        dynamicColor = dynamicColor,
        accentPalette = accentPalette,
        fontTheme = selectedFont
      ) {
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
  val scope = rememberCoroutineScope()
  val notes by viewModel.filteredNotes.collectAsStateWithLifecycle()
  val tags by viewModel.allTags.collectAsStateWithLifecycle()
  val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
  val selectedTag by viewModel.selectedTag.collectAsStateWithLifecycle()
  val activeNote by viewModel.activeNote.collectAsStateWithLifecycle()
  val editorMode by viewModel.editorMode.collectAsStateWithLifecycle()
  val isCompactView by viewModel.isCompactView.collectAsStateWithLifecycle()

  // Iniciar monitores de depuración in-app exclusivamente para variante debug (Canary)
  LaunchedEffect(Unit) {
    if (BuildConfig.DEBUG) {
      PerformanceMonitor.startMonitoring(scope)
      InAppLogCollector.startCollecting(scope)
    }
  }

  val metrics by PerformanceMonitor.metrics.collectAsStateWithLifecycle()
  val isHudVisible by PerformanceMonitor.isOverlayVisible.collectAsStateWithLifecycle()
  val logs by InAppLogCollector.logs.collectAsStateWithLifecycle()
  var showDebugDashboard by remember { mutableStateOf(false) }
  var showSettingsDialog by remember { mutableStateOf(false) }

  // Estados reactivos para el diálogo de ajustes
  val selectedFont by viewModel.selectedFont.collectAsStateWithLifecycle()
  val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
  val dynamicColor by viewModel.dynamicColor.collectAsStateWithLifecycle()
  val accentPalette by viewModel.accentPalette.collectAsStateWithLifecycle()
  val updateStatus by viewModel.updateStatus.collectAsStateWithLifecycle()
  val context = LocalContext.current

  Box(modifier = Modifier.fillMaxSize()) {
    // Transición fluida y reactiva (60 FPS) entre Lista y Editor aprovechando corrutinas en segundo plano
    AnimatedContent(
      targetState = (activeNote != null),
      transitionSpec = {
        if (targetState) {
          // Entrando al editor: deslizamiento suave hacia la izquierda + desvanecimiento (180ms)
          (slideInHorizontally(animationSpec = tween(180)) { fullWidth -> fullWidth / 4 } +
           fadeIn(animationSpec = tween(180)))
            .togetherWith(
              slideOutHorizontally(animationSpec = tween(160)) { fullWidth -> -fullWidth / 4 } +
              fadeOut(animationSpec = tween(160))
            )
        } else {
          // Regresando a la lista: deslizamiento hacia la derecha + desvanecimiento rápido (180ms)
          (slideInHorizontally(animationSpec = tween(180)) { fullWidth -> -fullWidth / 4 } +
           fadeIn(animationSpec = tween(180)))
            .togetherWith(
              slideOutHorizontally(animationSpec = tween(160)) { fullWidth -> fullWidth / 4 } +
              fadeOut(animationSpec = tween(160))
            )
        }
      },
      label = "ScreenTransition"
    ) { isEditing ->
      if (isEditing) {
        val currentActiveNote = activeNote
        if (currentActiveNote != null) {
          NoteEditorScreen(
            note = currentActiveNote,
            editorMode = editorMode,
            onTitleChange = viewModel::updateActiveNoteTitle,
            onContentChange = viewModel::updateActiveNoteContent,
            onIconChange = viewModel::updateActiveNoteIcon,
            onTagsChange = viewModel::updateActiveNoteTags,
            onTogglePin = viewModel::toggleActiveNotePin,
            onToggleTask = viewModel::toggleTaskAtLine,
            onModeChange = viewModel::setEditorMode,
            onNoteFontChange = viewModel::updateActiveNoteFontTheme,
            onDeleteNote = viewModel::deleteActiveNote,
            onExportMarkdown = { uri ->
              val success = viewModel.exportNoteToMarkdown(context, uri, currentActiveNote)
              Toast.makeText(
                context,
                if (success) context.getString(R.string.export_success) else context.getString(R.string.export_error),
                Toast.LENGTH_SHORT
              ).show()
            },
            onExportVaultZip = { uri ->
              val success = viewModel.exportNoteToVaultZip(context, uri, currentActiveNote)
              Toast.makeText(
                context,
                if (success) context.getString(R.string.export_success) else context.getString(R.string.export_error),
                Toast.LENGTH_SHORT
              ).show()
            },
            onEncryptNote = { password ->
              viewModel.encryptActiveNote(password)
              Toast.makeText(context, context.getString(R.string.toast_note_encrypted), Toast.LENGTH_SHORT).show()
            },
            onRemoveEncryption = {
              viewModel.removeActiveNoteEncryption()
              Toast.makeText(context, context.getString(R.string.toast_encryption_removed), Toast.LENGTH_SHORT).show()
            },
            onBackClick = viewModel::closeActiveNote
          )
        }
      } else {
        NotesListScreen(
          notes = notes,
          tags = tags,
          searchQuery = searchQuery,
          selectedTag = selectedTag,
          isCompactView = isCompactView,
          onSearchQueryChange = viewModel::onSearchQueryChange,
          onTagSelect = viewModel::onTagSelect,
          onToggleViewMode = viewModel::toggleViewMode,
          onNoteClick = viewModel::openNote,
          onUnlockNote = viewModel::unlockAndOpenNote,
          onCreateNoteClick = viewModel::createNewNote,
          onDeleteNote = viewModel::deleteNote,
          onOpenDebugDashboard = { showDebugDashboard = true },
          onOpenSettings = { showSettingsDialog = true },
          onImportNote = { uri ->
            viewModel.importNoteFromUri(context, uri) { _, message ->
              (context as? ComponentActivity)?.runOnUiThread {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
              }
            }
          }
        )
      }
    }

    // Diálogo de Ajustes de Apariencia y Actualizaciones (Canal Beta -b con descarga directa)
    if (showSettingsDialog) {
      SettingsDialog(
        themeMode = themeMode,
        dynamicColor = dynamicColor,
        accentPalette = accentPalette,
        fontTheme = selectedFont,
        onThemeModeChange = viewModel::setThemeMode,
        onDynamicColorChange = viewModel::setDynamicColor,
        onAccentPaletteChange = viewModel::setAccentPalette,
        onFontThemeChange = viewModel::setFontTheme,
        updateStatus = updateStatus,
        onCheckForUpdates = { viewModel.checkForUpdates(BuildConfig.VERSION_NAME) },
        onDownloadApk = { release -> viewModel.downloadApk(release) },
        onInstallApk = { apkFile -> viewModel.installApk(context, apkFile) },
        onResetUpdateStatus = viewModel::resetUpdateStatus,
        onDismiss = { showSettingsDialog = false }
      )
    }

    // HUD flotante de rendimiento en pantalla (FPS, RAM JVM y RAM Nativa de Rust/C++, Hilos) - Exclusivo Debug
    if (BuildConfig.DEBUG && isHudVisible) {
      PerformanceFloatingHud(
        metrics = metrics,
        onOpenFullDashboard = { showDebugDashboard = true },
        onClose = { PerformanceMonitor.setOverlayVisible(false) }
      )
    }

    // Diálogo con panel completo de depuración (Rendimiento, Hilos, Logs en vivo) - Exclusivo Debug
    if (BuildConfig.DEBUG && showDebugDashboard) {
      DebugDashboardDialog(
        metrics = metrics,
        logs = logs,
        onClose = { showDebugDashboard = false },
        onClearLogs = { InAppLogCollector.clear() }
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

