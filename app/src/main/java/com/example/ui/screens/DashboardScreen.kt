package com.example.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.Project
import com.example.model.TemplateType
import com.example.ui.components.NewProjectDialog
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    projects: List<Project>,
    customApiKey: String,
    onOpenProject: (Project) -> Unit,
    onCreateProject: (name: String, packageName: String, template: TemplateType, minSdk: Int) -> Unit,
    onDeleteProject: (Project) -> Unit,
    onExportProjectZip: (Project) -> Unit,
    onImportProjectZip: (Uri, String) -> Unit,
    onSaveCustomApiKey: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showNewProjectDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var apiKeyInput by remember { mutableStateOf(customApiKey) }

    val zipPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onImportProjectZip(uri, "ImportedApp_${System.currentTimeMillis() % 1000}")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.DeveloperMode,
                            contentDescription = null,
                            tint = Color(0xFF388E3C),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Android Studio Mobile", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        apiKeyInput = customApiKey
                        showSettingsDialog = true
                    }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showNewProjectDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("مشروع جديد") }
            )
        },
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Hero Welcome Card
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF1E1F22),
                                modifier = Modifier.size(54.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Terminal,
                                        contentDescription = null,
                                        tint = Color(0xFF388E3C),
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Android Studio Mobile IDE",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "بيئة تطوير متكاملة لتحرير الأكواد وبناء حزم APK على هاتفك",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = { showNewProjectDialog = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("مشروع جديد", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = { zipPickerLauncher.launch(arrayOf("application/zip", "application/octet-stream")) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("استيراد ZIP", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Recent Projects Header
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "المشاريع الأخيرة (${projects.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (projects.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.FolderSpecial, contentDescription = null, tint = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("لا توجد مشاريع سابقة", style = MaterialTheme.typography.bodyMedium)
                            Text("اضغط على 'مشروع جديد' للبدء في كتابة تطبيقك الأول!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                items(projects) { project ->
                    ProjectCard(
                        project = project,
                        onClick = { onOpenProject(project) },
                        onExportZip = { onExportProjectZip(project) },
                        onDelete = { onDeleteProject(project) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(64.dp))
            }
        }
    }

    if (showNewProjectDialog) {
        NewProjectDialog(
            onDismiss = { showNewProjectDialog = false },
            onCreateProject = { name, pkg, template, minSdk ->
                showNewProjectDialog = false
                onCreateProject(name, pkg, template, minSdk)
            }
        )
    }

    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("إعدادات بيئة التطوير") },
            text = {
                Column {
                    Text(
                        "مفتاح Gemini API (اختياري للذكاء الاصطناعي):",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        placeholder = { Text("AIzaSy...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "يمكنك أيضاً ضبط المفتاح في لوحة أسرار AI Studio (BuildConfig.GEMINI_API_KEY). عند ترك الحقل فارغاً، سيعمل المساعد في الوضع المحلي بدون إنترنت.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onSaveCustomApiKey(apiKeyInput.trim())
                    showSettingsDialog = false
                }) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun ProjectCard(
    project: Project,
    onClick: () -> Unit,
    onExportZip: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val dateStr = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(project.lastModifiedAt))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = when (project.templateType) {
                    TemplateType.COMPOSE -> Color(0xFF7F52FF).copy(alpha = 0.2f)
                    TemplateType.XML_VIEW -> Color(0xFF00B4D8).copy(alpha = 0.2f)
                    TemplateType.JAVA_XML -> Color(0xFFE76F00).copy(alpha = 0.2f)
                    TemplateType.EMPTY -> MaterialTheme.colorScheme.surfaceVariant
                },
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when (project.templateType) {
                            TemplateType.COMPOSE -> Icons.Default.Layers
                            TemplateType.XML_VIEW -> Icons.Default.ViewQuilt
                            TemplateType.JAVA_XML -> Icons.Default.Coffee
                            TemplateType.EMPTY -> Icons.Default.Folder
                        },
                        contentDescription = null,
                        tint = when (project.templateType) {
                            TemplateType.COMPOSE -> Color(0xFF7F52FF)
                            TemplateType.XML_VIEW -> Color(0xFF00B4D8)
                            TemplateType.JAVA_XML -> Color(0xFFE76F00)
                            TemplateType.EMPTY -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = project.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    SuggestionChip(
                        onClick = {},
                        label = { Text(project.templateType.displayName.split(" ").first(), fontSize = 10.sp) },
                        modifier = Modifier.height(20.dp)
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = project.packageName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "آخر تعديل: $dateStr",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Project options")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("فتح المشروع (Open)") },
                        leadingIcon = { Icon(Icons.Default.Launch, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("تصدير كملف ZIP") },
                        leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onExportZip()
                        }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("حذف المشروع (Delete)", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}
