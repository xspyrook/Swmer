package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.AiMessage
import com.example.model.*
import com.example.viewmodel.BottomPanelTab
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BottomPanel(
    selectedTab: BottomPanelTab,
    isExpanded: Boolean,
    buildState: BuildState,
    terminalHistory: List<TerminalEntry>,
    diagnostics: List<DiagnosticIssue>,
    projectDependencies: List<DependencyItem>,
    popularLibraries: List<DependencyItem>,
    searchResults: List<DependencyItem>,
    isSearchingDeps: Boolean,
    aiMessages: List<AiMessage>,
    isAiLoading: Boolean,
    apks: List<ApkItem>,
    sdkInfo: SdkInfo,
    onTabSelected: (BottomPanelTab) -> Unit,
    onToggleExpand: () -> Unit,
    onCancelBuild: () -> Unit,
    onInstallBuiltApk: () -> Unit,
    onShareBuiltApk: () -> Unit,
    onSendTerminalCommand: (String) -> Unit,
    onOpenFile: (File) -> Unit,
    onAddDependency: (DependencyItem) -> Unit,
    onRemoveDependency: (DependencyItem) -> Unit,
    onSearchDependencies: (String) -> Unit,
    onSendAiPrompt: (String) -> Unit,
    onApplyAiCode: (String) -> Unit,
    onInstallApk: (File) -> Unit,
    onShareApk: (File) -> Unit,
    onDeleteApk: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    var terminalInput by remember { mutableStateOf("") }
    var depSearchInput by remember { mutableStateOf("") }
    var aiInput by remember { mutableStateOf("") }

    val errorCount = remember(diagnostics) {
        diagnostics.count { it.severity == DiagnosticSeverity.ERROR }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Tab Header Bar
        Surface(
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BottomPanelTab.values().forEach { tab ->
                        val isSelected = tab == selectedTab
                        FilterChip(
                            selected = isSelected,
                            onClick = { onTabSelected(tab) },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(tab.titleAr, fontSize = 12.sp)
                                    if (tab == BottomPanelTab.DIAGNOSTICS && errorCount > 0) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Badge(containerColor = MaterialTheme.colorScheme.error) {
                                            Text("$errorCount", color = Color.White, fontSize = 10.sp)
                                        }
                                    }
                                    if (tab == BottomPanelTab.BUILD_LOG && buildState.status == BuildStatus.RUNNING) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        CircularProgressIndicator(modifier = Modifier.size(10.dp), strokeWidth = 1.5.dp)
                                    }
                                }
                            },
                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 2.dp)
                        )
                    }
                }

                IconButton(onClick = onToggleExpand, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                        contentDescription = if (isExpanded) "Collapse Panel" else "Expand Panel"
                    )
                }
            }
        }

        HorizontalDivider(thickness = 0.5.dp)

        if (isExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                when (selectedTab) {
                    BottomPanelTab.BUILD_LOG -> {
                        BuildLogView(
                            buildState = buildState,
                            onCancelBuild = onCancelBuild,
                            onInstallBuiltApk = onInstallBuiltApk,
                            onShareBuiltApk = onShareBuiltApk
                        )
                    }
                    BottomPanelTab.TERMINAL -> {
                        TerminalView(
                            history = terminalHistory,
                            input = terminalInput,
                            onInputChange = { terminalInput = it },
                            onSendCommand = {
                                if (terminalInput.isNotBlank()) {
                                    onSendTerminalCommand(terminalInput.trim())
                                    terminalInput = ""
                                }
                            }
                        )
                    }
                    BottomPanelTab.DIAGNOSTICS -> {
                        DiagnosticsView(
                            issues = diagnostics,
                            onIssueClick = { onOpenFile(it.file) }
                        )
                    }
                    BottomPanelTab.DEPENDENCIES -> {
                        DependenciesView(
                            declared = projectDependencies,
                            popular = popularLibraries,
                            searchResults = searchResults,
                            isSearching = isSearchingDeps,
                            searchInput = depSearchInput,
                            onSearchChange = {
                                depSearchInput = it
                                onSearchDependencies(it)
                            },
                            onAdd = onAddDependency,
                            onRemove = onRemoveDependency
                        )
                    }
                    BottomPanelTab.AI_ASSISTANT -> {
                        AiAssistantView(
                            messages = aiMessages,
                            isLoading = isAiLoading,
                            input = aiInput,
                            onInputChange = { aiInput = it },
                            onSend = {
                                if (aiInput.isNotBlank()) {
                                    onSendAiPrompt(aiInput.trim())
                                    aiInput = ""
                                }
                            },
                            onApplyCode = onApplyAiCode
                        )
                    }
                    BottomPanelTab.APKS -> {
                        ApksView(
                            apks = apks,
                            onInstall = onInstallApk,
                            onShare = onShareApk,
                            onDelete = onDeleteApk
                        )
                    }
                    BottomPanelTab.SDK_INFO -> {
                        SdkInfoView(sdkInfo = sdkInfo)
                    }
                }
            }
        }
    }
}

@Composable
private fun BuildLogView(
    buildState: BuildState,
    onCancelBuild: () -> Unit,
    onInstallBuiltApk: () -> Unit,
    onShareBuiltApk: () -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(buildState.logs.size) {
        if (buildState.logs.isNotEmpty()) {
            listState.animateScrollToItem(buildState.logs.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        // Status row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val (color, text) = when (buildState.status) {
                    BuildStatus.IDLE -> MaterialTheme.colorScheme.onSurfaceVariant to "جاهز للبناء (Idle)"
                    BuildStatus.RUNNING -> Color(0xFF00B4D8) to "جاري البناء... ${buildState.taskName}"
                    BuildStatus.SUCCESS -> Color(0xFF4CAF50) to "تم البناء بنجاح! (${buildState.durationMs / 1000}s)"
                    BuildStatus.FAILED -> MaterialTheme.colorScheme.error to "فشل البناء: ${buildState.errorMessage ?: ""}"
                }
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(color, shape = RoundedCornerShape(5.dp))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = text, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }

            Row {
                if (buildState.status == BuildStatus.RUNNING) {
                    TextButton(onClick = onCancelBuild) {
                        Text("إلغاء", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
                if (buildState.outputApk != null && buildState.status == BuildStatus.SUCCESS) {
                    Button(
                        onClick = onInstallBuiltApk,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تثبيت APK", fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = onShareBuiltApk, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        if (buildState.status == BuildStatus.RUNNING) {
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { buildState.progress },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF141517), shape = RoundedCornerShape(4.dp))
                .padding(8.dp)
        ) {
            items(buildState.logs) { log ->
                val time = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(log.timestamp))
                val logColor = when (log.level) {
                    LogLevel.INFO -> Color(0xFF80D8FF)
                    LogLevel.WARNING -> Color(0xFFFFD54F)
                    LogLevel.ERROR -> Color(0xFFFF5252)
                    LogLevel.SUCCESS -> Color(0xFF69F0AE)
                }

                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                    Text(
                        text = "[$time] ",
                        color = Color(0xFF757575),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                    Text(
                        text = log.message,
                        color = logColor,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun TerminalView(
    history: List<TerminalEntry>,
    input: String,
    onInputChange: (String) -> Unit,
    onSendCommand: () -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(history.size) {
        if (history.isNotEmpty()) listState.animateScrollToItem(history.size - 1)
    }

    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        // Quick command chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 6.dp)
        ) {
            val chips = listOf("pwd", "ls", "cat app/build.gradle.kts", "./gradlew assembleDebug", "./gradlew test", "env", "clear")
            chips.forEach { chip ->
                SuggestionChip(
                    onClick = { onInputChange(chip) },
                    label = { Text(chip, fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
        }

        // Terminal History Box
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF0F1115), shape = RoundedCornerShape(4.dp))
                .padding(8.dp)
        ) {
            items(history) { entry ->
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Row {
                        Text("$ ", color = Color(0xFF00E676), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        Text(entry.command, color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    }
                    if (entry.output.isNotBlank()) {
                        Text(
                            text = entry.output,
                            color = if (entry.isError) Color(0xFFFF5252) else Color(0xFFCFD8DC),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(start = 12.dp, top = 2.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Input Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = onInputChange,
                placeholder = { Text("أدخل أمر Terminal هنا...", fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            )
            Spacer(modifier = Modifier.width(6.dp))
            IconButton(onClick = onSendCommand) {
                Icon(Icons.Default.Send, contentDescription = "Run")
            }
        }
    }
}

@Composable
private fun DiagnosticsView(
    issues: List<DiagnosticIssue>,
    onIssueClick: (DiagnosticIssue) -> Unit
) {
    if (issues.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("لا توجد أخطاء برمجية في المشروع (All Checks Passed)", style = MaterialTheme.typography.bodyMedium)
            }
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        items(issues) { issue ->
            val icon = when (issue.severity) {
                DiagnosticSeverity.ERROR -> Icons.Default.Cancel
                DiagnosticSeverity.WARNING -> Icons.Default.Warning
                DiagnosticSeverity.INFO -> Icons.Default.Info
            }
            val color = when (issue.severity) {
                DiagnosticSeverity.ERROR -> MaterialTheme.colorScheme.error
                DiagnosticSeverity.WARNING -> Color(0xFFFFB300)
                DiagnosticSeverity.INFO -> Color(0xFF00B0FF)
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onIssueClick(issue) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = issue.message,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${issue.file.name}:${issue.line}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (issue.quickFixText != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "إصلاح مقترح: ${issue.quickFixText}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DependenciesView(
    declared: List<DependencyItem>,
    popular: List<DependencyItem>,
    searchResults: List<DependencyItem>,
    isSearching: Boolean,
    searchInput: String,
    onSearchChange: (String) -> Unit,
    onAdd: (DependencyItem) -> Unit,
    onRemove: (DependencyItem) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        OutlinedTextField(
            value = searchInput,
            onValueChange = onSearchChange,
            placeholder = { Text("بحث في مستودع Maven Central عن مكتبات...", fontSize = 12.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            if (searchInput.isNotBlank()) {
                item {
                    Text(
                        text = "نتائج البحث (Maven Central):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                if (isSearching) {
                    item {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
                    }
                }
                items(searchResults) { item ->
                    DependencyRow(item = item, isAdded = declared.any { it.group == item.group && it.name == item.name }, onAdd = { onAdd(item) }, onRemove = { onRemove(item) })
                }
            } else {
                item {
                    Text(
                        text = "الاعتماديات الحالية في المشروع (${declared.size}):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                items(declared) { item ->
                    DependencyRow(item = item, isAdded = true, onAdd = {}, onRemove = { onRemove(item) })
                }

                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "مكتبات موصى بها (Jetpack & Popular):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                items(popular) { item ->
                    DependencyRow(item = item, isAdded = declared.any { it.group == item.group && it.name == item.name }, onAdd = { onAdd(item) }, onRemove = { onRemove(item) })
                }
            }
        }
    }
}

@Composable
private fun DependencyRow(
    item: DependencyItem,
    isAdded: Boolean,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("${item.group}:${item.name}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                if (item.version.isNotBlank()) {
                    Text("الإصدار: ${item.version}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (isAdded) {
                IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                }
            } else {
                IconButton(onClick = onAdd, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.AddCircle, contentDescription = "Add", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun AiAssistantView(
    messages: List<AiMessage>,
    isLoading: Boolean,
    input: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onApplyCode: (String) -> Unit
) {
    val listState = rememberLazyListState()
    val clipboard = LocalClipboardManager.current

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
            items(messages) { msg ->
                val isUser = msg.sender == "user"
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.widthIn(max = 300.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(msg.content, style = MaterialTheme.typography.bodySmall)

                            if (msg.proposedCode != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    color = Color(0xFF141517),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            text = msg.proposedCode,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            color = Color(0xFF80D8FF),
                                            maxLines = 8,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row {
                                            Button(
                                                onClick = { onApplyCode(msg.proposedCode) },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.height(26.dp)
                                            ) {
                                                Text("تطبيق في الملف", fontSize = 10.sp)
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            TextButton(
                                                onClick = { clipboard.setText(AnnotatedString(msg.proposedCode)) },
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                                modifier = Modifier.height(26.dp)
                                            ) {
                                                Text("نسخ", fontSize = 10.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (isLoading) {
                item {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("جاري التفكير وتوليد الكود...", fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = onInputChange,
                placeholder = { Text("اسأل الذكاء الاصطناعي عن الكود أو إصلاح الأخطاء...", fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(6.dp))
            IconButton(onClick = onSend, enabled = !isLoading && input.isNotBlank()) {
                Icon(Icons.Default.Send, contentDescription = "Send")
            }
        }
    }
}

@Composable
private fun ApksView(
    apks: List<ApkItem>,
    onInstall: (File) -> Unit,
    onShare: (File) -> Unit,
    onDelete: (File) -> Unit
) {
    if (apks.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Android, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("لا توجد ملفات APK مبنية بعد. اضغط على 'بناء APK' لإنشاء أول تطبيق!", style = MaterialTheme.typography.bodySmall)
            }
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        items(apks) { apk ->
            val dateStr = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(apk.lastModified))
            val sizeMb = String.format(Locale.US, "%.2f MB", apk.sizeBytes / (1024.0 * 1024.0))

            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Android, contentDescription = null, tint = Color(0xFF388E3C), modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(apk.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text("$sizeMb | $dateStr | ${apk.buildVariant}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(apk.packageName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }

                    Row {
                        IconButton(onClick = { onInstall(apk.file) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Download, contentDescription = "Install", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { onShare(apk.file) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                        }
                        IconButton(onClick = { onDelete(apk.file) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SdkInfoView(sdkInfo: SdkInfo) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("بيئة التشغيل وأدوات Android SDK", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                SdkDetailRow("نظام التشغيل", "${sdkInfo.androidVersion} (API ${sdkInfo.apiLevel})")
                SdkDetailRow("طراز الجهاز", sdkInfo.deviceModel)
                SdkDetailRow("المعمارية (ABI)", sdkInfo.architecture)
                SdkDetailRow("الذاكرة العشوائية RAM", "${sdkInfo.availableRamMb} MB متاحة من إجمالي ${sdkInfo.totalRamMb} MB")
                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                SdkDetailRow("مترجم Kotlin", sdkInfo.kotlinVersion)
                SdkDetailRow("محرّك Gradle", sdkInfo.gradleVersion)
                SdkDetailRow("إصدار AGP", sdkInfo.agpVersion)
                SdkDetailRow("المنصة المستهدفة", sdkInfo.targetPlatform)
            }
        }
    }
}

@Composable
private fun SdkDetailRow(title: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}
