package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FileNode
import java.io.File

@Composable
fun FileTreeView(
    rootNode: FileNode?,
    selectedFile: File?,
    onFileSelected: (File) -> Unit,
    onToggleExpand: (String) -> Unit,
    onCreateFile: (parent: File, name: String) -> Unit,
    onCreateFolder: (parent: File, name: String) -> Unit,
    onDeleteFile: (File) -> Unit,
    onRenameFile: (File, newName: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var contextMenuTarget by remember { mutableStateOf<File?>(null) }
    var showNewFileDialog by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var inputName by remember { mutableStateOf("") }

    if (rootNode == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("لا توجد ملفات مفتوحة", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val flattenedList = remember(rootNode) {
        val list = mutableListOf<Pair<FileNode, Int>>()
        fun flatten(node: FileNode, depth: Int) {
            list.add(node to depth)
            if (node.isDirectory && node.isExpanded) {
                node.children.forEach { flatten(it, depth + 1) }
            }
        }
        rootNode.children.forEach { flatten(it, 0) }
        list
    }

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        // Tree Header
        Surface(
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = "Project Files",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = rootNode.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row {
                    IconButton(
                        onClick = {
                            contextMenuTarget = rootNode.file
                            inputName = ""
                            showNewFileDialog = true
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "New File", modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                        onClick = {
                            contextMenuTarget = rootNode.file
                            inputName = ""
                            showNewFolderDialog = true
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = "New Folder", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(flattenedList) { (node, depth) ->
                val isSelected = selectedFile?.absolutePath == node.file.absolutePath

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            else Color.Transparent
                        )
                        .clickable {
                            if (node.isDirectory) {
                                onToggleExpand(node.path)
                            } else {
                                onFileSelected(node.file)
                            }
                        }
                        .padding(vertical = 4.dp, horizontal = 4.dp)
                        .padding(start = (depth * 14).dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (node.isDirectory) {
                        Icon(
                            imageVector = if (node.isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                            contentDescription = if (node.isExpanded) "Collapse" else "Expand",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = if (node.isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder,
                            contentDescription = "Folder",
                            tint = Color(0xFFFFB74D),
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.width(18.dp))
                        Icon(
                            imageVector = getFileIcon(node.extension),
                            contentDescription = "File",
                            tint = getFileColor(node.extension),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = node.name,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Context Menu button
                    IconButton(
                        onClick = {
                            contextMenuTarget = node.file
                            inputName = node.name
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "File Options",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Context Menu Dropdown / Dialog
    if (contextMenuTarget != null && !showNewFileDialog && !showNewFolderDialog && !showRenameDialog && !showDeleteConfirmDialog) {
        val target = contextMenuTarget!!
        AlertDialog(
            onDismissRequest = { contextMenuTarget = null },
            title = { Text(target.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            text = {
                Column {
                    if (target.isDirectory) {
                        TextButton(
                            onClick = {
                                inputName = ""
                                showNewFileDialog = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, null)
                            Spacer(Modifier.width(8.dp))
                            Text("ملف جديد هنا (New File)")
                        }
                        TextButton(
                            onClick = {
                                inputName = ""
                                showNewFolderDialog = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.CreateNewFolder, null)
                            Spacer(Modifier.width(8.dp))
                            Text("مجلد جديد (New Folder)")
                        }
                    }
                    TextButton(
                        onClick = {
                            inputName = target.name
                            showRenameDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Edit, null)
                        Spacer(Modifier.width(8.dp))
                        Text("إعادة تسمية (Rename)")
                    }
                    TextButton(
                        onClick = { showDeleteConfirmDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, null)
                        Spacer(Modifier.width(8.dp))
                        Text("حذف (Delete)")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { contextMenuTarget = null }) {
                    Text("إغلاق")
                }
            }
        )
    }

    // New File Dialog
    if (showNewFileDialog && contextMenuTarget != null) {
        AlertDialog(
            onDismissRequest = { showNewFileDialog = false },
            title = { Text("إنشاء ملف جديد") },
            text = {
                OutlinedTextField(
                    value = inputName,
                    onValueChange = { inputName = it },
                    label = { Text("اسم الملف (مثال: CustomView.kt)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputName.isNotBlank()) {
                            val parent = if (contextMenuTarget!!.isDirectory) contextMenuTarget!! else contextMenuTarget!!.parentFile ?: contextMenuTarget!!
                            onCreateFile(parent, inputName.trim())
                            showNewFileDialog = false
                            contextMenuTarget = null
                        }
                    }
                ) {
                    Text("إنشاء")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFileDialog = false }) { Text("إلغاء") }
            }
        )
    }

    // New Folder Dialog
    if (showNewFolderDialog && contextMenuTarget != null) {
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text("إنشاء مجلد جديد") },
            text = {
                OutlinedTextField(
                    value = inputName,
                    onValueChange = { inputName = it },
                    label = { Text("اسم المجلد") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputName.isNotBlank()) {
                            val parent = if (contextMenuTarget!!.isDirectory) contextMenuTarget!! else contextMenuTarget!!.parentFile ?: contextMenuTarget!!
                            onCreateFolder(parent, inputName.trim())
                            showNewFolderDialog = false
                            contextMenuTarget = null
                        }
                    }
                ) {
                    Text("إنشاء")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) { Text("إلغاء") }
            }
        )
    }

    // Rename Dialog
    if (showRenameDialog && contextMenuTarget != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("إعادة التسمية") },
            text = {
                OutlinedTextField(
                    value = inputName,
                    onValueChange = { inputName = it },
                    label = { Text("الاسم الجديد") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputName.isNotBlank()) {
                            onRenameFile(contextMenuTarget!!, inputName.trim())
                            showRenameDialog = false
                            contextMenuTarget = null
                        }
                    }
                ) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("إلغاء") }
            }
        )
    }

    // Delete Confirm Dialog
    if (showDeleteConfirmDialog && contextMenuTarget != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("تأكيد الحذف") },
            text = { Text("هل أنت متأكد من رغبتك في حذف '${contextMenuTarget!!.name}'؟ لا يمكن التراجع عن هذا الإجراء.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteFile(contextMenuTarget!!)
                        showDeleteConfirmDialog = false
                        contextMenuTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف نهائي")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("إلغاء") }
            }
        )
    }
}

private fun getFileIcon(extension: String): ImageVector {
    return when (extension) {
        "kt", "kts" -> Icons.Default.Code
        "java" -> Icons.Default.IntegrationInstructions
        "xml" -> Icons.Default.DataObject
        "gradle" -> Icons.Default.Build
        "json" -> Icons.Default.Description
        "md", "markdown" -> Icons.Default.Article
        "png", "jpg", "jpeg", "webp" -> Icons.Default.Image
        else -> Icons.Default.InsertDriveFile
    }
}

private fun getFileColor(extension: String): Color {
    return when (extension) {
        "kt", "kts" -> Color(0xFF7F52FF) // Kotlin purple
        "java" -> Color(0xFFE76F00) // Java orange
        "xml" -> Color(0xFF00B4D8) // XML Cyan
        "gradle" -> Color(0xFF02303A) // Gradle Dark
        "json" -> Color(0xFFF4A261)
        "md" -> Color(0xFF457B9D)
        else -> Color(0xFF9E9E9E)
    }
}
