package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.Note
import com.example.ui.components.EmojiPickerDialog
import com.example.ui.components.MarkdownToolbar
import com.example.ui.markdown.MarkdownPreview
import com.example.ui.viewmodel.EditorMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
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
    onDeleteNote: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showEmojiPicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showTagsEditor by remember { mutableStateOf(false) }

    // Intercept hardware or gesture back button to close active note gracefully
    BackHandler {
        onBackClick()
    }

    // Keep track of text field value with cursor selection for toolbar insertions
    var contentFieldValue by remember(note.id) {
        mutableStateOf(TextFieldValue(note.content, TextRange(note.content.length)))
    }

    // Sync content if changed externally (e.g., from toggling tasks in preview)
    if (contentFieldValue.text != note.content) {
        contentFieldValue = contentFieldValue.copy(text = note.content)
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("editor_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                title = {
                    // Segmented Button: Edit vs Preview
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(38.dp)
                    ) {
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            onClick = { onModeChange(EditorMode.EDIT) },
                            selected = editorMode == EditorMode.EDIT,
                            icon = {},
                            modifier = Modifier.testTag("mode_edit_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.edit_mode),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.preview_mode),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                },
                actions = {
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

                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.testTag("delete_note_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete_note),
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            // Only show Markdown toolbar in EDIT mode
            if (editorMode == EditorMode.EDIT) {
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
                    },
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
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Editado $formattedTime • ${note.wordCount} palabras • ${note.characterCount} caracteres",
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
                            contentFieldValue = newValue
                            onContentChange(newValue.text)
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp)
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
                            fontFamily = FontFamily.Default,
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
            title = { Text(stringResource(R.string.delete_confirm_title)) },
            text = { Text(stringResource(R.string.delete_confirm_message)) },
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
}
