package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiAssistantService
import com.example.ai.AiMessage
import com.example.apkmanager.ApkManagerService
import com.example.build.ApkBuilderEngine
import com.example.dependencies.DependencyRepository
import com.example.diagnostics.ProjectLinter
import com.example.editor.CodeSyntaxHighlighter
import com.example.filesystem.ProjectFileManager
import com.example.model.ApkItem
import com.example.model.BuildState
import com.example.model.DependencyItem
import com.example.model.DiagnosticIssue
import com.example.model.EditorTab
import com.example.model.FileNode
import com.example.model.Project
import com.example.model.SdkInfo
import com.example.model.TemplateType
import com.example.model.TerminalEntry
import com.example.sdk.SdkInfoProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream

enum class BottomPanelTab(val titleAr: String, val titleEn: String) {
    BUILD_LOG("سجل البناء", "Build Log"),
    TERMINAL("الطرفية", "Terminal"),
    DIAGNOSTICS("التشخيص", "Diagnostics"),
    DEPENDENCIES("الاعتماديات", "Dependencies"),
    AI_ASSISTANT("المساعد الذكي", "AI Assistant"),
    APKS("ملفات APK", "APK Files"),
    SDK_INFO("أدوات SDK", "SDK Info")
}

class StudioViewModel(application: Application) : AndroidViewModel(application) {

    val fileManager = ProjectFileManager(application)
    val builderEngine = ApkBuilderEngine(application)
    val sdkProvider = SdkInfoProvider(application)
    val depRepo = DependencyRepository()
    val terminalExecutor = com.example.terminal.TerminalExecutor()
    val linter = ProjectLinter()
    val aiService = AiAssistantService()
    val apkService = ApkManagerService(application)

    // Projects & Active Project
    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    val projects: StateFlow<List<Project>> = _projects.asStateFlow()

    private val _currentProject = MutableStateFlow<Project?>(null)
    val currentProject: StateFlow<Project?> = _currentProject.asStateFlow()

    // File Tree & Expanded Nodes
    private val _expandedPaths = MutableStateFlow<Set<String>>(emptySet())
    val expandedPaths: StateFlow<Set<String>> = _expandedPaths.asStateFlow()

    private val _fileTree = MutableStateFlow<FileNode?>(null)
    val fileTree: StateFlow<FileNode?> = _fileTree.asStateFlow()

    // Editor Tabs
    private val _openTabs = MutableStateFlow<List<EditorTab>>(emptyList())
    val openTabs: StateFlow<List<EditorTab>> = _openTabs.asStateFlow()

    private val _activeTabIndex = MutableStateFlow<Int>(-1)
    val activeTabIndex: StateFlow<Int> = _activeTabIndex.asStateFlow()

    // Bottom Panel
    private val _bottomPanelTab = MutableStateFlow(BottomPanelTab.BUILD_LOG)
    val bottomPanelTab: StateFlow<BottomPanelTab> = _bottomPanelTab.asStateFlow()

    private val _isBottomPanelExpanded = MutableStateFlow(false)
    val isBottomPanelExpanded: StateFlow<Boolean> = _isBottomPanelExpanded.asStateFlow()

    // Build State
    val buildState: StateFlow<BuildState> = builderEngine.buildState

    // Terminal History
    val terminalHistory: StateFlow<List<TerminalEntry>> = terminalExecutor.history

    // Diagnostics
    private val _diagnostics = MutableStateFlow<List<DiagnosticIssue>>(emptyList())
    val diagnostics: StateFlow<List<DiagnosticIssue>> = _diagnostics.asStateFlow()

    // SDK Info
    private val _sdkInfo = MutableStateFlow(sdkProvider.getSdkInfo())
    val sdkInfo: StateFlow<SdkInfo> = _sdkInfo.asStateFlow()

    // Dependencies
    private val _projectDependencies = MutableStateFlow<List<DependencyItem>>(emptyList())
    val projectDependencies: StateFlow<List<DependencyItem>> = _projectDependencies.asStateFlow()

    private val _searchResults = MutableStateFlow<List<DependencyItem>>(emptyList())
    val searchResults: StateFlow<List<DependencyItem>> = _searchResults.asStateFlow()

    private val _isSearchingDeps = MutableStateFlow(false)
    val isSearchingDeps: StateFlow<Boolean> = _isSearchingDeps.asStateFlow()

    // AI Messages
    private val _aiMessages = MutableStateFlow<List<AiMessage>>(
        listOf(
            AiMessage(
                sender = "assistant",
                content = "مرحباً بك في مساعد Android Studio Mobile الذكي! كيف يمكنني مساعدتك في تطوير تطبيقك اليوم؟"
            )
        )
    )
    val aiMessages: StateFlow<List<AiMessage>> = _aiMessages.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    // APKs
    private val _apks = MutableStateFlow<List<ApkItem>>(emptyList())
    val apks: StateFlow<List<ApkItem>> = _apks.asStateFlow()

    // Search and Replace inside current file
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _replaceQuery = MutableStateFlow("")
    val replaceQuery: StateFlow<String> = _replaceQuery.asStateFlow()

    private val _isSearchBarVisible = MutableStateFlow(false)
    val isSearchBarVisible: StateFlow<Boolean> = _isSearchBarVisible.asStateFlow()

    // Custom API Key in settings
    private val _customApiKey = MutableStateFlow("")
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    init {
        loadProjects()
        // If there are no projects, create an initial demo project so the IDE is immediately ready to run
        if (_projects.value.isEmpty()) {
            createNewProject(
                name = "MyFirstApp",
                packageName = "com.my.androidstudio",
                template = TemplateType.COMPOSE
            )
        }
    }

    fun loadProjects() {
        _projects.value = fileManager.getAllProjects()
        refreshApks()
    }

    fun createNewProject(name: String, packageName: String, template: TemplateType, minSdk: Int = 26) {
        val proj = fileManager.createProject(name, packageName, template, minSdk)
        loadProjects()
        openProject(proj)
    }

    fun openProject(project: Project) {
        _currentProject.value = project
        fileManager.updateProjectTimestamp(project.id)
        _openTabs.value = emptyList()
        _activeTabIndex.value = -1

        // Expand root by default
        _expandedPaths.value = setOf(
            project.rootDir.absolutePath,
            File(project.rootDir, "app").absolutePath,
            File(project.rootDir, "app/src").absolutePath,
            File(project.rootDir, "app/src/main").absolutePath
        )
        refreshFileTree()
        refreshDependencies()
        runDiagnostics()
        refreshApks()

        // Automatically open MainActivity or AndroidManifest.xml
        viewModelScope.launch {
            val mainActivity = project.rootDir.walkTopDown().firstOrNull {
                it.name.startsWith("MainActivity")
            }
            if (mainActivity != null) {
                openFile(mainActivity)
            } else {
                val manifest = File(project.rootDir, "app/src/main/AndroidManifest.xml")
                if (manifest.exists()) openFile(manifest)
            }
        }
    }

    fun closeProject() {
        _currentProject.value = null
        _openTabs.value = emptyList()
        _activeTabIndex.value = -1
        _fileTree.value = null
        loadProjects()
    }

    fun deleteProject(project: Project) {
        fileManager.deleteProject(project)
        if (_currentProject.value?.id == project.id) {
            closeProject()
        } else {
            loadProjects()
        }
    }

    fun togglePathExpansion(path: String) {
        val set = _expandedPaths.value.toMutableSet()
        if (set.contains(path)) set.remove(path) else set.add(path)
        _expandedPaths.value = set
        refreshFileTree()
    }

    fun refreshFileTree() {
        val proj = _currentProject.value ?: return
        _fileTree.value = fileManager.buildFileTree(proj.rootDir, _expandedPaths.value)
    }

    fun openFile(file: File) {
        if (file.isDirectory) {
            togglePathExpansion(file.absolutePath)
            return
        }
        val existingIndex = _openTabs.value.indexOfFirst { it.file.absolutePath == file.absolutePath }
        if (existingIndex >= 0) {
            _activeTabIndex.value = existingIndex
            return
        }
        val text = fileManager.readFile(file)
        val newTab = EditorTab(
            file = file,
            title = file.name,
            content = text,
            originalContent = text,
            isModified = false
        )
        val updated = _openTabs.value + newTab
        _openTabs.value = updated
        _activeTabIndex.value = updated.size - 1
    }

    fun closeTab(index: Int) {
        val list = _openTabs.value.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            _openTabs.value = list
            _activeTabIndex.value = when {
                list.isEmpty() -> -1
                _activeTabIndex.value >= list.size -> list.size - 1
                else -> _activeTabIndex.value
            }
        }
    }

    fun updateActiveTabContent(newContent: String) {
        val idx = _activeTabIndex.value
        if (idx !in _openTabs.value.indices) return
        val current = _openTabs.value[idx]
        val modified = newContent != current.originalContent
        val updatedTab = current.copy(content = newContent, isModified = modified)
        val list = _openTabs.value.toMutableList()
        list[idx] = updatedTab
        _openTabs.value = list
    }

    fun saveActiveTab(): Boolean {
        val idx = _activeTabIndex.value
        if (idx !in _openTabs.value.indices) return false
        val current = _openTabs.value[idx]
        val ok = fileManager.writeFile(current.file, current.content)
        if (ok) {
            val list = _openTabs.value.toMutableList()
            list[idx] = current.copy(originalContent = current.content, isModified = false)
            _openTabs.value = list
            runDiagnostics()
        }
        return ok
    }

    fun saveAllTabs() {
        val list = _openTabs.value.toMutableList()
        for (i in list.indices) {
            val tab = list[i]
            if (tab.isModified) {
                fileManager.writeFile(tab.file, tab.content)
                list[i] = tab.copy(originalContent = tab.content, isModified = false)
            }
        }
        _openTabs.value = list
        runDiagnostics()
    }

    fun createNewFileIn(parentDir: File, name: String): File? {
        val f = fileManager.createNewFile(parentDir, name)
        if (f != null) {
            refreshFileTree()
            openFile(f)
        }
        return f
    }

    fun createNewFolderIn(parentDir: File, name: String): File? {
        val d = fileManager.createNewFolder(parentDir, name)
        if (d != null) {
            val set = _expandedPaths.value.toMutableSet()
            set.add(parentDir.absolutePath)
            set.add(d.absolutePath)
            _expandedPaths.value = set
            refreshFileTree()
        }
        return d
    }

    fun deleteFile(file: File) {
        fileManager.deleteFile(file)
        // Close if open in tabs
        val idx = _openTabs.value.indexOfFirst { it.file.absolutePath == file.absolutePath }
        if (idx >= 0) closeTab(idx)
        refreshFileTree()
        runDiagnostics()
    }

    fun renameFile(file: File, newName: String) {
        val target = fileManager.renameFile(file, newName)
        if (target != null) {
            // Update open tab
            val idx = _openTabs.value.indexOfFirst { it.file.absolutePath == file.absolutePath }
            if (idx >= 0) {
                val list = _openTabs.value.toMutableList()
                list[idx] = list[idx].copy(file = target, title = target.name)
                _openTabs.value = list
            }
            refreshFileTree()
        }
    }

    fun triggerBuild(isRelease: Boolean = false) {
        val proj = _currentProject.value ?: return
        saveAllTabs()
        _bottomPanelTab.value = BottomPanelTab.BUILD_LOG
        _isBottomPanelExpanded.value = true

        viewModelScope.launch {
            val apk = builderEngine.executeBuild(proj, isRelease)
            if (apk != null) {
                refreshApks()
            }
        }
    }

    fun cancelBuild() {
        builderEngine.cancelBuild()
    }

    fun installBuiltApk() {
        val apk = buildState.value.outputApk ?: _apks.value.firstOrNull()?.file ?: return
        builderEngine.installApk(apk)
    }

    fun shareBuiltApk() {
        val apk = buildState.value.outputApk ?: _apks.value.firstOrNull()?.file ?: return
        builderEngine.shareApk(apk)
    }

    fun sendTerminalCommand(command: String) {
        viewModelScope.launch {
            terminalExecutor.execute(
                commandRaw = command,
                project = _currentProject.value,
                onBuildRequested = { isRel -> triggerBuild(isRel) }
            )
        }
    }

    fun runDiagnostics() {
        val proj = _currentProject.value ?: return
        _diagnostics.value = linter.lintProject(proj)
    }

    fun refreshDependencies() {
        val proj = _currentProject.value ?: return
        val appGradle = File(proj.rootDir, "app/build.gradle.kts")
        _projectDependencies.value = depRepo.parseProjectDependencies(appGradle)
    }

    fun searchDependencies(query: String) {
        viewModelScope.launch {
            _isSearchingDeps.value = true
            _searchResults.value = depRepo.searchMavenCentral(query)
            _isSearchingDeps.value = false
        }
    }

    fun addDependency(item: DependencyItem) {
        val proj = _currentProject.value ?: return
        val appGradle = File(proj.rootDir, "app/build.gradle.kts")
        val added = depRepo.addDependencyToGradle(appGradle, item)
        if (added) {
            refreshDependencies()
            // Update open tab if build.gradle.kts is open
            val idx = _openTabs.value.indexOfFirst { it.file.absolutePath == appGradle.absolutePath }
            if (idx >= 0) {
                val list = _openTabs.value.toMutableList()
                val newText = appGradle.readText()
                list[idx] = list[idx].copy(content = newText, originalContent = newText, isModified = false)
                _openTabs.value = list
            }
        }
    }

    fun removeDependency(item: DependencyItem) {
        val proj = _currentProject.value ?: return
        val appGradle = File(proj.rootDir, "app/build.gradle.kts")
        val removed = depRepo.removeDependencyFromGradle(appGradle, item)
        if (removed) {
            refreshDependencies()
        }
    }

    fun sendAiPrompt(prompt: String) {
        val activeCode = _openTabs.value.getOrNull(_activeTabIndex.value)?.content ?: ""
        val userMsg = AiMessage(sender = "user", content = prompt)
        _aiMessages.value = _aiMessages.value + userMsg
        _isAiLoading.value = true

        viewModelScope.launch {
            val response = aiService.sendMessage(
                prompt = prompt,
                contextCode = activeCode,
                userCustomKey = _customApiKey.value
            )
            _aiMessages.value = _aiMessages.value + response
            _isAiLoading.value = false
        }
    }

    fun applyAiCodeToActiveTab(code: String) {
        val idx = _activeTabIndex.value
        if (idx in _openTabs.value.indices) {
            updateActiveTabContent(code)
        }
    }

    fun refreshApks() {
        _apks.value = apkService.scanApks(_projects.value)
    }

    fun installApk(file: File) {
        apkService.installApk(file)
    }

    fun shareApk(file: File) {
        apkService.shareApk(file)
    }

    fun deleteApk(file: File) {
        apkService.deleteApk(file)
        refreshApks()
    }

    fun setBottomPanelTab(tab: BottomPanelTab) {
        _bottomPanelTab.value = tab
        _isBottomPanelExpanded.value = true
    }

    fun toggleBottomPanel() {
        _isBottomPanelExpanded.value = !_isBottomPanelExpanded.value
    }

    fun toggleSearchBar() {
        _isSearchBarVisible.value = !_isSearchBarVisible.value
        if (!_isSearchBarVisible.value) {
            _searchQuery.value = ""
            _replaceQuery.value = ""
        }
    }

    fun setSearchQuery(q: String) {
        _searchQuery.value = q
    }

    fun setReplaceQuery(r: String) {
        _replaceQuery.value = r
    }

    fun performReplace() {
        val q = _searchQuery.value
        val r = _replaceQuery.value
        if (q.isBlank()) return
        val activeContent = _openTabs.value.getOrNull(_activeTabIndex.value)?.content ?: return
        val updated = activeContent.replace(q, r)
        updateActiveTabContent(updated)
    }

    fun setCustomApiKey(key: String) {
        _customApiKey.value = key
    }
}
