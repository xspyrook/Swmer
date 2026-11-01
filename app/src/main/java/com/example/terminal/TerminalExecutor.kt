package com.example.terminal

import com.example.model.Project
import com.example.model.TerminalEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TerminalExecutor {

    private val _history = MutableStateFlow<List<TerminalEntry>>(
        listOf(
            TerminalEntry(
                command = "welcome",
                output = "Android Studio Mobile Terminal [Version 1.0.0]\nType 'help' to see available development commands."
            )
        )
    )
    val history: StateFlow<List<TerminalEntry>> = _history.asStateFlow()

    fun clear() {
        _history.value = emptyList()
    }

    suspend fun execute(
        commandRaw: String,
        project: Project?,
        onBuildRequested: (suspend (Boolean) -> Unit)? = null
    ) {
        val cmd = commandRaw.trim()
        if (cmd.isEmpty()) return

        if (cmd == "clear") {
            clear()
            return
        }

        if (project == null) {
            appendEntry(cmd, "Error: No project opened. Open or create a project first.", isError = true)
            return
        }

        val workingDir = project.rootDir
        val parts = cmd.split("\\s+".toRegex())
        val name = parts[0]
        val args = parts.drop(1)

        val output = when (name) {
            "pwd" -> workingDir.absolutePath
            "ls", "dir" -> {
                val targetDir = if (args.isNotEmpty()) File(workingDir, args[0]) else workingDir
                if (!targetDir.exists()) {
                    "Error: directory not found: ${args.getOrNull(0)}"
                } else {
                    val files = targetDir.listFiles()?.sortedWith(compareBy<File>({ !it.isDirectory }, { it.name })) ?: emptyList<File>()
                    val sb = StringBuilder()
                    files.forEach { f ->
                        val type = if (f.isDirectory) "<DIR> " else "      "
                        val size = if (f.isFile) "${f.length()} B" else ""
                        sb.append(String.format(Locale.US, "%-6s %-10s %s\n", type, size, f.name))
                    }
                    if (files.isEmpty()) "Directory is empty" else sb.toString().trimEnd()
                }
            }
            "cat" -> {
                if (args.isEmpty()) {
                    "Usage: cat <file_name>"
                } else {
                    val file = File(workingDir, args[0])
                    if (!file.exists()) {
                        "File not found: ${args[0]}"
                    } else if (file.isDirectory) {
                        "Error: ${args[0]} is a directory"
                    } else {
                        file.readText().take(4000)
                    }
                }
            }
            "mkdir" -> {
                if (args.isEmpty()) {
                    "Usage: mkdir <folder_name>"
                } else {
                    val dir = File(workingDir, args[0])
                    if (dir.mkdirs()) "Created directory: ${args[0]}" else "Failed to create directory"
                }
            }
            "touch" -> {
                if (args.isEmpty()) {
                    "Usage: touch <file_name>"
                } else {
                    val file = File(workingDir, args[0])
                    if (file.createNewFile()) "Created file: ${args[0]}" else "Failed or already exists"
                }
            }
            "rm" -> {
                if (args.isEmpty()) {
                    "Usage: rm <path>"
                } else {
                    val file = File(workingDir, args[0])
                    if (file.exists()) {
                        if (file.deleteRecursively()) "Deleted: ${args[0]}" else "Failed to delete"
                    } else {
                        "Path not found: ${args[0]}"
                    }
                }
            }
            "gradlew", "./gradlew", "gradle" -> {
                val task = args.firstOrNull() ?: "help"
                when (task) {
                    "assembleDebug", "build" -> {
                        appendEntry(cmd, "Executing task :app:assembleDebug...")
                        onBuildRequested?.invoke(false)
                        return
                    }
                    "assembleRelease" -> {
                        appendEntry(cmd, "Executing task :app:assembleRelease...")
                        onBuildRequested?.invoke(true)
                        return
                    }
                    "test", "testDebugUnitTest" -> {
                        ":app:compileDebugUnitTestKotlin UP-TO-DATE\n" +
                        ":app:testDebugUnitTest\n" +
                        "com.example.ExampleUnitTest > addition_isCorrect PASSED\n" +
                        "BUILD SUCCESSFUL in 1s (1 test passed)"
                    }
                    "tasks" -> {
                        """
                        > Task :tasks
                        ------------------------------------------------------------
                        Tasks runnable from project ':app'
                        ------------------------------------------------------------
                        Build Tasks:
                          assemble - Assembles all variants of all applications and secondary packages.
                          assembleDebug - Assembles main APK in Debug mode.
                          assembleRelease - Assembles main APK in Release mode.
                          clean - Deletes the build directory.
                        Verification Tasks:
                          check - Runs all checks.
                          test - Runs the unit tests.
                        Help Tasks:
                          dependencies - Displays all dependencies declared in project ':app'.
                        """.trimIndent()
                    }
                    "clean" -> {
                        val buildDir = File(workingDir, "build")
                        val appBuildDir = File(workingDir, "app/build")
                        buildDir.deleteRecursively()
                        appBuildDir.deleteRecursively()
                        ":app:clean UP-TO-DATE\nBUILD SUCCESSFUL"
                    }
                    else -> "Unknown Gradle task '$task'. Try 'tasks' or 'assembleDebug'."
                }
            }
            "git" -> {
                val sub = args.firstOrNull() ?: "status"
                when (sub) {
                    "status" -> {
                        "On branch main\nChanges not staged for commit:\n  (use git add <file> to update)\n  app/build.gradle.kts\nno changes added to commit (use git add)"
                    }
                    "log" -> {
                        "commit a1b2c3d4 (HEAD -> main)\nAuthor: Mobile Developer <user@as-mobile.local>\nDate:   ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())}\n\n    Initial project commit (${project.name})"
                    }
                    else -> "git: command '$sub' simulated on mobile workspace."
                }
            }
            "env", "sdk" -> {
                """
                Android Studio Mobile Environment:
                  Target SDK: ${project.targetSdk}
                  Min SDK: ${project.minSdk}
                  Kotlin Compiler: 2.2.10
                  Gradle Daemon: 9.1.1
                  JVM: Android ART (Java 17 compatible)
                  Working Dir: ${workingDir.name}
                """.trimIndent()
            }
            "help" -> {
                """
                Available IDE Terminal Commands:
                  pwd             - Print working directory
                  ls [path]       - List files and directories
                  cat <file>      - View file contents
                  mkdir <dir>     - Create directory
                  touch <file>    - Create empty file
                  rm <path>       - Remove file or directory
                  ./gradlew <task>- Run Gradle tasks (assembleDebug, assembleRelease, test, tasks, clean)
                  git [status|log]- Inspect repository status
                  env             - Display environment & SDK info
                  clear           - Clear terminal window
                """.trimIndent()
            }
            else -> "Command not recognized: '$name'. Type 'help' for available commands."
        }

        appendEntry(cmd, output)
    }

    private fun appendEntry(cmd: String, out: String, isError: Boolean = false) {
        _history.value = _history.value + TerminalEntry(command = cmd, output = out, isError = isError)
    }
}
