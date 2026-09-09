package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import com.example.ui.components.EncryptNoteDialog
import com.example.ui.components.RemoveEncryptionDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.R
import com.example.data.model.Note
import com.example.ui.components.EmojiPickerDialog
import com.example.ui.components.FontSelectionDialog
import com.example.ui.components.MarkdownToolbar
import com.example.ui.markdown.MarkdownPreview
import com.example.ui.theme.AppFontTheme
import com.example.ui.viewmodel.EditorMode
import androidx.compose.material.icons.filled.FormatSize
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun NoteEditorScreen(
    note: Note,
    editorMode: EditorMode,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onIconChange: (String) -> Unit,
    onTagsChange: (String) -> Unit,
    onTogglePin: () -> Unit,
    onToggleTask: (lineIndex: Int) -> Unit,
    onModeChange: (EditorMode) -> Unit,
    onNoteFontChange: (String) -> Unit = {},
    onDeleteNote: () -> Unit,
    onExportMarkdown: (Uri) -> Unit = {},
    onExportVaultZip: (Uri) -> Unit = {},
    onEncryptNote: (password: String) -> Unit = {},
    onRemoveEncryption: () -> Unit = {},
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val noteFont = remember(note.fontTheme) { AppFontTheme.fromId(note.fontTheme) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showTagsEditor by remember { mutableStateOf(false) }
    var showFontDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showEncryptDialog by remember { mutableStateOf(false) }
    var showRemoveEncryptionDialog by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }

    val exportMarkdownLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri ->
        if (uri != null) onExportMarkdown(uri)
    }

    val exportVaultZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) onExportVaultZip(uri)
    }

    var isContentFocused by remember { mutableStateOf(false) }
    val isImeVisible = WindowInsets.isImeVisible
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Cierre inmediato y fluido: oculta el teclado virtual y libera el foco antes de disparar la transición
    val handleExit = {
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
        onBackClick()
    }

    // Intercept hardware or gesture back button to close active note gracefully
    BackHandler {
        handleExit()
    }

    // Keep track of text field value with cursor selection for toolbar insertions
    var contentFieldValue by remember(note.id) {
        mutableStateOf(TextFieldValue(note.content, TextRange(note.content.length)))
    }

    // Sync content if changed externally (e.g., from toggling tasks in preview)
    if (contentFieldValue.text != note.content) {
        contentFieldValue = contentFieldValue.copy(text = note.content)
    }

    // Auto-scroll towards cursor when typing, changing selection, or opening keyboard
    LaunchedEffect(contentFieldValue.selection) {
        if (isContentFocused) {
            bringIntoViewRequester.bringIntoView()
        }
    }

    LaunchedEffect(isImeVisible) {
        if (isImeVisible && isContentFocused) {
            delay(150)
            bringIntoViewRequester.bringIntoView()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = handleExit,
                        modifier = Modifier.testTag("editor_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                title = {
                    // Selector de modo Editar vs Vista Previa adaptable y con espaciado óptimo
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .wrapContentWidth()
                            .height(36.dp)
                    ) {
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            onClick = { onModeChange(EditorMode.EDIT) },
                            selected = editorMode == EditorMode.EDIT,
                            icon = {},
                            modifier = Modifier.testTag("mode_edit_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.edit_mode),
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            onClick = { onModeChange(EditorMode.PREVIEW) },
                            selected = editorMode == EditorMode.PREVIEW,
                            icon = {},
                            modifier = Modifier.testTag("mode_preview_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.preview_mode),
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                },
                actions = {
                    // 1. Candado de cifrado / seguridad
                    IconButton(
                        onClick = {
                            if (note.isEncrypted) {
                                showRemoveEncryptionDialog = true
                            } else {
                                showEncryptDialog = true
                            }
                        },
                        modifier = Modifier.testTag("toggle_encryption_button")
                    ) {
                        Icon(
                            imageVector = if (note.isEncrypted) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = if (note.isEncrypted) stringResource(R.string.remove_encryption_title) else stringResource(R.string.encrypt_note_title),
                            tint = if (note.isEncrypted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // 2. Fijar / Desfijar nota
                    IconButton(
                        onClick = onTogglePin,
                        modifier = Modifier.testTag("toggle_pin_button")
                    ) {
                        Icon(
                            imageVector = if (note.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (note.isPinned) "Desfijar" else "Fijar",
                            tint = if (note.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // 3. Menú de más opciones para evitar saturar la barra en pantallas móviles
                    Box {
                        IconButton(
                            onClick = { showOptionsMenu = true },
                            modifier = Modifier.testTag("editor_more_options_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.more_options),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        DropdownMenu(
                            expanded = showOptionsMenu,
                            onDismissRequest = { showOptionsMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.select_typography)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.FormatSize,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showOptionsMenu = false
                                    showFontDialog = true
                                },
                                modifier = Modifier.testTag("editor_select_font_button")
                            )

                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.export_note)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.FileDownload,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                onClick = {
                                    showOptionsMenu = false
                                    showExportDialog = true
                                },
                                modifier = Modifier.testTag("export_note_button")
                            )

                            HorizontalDivider()

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(R.string.delete_note),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    showOptionsMenu = false
                                    showDeleteDialog = true
                                },
                                modifier = Modifier.testTag("delete_note_button")
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            // Only show Markdown toolbar when actively writing in EDIT mode (cursor/keyboard active)
            val showToolbar = editorMode == EditorMode.EDIT && (isContentFocused || isImeVisible)
            AnimatedVisibility(
                visible = showToolbar,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it }
            ) {
                MarkdownToolbar(
                    onInsertText = { prefix, suffix ->
                        val text = contentFieldValue.text
                        val selection = contentFieldValue.selection
                        val start = selection.start
                        val end = selection.end
                        val selectedText = text.substring(start, end)

                        val newText = text.substring(0, start) + prefix + selectedText + suffix + text.substring(end)
                        val newCursorPos = start + prefix.length + selectedText.length + suffix.length

                        contentFieldValue = TextFieldValue(
                            text = newText,
                            selection = TextRange(newCursorPos)
                        )
                        onContentChange(newText)
                        coroutineScope.launch {
                            bringIntoViewRequester.bringIntoView()
                        }
                    },
                    onOpenFontDialog = { showFontDialog = true },
                    modifier = Modifier.testTag("editor_markdown_toolbar")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Note Header: Icon + Title + Metadata
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                if (note.isEncrypted) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .testTag("encrypted_note_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.encrypted_badge),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Notion-style Emoji Picker Trigger
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                            .clickable { showEmojiPicker = true }
                            .testTag("emoji_picker_trigger"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = note.icon, fontSize = 24.sp)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Title Input
                    BasicTextField(
                        value = note.title,
                        onValueChange = onTitleChange,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("note_title_input"),
                        textStyle = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { innerTextField ->
                            if (note.title.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.note_title_placeholder),
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                )
                            }
                            innerTextField()
                        }
                    )

                    // Tag editor toggle button
                    IconButton(
                        onClick = { showTagsEditor = !showTagsEditor },
                        modifier = Modifier.size(36.dp).testTag("toggle_tags_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Label,
                            contentDescription = "Etiquetas",
                            tint = if (note.tags.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Expandable Tags input
                AnimatedVisibility(visible = showTagsEditor) {
                    OutlinedTextField(
                        value = note.tags,
                        onValueChange = onTagsChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .testTag("note_tags_input"),
                        label = { Text("Etiquetas (separadas por coma)") },
                        placeholder = { Text("ej: ideas, trabajo, personal") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                        )
                    )
                }

                // Metadata details
                val formattedTime = remember(note.updatedAt) {
                    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                    sdf.format(Date(note.updatedAt))
                }
                val wordCount = remember(contentFieldValue.text) {
                    var count = 0
                    var inWord = false
                    val text = contentFieldValue.text
                    for (i in 0 until text.length) {
                        if (text[i].isWhitespace()) {
                            inWord = false
                        } else if (!inWord) {
                            inWord = true
                            count++
                        }
                    }
                    count
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Editado $formattedTime • $wordCount palabras • ${contentFieldValue.text.length} caracteres",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            // Content: Edit mode vs Preview mode
            when (editorMode) {
                EditorMode.EDIT -> {
                    TextField(
                        value = contentFieldValue,
                        onValueChange = { newValue ->
                            val textChanged = contentFieldValue.text != newValue.text
                            contentFieldValue = newValue
                            if (textChanged) {
                                onContentChange(newValue.text)
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp)
                            .bringIntoViewRequester(bringIntoViewRequester)
                            .onFocusChanged { focusState ->
                                isContentFocused = focusState.isFocused
                                if (focusState.isFocused) {
                                    coroutineScope.launch {
                                        delay(200)
                                        bringIntoViewRequester.bringIntoView()
                                    }
                                }
                            }
                            .testTag("note_content_input"),
                        placeholder = {
                            Text(
                                text = stringResource(R.string.note_content_placeholder),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                fontSize = 15.sp,
                                lineHeight = 22.sp
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            fontFamily = noteFont.fontFamily,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                EditorMode.PREVIEW -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag("markdown_preview_container")
                    ) {
                        MarkdownPreview(
                            content = note.content,
                            baseFontTheme = noteFont,
                            onToggleTask = onToggleTask
                        )
                        Spacer(modifier = Modifier.height(48.dp))
                    }
                }
            }
        }
    }

    // Emoji Picker Dialog
    if (showEmojiPicker) {
        EmojiPickerDialog(
            currentEmoji = note.icon,
            onEmojiSelected = onIconChange,
            onDismissRequest = { showEmojiPicker = false }
        )
    }

    // Delete Note Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = { Text(stringResource(R.string.delete_confirm_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Se eliminará permanentemente \"${note.title.ifBlank { "Nota sin título" }}\".",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.delete_confirm_message),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteNote()
                    },
                    modifier = Modifier.testTag("confirm_delete_note")
                ) {
                    Text(
                        text = stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Diálogo para seleccionar tipografía (Toda la nota vs Texto seleccionado)
    if (showFontDialog) {
        val selection = contentFieldValue.selection
        val hasSelection = selection.start != selection.end
        val selectedSnippet = if (hasSelection) {
            val s = selection.start
            val e = selection.end
            val sub = contentFieldValue.text.substring(s, e)
            if (sub.length > 25) sub.take(25) + "…" else sub
        } else ""

        FontSelectionDialog(
            currentFont = noteFont,
            hasSelection = hasSelection,
            selectedTextSnippet = selectedSnippet,
            onApplyToWholeNote = { newFont ->
                onNoteFontChange(newFont.id)
            },
            onApplyToSelection = { newFont ->
                val text = contentFieldValue.text
                val sel = contentFieldValue.selection
                val start = sel.start
                val end = sel.end
                val prefix = "[font:${newFont.id}]"
                val suffix = "[/font]"
                val textToWrap = if (start != end) text.substring(start, end) else "texto"
                val newText = text.substring(0, start) + prefix + textToWrap + suffix + text.substring(end)
                val newCursorPos = start + prefix.length + textToWrap.length + suffix.length

                contentFieldValue = TextFieldValue(
                    text = newText,
                    selection = TextRange(newCursorPos)
                )
                onContentChange(newText)
                coroutineScope.launch {
                    bringIntoViewRequester.bringIntoView()
                }
            },
            onDismissRequest = { showFontDialog = false }
        )
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.export_note),
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Opción 1: Markdown (.md)
                    Surface(
                        onClick = {
                            showExportDialog = false
                            val cleanName = note.title.ifBlank { "nota" }
                                .replace("[^a-zA-Z0-9_\\-]".toRegex(), "_")
                            exportMarkdownLauncher.launch("$cleanName.md")
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_option_markdown")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "📄", fontSize = 24.sp, modifier = Modifier.padding(end = 12.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.export_as_markdown),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = stringResource(R.string.export_as_markdown_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Opción 2: Paquete Vault ZIP firmado
                    Surface(
                        onClick = {
                            showExportDialog = false
                            val cleanName = note.title.ifBlank { "nota" }
                                .replace("[^a-zA-Z0-9_\\-]".toRegex(), "_")
                            exportVaultZipLauncher.launch("${cleanName}_vault.zip")
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_option_vault_zip")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "🛡️", fontSize = 24.sp, modifier = Modifier.padding(end = 12.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.export_as_vault_zip),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = stringResource(R.string.export_as_vault_zip_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showEncryptDialog) {
        EncryptNoteDialog(
            onDismiss = { showEncryptDialog = false },
            onConfirm = { password ->
                showEncryptDialog = false
                onEncryptNote(password)
            }
        )
    }

    if (showRemoveEncryptionDialog) {
        RemoveEncryptionDialog(
            onDismiss = { showRemoveEncryptionDialog = false },
            onConfirm = {
                showRemoveEncryptionDialog = false
                onRemoveEncryption()
            }
        )
    }
}
