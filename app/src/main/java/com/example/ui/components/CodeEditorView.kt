package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.editor.CodeSyntaxHighlighter
import com.example.model.EditorTab

@Composable
fun CodeEditorView(
    activeTab: EditorTab?,
    searchQuery: String,
    replaceQuery: String,
    isSearchBarVisible: Boolean,
    onContentChanged: (String) -> Unit,
    onSaveRequested: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onReplaceQueryChanged: (String) -> Unit,
    onPerformReplace: () -> Unit,
    onCloseSearchBar: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (activeTab == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "حدد ملفاً من القائمة الجانبية للبدء بالتحرير",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    val isDark = isSystemInDarkTheme()
    val language = remember(activeTab.file.name) {
        CodeSyntaxHighlighter.detectLanguage(activeTab.file.name)
    }

    var textFieldValue by remember(activeTab.file.path) {
        mutableStateOf(TextFieldValue(activeTab.content))
    }

    // Keep state in sync if external modification occurs
    LaunchedEffect(activeTab.content) {
        if (textFieldValue.text != activeTab.content) {
            textFieldValue = textFieldValue.copy(text = activeTab.content)
        }
    }

    // Undo / Redo stacks
    val undoStack = remember(activeTab.file.path) { mutableStateListOf<String>() }
    val redoStack = remember(activeTab.file.path) { mutableStateListOf<String>() }

    fun updateTextWithUndo(newText: String) {
        if (newText != textFieldValue.text) {
            undoStack.add(textFieldValue.text)
            redoStack.clear()
            textFieldValue = textFieldValue.copy(text = newText)
            onContentChanged(newText)
        }
    }

    val lines = remember(textFieldValue.text) { textFieldValue.text.lines() }
    val lineCount = remember(lines) { maxOf(1, lines.size) }

    val verticalScroll = rememberScrollState()
    val horizontalScroll = rememberScrollState()

    var showJumpToLineDialog by remember { mutableStateOf(false) }
    var jumpLineInput by remember { mutableStateOf("") }

    val syntaxTransformation = remember(language, isDark, searchQuery) {
        VisualTransformation { original ->
            val highlighted = CodeSyntaxHighlighter.highlightCode(
                code = original.text,
                language = language,
                isDark = isDark,
                searchQuery = searchQuery
            )
            TransformedText(highlighted, OffsetMapping.Identity)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) Color(0xFF1E1F22) else Color(0xFFFAFAFA))
    ) {
        // Search & Replace Bar
        if (isSearchBarVisible) {
            Surface(
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChanged,
                            label = { Text("بحث (Search)", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            textStyle = TextStyle(fontSize = 13.sp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(onClick = onCloseSearchBar, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close search")
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = replaceQuery,
                            onValueChange = onReplaceQueryChanged,
                            label = { Text("استبدال بـ (Replace with)", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            textStyle = TextStyle(fontSize = 13.sp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = onPerformReplace,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("استبدال الكل", fontSize = 12.sp)
                        }
                    }
                }
            }
            HorizontalDivider(thickness = 0.5.dp)
        }

        // Quick action sub-toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isDark) Color(0xFF2B2D30) else Color(0xFFEDEDED))
                .padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${language.name} | Lines: $lineCount",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        if (undoStack.isNotEmpty()) {
                            val previous = undoStack.removeLast()
                            redoStack.add(textFieldValue.text)
                            textFieldValue = textFieldValue.copy(text = previous)
                            onContentChanged(previous)
                        }
                    },
                    enabled = undoStack.isNotEmpty(),
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        Icons.Default.Undo,
                        contentDescription = "Undo",
                        modifier = Modifier.size(16.dp),
                        tint = if (undoStack.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }

                IconButton(
                    onClick = {
                        if (redoStack.isNotEmpty()) {
                            val next = redoStack.removeLast()
                            undoStack.add(textFieldValue.text)
                            textFieldValue = textFieldValue.copy(text = next)
                            onContentChanged(next)
                        }
                    },
                    enabled = redoStack.isNotEmpty(),
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        Icons.Default.Redo,
                        contentDescription = "Redo",
                        modifier = Modifier.size(16.dp),
                        tint = if (redoStack.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }

                IconButton(
                    onClick = { showJumpToLineDialog = true },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(Icons.Default.FormatListNumbered, contentDescription = "Jump to line", modifier = Modifier.size(16.dp))
                }
            }
        }

        // Editor Area: Line Numbers + Text Field
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // Line numbers column
            Column(
                modifier = Modifier
                    .verticalScroll(verticalScroll)
                    .background(if (isDark) Color(0xFF1E1F22) else Color(0xFFF0F0F0))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .widthIn(min = 36.dp),
                horizontalAlignment = Alignment.End
            ) {
                for (i in 1..lineCount) {
                    Text(
                        text = "$i",
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (isDark) Color(0xFF6E7681) else Color(0xFFAAAAAA)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(if (isDark) Color(0xFF32363A) else Color(0xFFE0E0E0))
            )

            // Text Field Area (with horizontal and vertical scrolling)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(verticalScroll)
                    .horizontalScroll(horizontalScroll)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                BasicTextField(
                    value = textFieldValue,
                    onValueChange = { newValue ->
                        if (newValue.text != textFieldValue.text) {
                            updateTextWithUndo(newValue.text)
                        } else {
                            textFieldValue = newValue
                        }
                    },
                    visualTransformation = syntaxTransformation,
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = if (isDark) Color(0xFFBCBEC4) else Color(0xFF1F2328)
                    ),
                    cursorBrush = SolidColor(if (isDark) Color(0xFF388E3C) else Color(0xFF0066CC)),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Mobile Developer Code Symbols Bar
        Surface(
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val symbols = listOf(
                    "{", "}", "(", ")", "[", "]", ";", ":", "\"", "'",
                    "=", "->", "+", "-", "<", ">", ".", ",", "!", "?",
                    "fun", "val", "var", "class", "override", "import"
                )

                symbols.forEach { sym ->
                    TextButton(
                        onClick = {
                            val cursor = textFieldValue.selection.start
                            val currentText = textFieldValue.text
                            val insert = if (sym.length > 2) "$sym " else sym
                            val newText = currentText.substring(0, cursor) + insert + currentText.substring(cursor)
                            val newCursor = cursor + insert.length
                            updateTextWithUndo(newText)
                            textFieldValue = textFieldValue.copy(
                                text = newText,
                                selection = TextRange(newCursor)
                            )
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = sym,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }

    // Jump to Line Dialog
    if (showJumpToLineDialog) {
        AlertDialog(
            onDismissRequest = { showJumpToLineDialog = false },
            title = { Text("الانتقال إلى سطر") },
            text = {
                OutlinedTextField(
                    value = jumpLineInput,
                    onValueChange = { jumpLineInput = it.filter { ch -> ch.isDigit() } },
                    label = { Text("رقم السطر (1 - $lineCount)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val targetLine = jumpLineInput.toIntOrNull()
                        if (targetLine != null && targetLine in 1..lineCount) {
                            // Calculate cursor index for line
                            var charIndex = 0
                            for (l in 0 until (targetLine - 1)) {
                                charIndex += lines[l].length + 1
                            }
                            textFieldValue = textFieldValue.copy(selection = TextRange(charIndex))
                            showJumpToLineDialog = false
                        }
                    }
                ) {
                    Text("انتقال")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJumpToLineDialog = false }) { Text("إلغاء") }
            }
        )
    }
}
