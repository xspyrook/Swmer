package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BuildStatus
import com.example.model.DiagnosticSeverity
import com.example.model.EditorTab
import com.example.model.Project
import com.example.ui.components.BottomPanel
import com.example.ui.components.CodeEditorView
import com.example.ui.components.FileTreeView
import com.example.viewmodel.BottomPanelTab
import com.example.viewmodel.StudioViewModel
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdeScreen(
    viewModel: StudioViewModel,
    project: Project,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val fileTree by viewModel.fileTree.collectAsState()
    val openTabs by viewModel.openTabs.collectAsState()
    val activeTabIndex by viewModel.activeTabIndex.collectAsState()
    val buildState by viewModel.buildState.collectAsState()
    val bottomPanelTab by viewModel.bottomPanelTab.collectAsState()
    val isBottomPanelExpanded by viewModel.isBottomPanelExpanded.collectAsState()
    val terminalHistory by viewModel.terminalHistory.collectAsState()
    val diagnostics by viewModel.diagnostics.collectAsState()
    val sdkInfo by viewModel.sdkInfo.collectAsState()
    val projectDependencies by viewModel.projectDependencies.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearchingDeps by viewModel.isSearchingDeps.collectAsState()
    val aiMessages by viewModel.aiMessages.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    val apks by viewModel.apks.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val replaceQuery by viewModel.replaceQuery.collectAsState()
    val isSearchBarVisible by viewModel.isSearchBarVisible.collectAsState()

    var showBuildVariantMenu by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    val activeTab: EditorTab? = openTabs.getOrNull(activeTabIndex)

    val errorCount = remember(diagnostics) {
        diagnostics.count { it.severity == DiagnosticSeverity.ERROR }
    }

    // Intercept Back button: Close bottom panel if open, else close drawer, else ask to return to dashboard
    BackHandler {
        if (isBottomPanelExpanded) {
            viewModel.toggleBottomPanel()
        } else if (drawerState.isOpen) {
            coroutineScope.launch { drawerState.close() }
        } else {
            viewModel.saveAllTabs()
            onNavigateBack()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp)
            ) {
                FileTreeView(
                    rootNode = fileTree,
                    selectedFile = activeTab?.file,
                    onFileSelected = { file ->
                        viewModel.openFile(file)
                        coroutineScope.launch { drawerState.close() }
                    },
                    onToggleExpand = { viewModel.togglePathExpansion(it) },
                    onCreateFile = { parent, name -> viewModel.createNewFileIn(parent, name) },
                    onCreateFolder = { parent, name -> viewModel.createNewFolderIn(parent, name) },
                    onDeleteFile = { viewModel.deleteFile(it) },
                    onRenameFile = { file, newName -> viewModel.renameFile(file, newName) }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = project.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = project.packageName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Open File Explorer")
                        }
                    },
                    actions = {
                        // Save button
                        IconButton(
                            onClick = {
                                val ok = viewModel.saveActiveTab()
                                if (ok) Toast.makeText(context, "تم حفظ الملف", Toast.LENGTH_SHORT).show()
                            },
                            enabled = activeTab?.isModified == true
                        ) {
                            Icon(
                                Icons.Default.Save,
                                contentDescription = "Save",
                                tint = if (activeTab?.isModified == true) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }

                        // Build Action
                        Box {
                            IconButton(onClick = { showBuildVariantMenu = true }) {
                                if (buildState.status == BuildStatus.RUNNING) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(
                                        Icons.Default.Build,
                                        contentDescription = "Build APK",
                                        tint = Color(0xFF29B6F6)
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = showBuildVariantMenu,
                                onDismissRequest = { showBuildVariantMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("بناء Debug APK") },
                                    leadingIcon = { Icon(Icons.Default.BugReport, contentDescription = null) },
                                    onClick = {
                                        showBuildVariantMenu = false
                                        viewModel.triggerBuild(isRelease = false)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("بناء Release APK") },
                                    leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
                                    onClick = {
                                        showBuildVariantMenu = false
                                        viewModel.triggerBuild(isRelease = true)
                                    }
                                )
                            }
                        }

                        // Run / Install APK
                        IconButton(
                            onClick = {
                                if (buildState.outputApk != null) {
                                    viewModel.installBuiltApk()
                                } else {
                                    viewModel.triggerBuild(isRelease = false)
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Run App",
                                tint = Color(0xFF00E676)
                            )
                        }

                        // Bottom Panel Toggle
                        IconButton(onClick = { viewModel.toggleBottomPanel() }) {
                            BadgedBox(badge = {
                                if (errorCount > 0) {
                                    Badge { Text("$errorCount") }
                                }
                            }) {
                                Icon(Icons.Default.Terminal, contentDescription = "Terminal & Logs")
                            }
                        }

                        // More options
                        Box {
                            IconButton(onClick = { showOverflowMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More")
                            }
                            DropdownMenu(
                                expanded = showOverflowMenu,
                                onDismissRequest = { showOverflowMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("بحث واستبدال (Search/Replace)") },
                                    leadingIcon = { Icon(Icons.Default.Search, null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        viewModel.toggleSearchBar()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("تشخيص وفحص المشروع") },
                                    leadingIcon = { Icon(Icons.Default.FactCheck, null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        viewModel.setBottomPanelTab(BottomPanelTab.DIAGNOSTICS)
                                        viewModel.runDiagnostics()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("مدير الاعتماديات (Dependencies)") },
                                    leadingIcon = { Icon(Icons.Default.Extension, null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        viewModel.setBottomPanelTab(BottomPanelTab.DEPENDENCIES)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("مساعد الذكاء الاصطناعي") },
                                    leadingIcon = { Icon(Icons.Default.AutoAwesome, null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        viewModel.setBottomPanelTab(BottomPanelTab.AI_ASSISTANT)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("مدير حزم APK") },
                                    leadingIcon = { Icon(Icons.Default.Android, null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        viewModel.setBottomPanelTab(BottomPanelTab.APKS)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("أدوات Android SDK") },
                                    leadingIcon = { Icon(Icons.Default.Info, null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        viewModel.setBottomPanelTab(BottomPanelTab.SDK_INFO)
                                    }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("الخروج إلى لوحة التحكم") },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        viewModel.saveAllTabs()
                                        onNavigateBack()
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            modifier = modifier
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Open Tabs Strip
                if (openTabs.isNotEmpty()) {
                    Surface(
                        tonalElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .background(MaterialTheme.colorScheme.surface),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            openTabs.forEachIndexed { index, tab ->
                                val isSelected = index == activeTabIndex
                                Surface(
                                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                                    modifier = Modifier
                                        .clickable { viewModel.openFile(tab.file) }
                                        .padding(horizontal = 2.dp, vertical = 2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = tab.title + if (tab.isModified) " *" else "",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        IconButton(
                                            onClick = { viewModel.closeTab(index) },
                                            modifier = Modifier.size(18.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Close tab",
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    HorizontalDivider(thickness = 0.5.dp)
                }

                // Editor
                Box(modifier = Modifier.weight(1f)) {
                    CodeEditorView(
                        activeTab = activeTab,
                        searchQuery = searchQuery,
                        replaceQuery = replaceQuery,
                        isSearchBarVisible = isSearchBarVisible,
                        onContentChanged = { viewModel.updateActiveTabContent(it) },
                        onSaveRequested = { viewModel.saveActiveTab() },
                        onSearchQueryChanged = { viewModel.setSearchQuery(it) },
                        onReplaceQueryChanged = { viewModel.setReplaceQuery(it) },
                        onPerformReplace = { viewModel.performReplace() },
                        onCloseSearchBar = { viewModel.toggleSearchBar() },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Bottom Panel
                BottomPanel(
                    selectedTab = bottomPanelTab,
                    isExpanded = isBottomPanelExpanded,
                    buildState = buildState,
                    terminalHistory = terminalHistory,
                    diagnostics = diagnostics,
                    projectDependencies = projectDependencies,
                    popularLibraries = viewModel.depRepo.popularLibraries,
                    searchResults = searchResults,
                    isSearchingDeps = isSearchingDeps,
                    aiMessages = aiMessages,
                    isAiLoading = isAiLoading,
                    apks = apks,
                    sdkInfo = sdkInfo,
                    onTabSelected = { viewModel.setBottomPanelTab(it) },
                    onToggleExpand = { viewModel.toggleBottomPanel() },
                    onCancelBuild = { viewModel.cancelBuild() },
                    onInstallBuiltApk = { viewModel.installBuiltApk() },
                    onShareBuiltApk = { viewModel.shareBuiltApk() },
                    onSendTerminalCommand = { viewModel.sendTerminalCommand(it) },
                    onOpenFile = {
                        viewModel.openFile(it)
                        coroutineScope.launch { drawerState.close() }
                    },
                    onAddDependency = { viewModel.addDependency(it) },
                    onRemoveDependency = { viewModel.removeDependency(it) },
                    onSearchDependencies = { viewModel.searchDependencies(it) },
                    onSendAiPrompt = { viewModel.sendAiPrompt(it) },
                    onApplyAiCode = { viewModel.applyAiCodeToActiveTab(it) },
                    onInstallApk = { viewModel.installApk(it) },
                    onShareApk = { viewModel.shareApk(it) },
                    onDeleteApk = { viewModel.deleteApk(it) }
                )
            }
        }
    }
}
