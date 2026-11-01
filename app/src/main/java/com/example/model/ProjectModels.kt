package com.example.model

import java.io.File

enum class TemplateType(val displayName: String, val description: String) {
    COMPOSE("Jetpack Compose App", "Modern UI with Kotlin & Material 3 Compose"),
    XML_VIEW("Kotlin + XML Views", "Classic Android View system with Kotlin & ViewBinding"),
    JAVA_XML("Java + XML Views", "Traditional Java development with Android XML layouts"),
    EMPTY("Empty Project", "Minimal project skeleton with Gradle configuration")
}

data class Project(
    val id: String,
    val name: String,
    val packageName: String,
    val rootDir: File,
    val templateType: TemplateType,
    val minSdk: Int = 26,
    val targetSdk: Int = 36,
    val createdAt: Long = System.currentTimeMillis(),
    val lastModifiedAt: Long = System.currentTimeMillis()
)

data class FileNode(
    val file: File,
    val name: String = file.name,
    val isDirectory: Boolean = file.isDirectory,
    val path: String = file.absolutePath,
    val extension: String = file.extension.lowercase(),
    val children: List<FileNode> = emptyList(),
    val isExpanded: Boolean = false,
    val sizeBytes: Long = if (!file.isDirectory) file.length() else 0L
)

data class EditorTab(
    val file: File,
    val title: String = file.name,
    val content: String = "",
    val originalContent: String = "",
    val isModified: Boolean = false,
    val cursorPosition: Int = 0,
    val scrollLine: Int = 0
)

enum class LogLevel {
    INFO, WARNING, ERROR, SUCCESS
}

data class BuildLogItem(
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel,
    val message: String,
    val filePath: String? = null,
    val lineNumber: Int? = null
)

enum class BuildStatus {
    IDLE, RUNNING, SUCCESS, FAILED
}

data class BuildState(
    val status: BuildStatus = BuildStatus.IDLE,
    val taskName: String = "",
    val progress: Float = 0f,
    val logs: List<BuildLogItem> = emptyList(),
    val outputApk: File? = null,
    val durationMs: Long = 0L,
    val errorMessage: String? = null
)

data class DependencyItem(
    val group: String,
    val name: String,
    val version: String,
    val configuration: String = "implementation",
    val description: String = "",
    val isOfficial: Boolean = true
)

enum class DiagnosticSeverity {
    INFO, WARNING, ERROR
}

data class DiagnosticIssue(
    val file: File,
    val line: Int,
    val column: Int = 0,
    val message: String,
    val severity: DiagnosticSeverity = DiagnosticSeverity.ERROR,
    val ruleId: String = "",
    val quickFixText: String? = null
)

data class TerminalEntry(
    val command: String,
    val output: String,
    val isError: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

data class ApkItem(
    val file: File,
    val name: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val buildVariant: String,
    val packageName: String,
    val versionName: String = "1.0",
    val versionCode: Int = 1
)

data class SdkInfo(
    val androidVersion: String,
    val apiLevel: Int,
    val deviceModel: String,
    val architecture: String,
    val totalRamMb: Long,
    val availableRamMb: Long,
    val kotlinVersion: String = "2.2.10",
    val gradleVersion: String = "9.1.1",
    val agpVersion: String = "9.1.1",
    val targetPlatform: String = "Android SDK 36 (VanillaIceCream)"
)
