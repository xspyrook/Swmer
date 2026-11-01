package com.example.diagnostics

import com.example.model.DiagnosticIssue
import com.example.model.DiagnosticSeverity
import com.example.model.Project
import java.io.File
import java.util.Stack

class ProjectLinter {

    fun lintProject(project: Project): List<DiagnosticIssue> {
        val issues = mutableListOf<DiagnosticIssue>()

        // 1. Lint AndroidManifest.xml
        val manifest = File(project.rootDir, "app/src/main/AndroidManifest.xml")
        if (!manifest.exists()) {
            issues.add(
                DiagnosticIssue(
                    file = manifest,
                    line = 1,
                    message = "Missing AndroidManifest.xml in app/src/main/",
                    severity = DiagnosticSeverity.ERROR,
                    ruleId = "manifest-missing"
                )
            )
        } else {
            lintManifest(manifest, issues)
        }

        // 2. Lint Gradle files
        val appGradle = File(project.rootDir, "app/build.gradle.kts")
        if (appGradle.exists()) {
            lintGradle(appGradle, issues)
        }

        // 3. Lint Source files (Kotlin, Java, XML)
        val srcDir = File(project.rootDir, "app/src/main")
        if (srcDir.exists()) {
            srcDir.walkTopDown().forEach { file ->
                if (file.isFile) {
                    when (file.extension.lowercase()) {
                        "kt", "java" -> lintSourceCode(file, issues)
                        "xml" -> if (file != manifest) lintXmlFile(file, issues)
                    }
                }
            }
        }

        return issues
    }

    private fun lintManifest(file: File, issues: MutableList<DiagnosticIssue>) {
        val text = file.readText()
        val lines = text.lines()

        if (!text.contains("<application")) {
            issues.add(
                DiagnosticIssue(
                    file = file,
                    line = 1,
                    message = "AndroidManifest.xml missing <application> tag",
                    severity = DiagnosticSeverity.ERROR,
                    ruleId = "manifest-no-app"
                )
            )
        }

        if (!text.contains("android.intent.action.MAIN")) {
            issues.add(
                DiagnosticIssue(
                    file = file,
                    line = 1,
                    message = "No Activity with MAIN action found. App cannot be launched from home screen.",
                    severity = DiagnosticSeverity.WARNING,
                    ruleId = "manifest-no-launcher"
                )
            )
        }

        // Check android:exported on activities with intent filters
        lines.forEachIndexed { index, line ->
            if (line.contains("<activity") && !line.contains("android:exported") && text.contains("android.intent.action.MAIN")) {
                // If it doesn't specify android:exported
                issues.add(
                    DiagnosticIssue(
                        file = file,
                        line = index + 1,
                        message = "Apps targeting Android 12+ must explicitly specify 'android:exported' on <activity>",
                        severity = DiagnosticSeverity.WARNING,
                        ruleId = "manifest-exported",
                        quickFixText = "android:exported=\"true\""
                    )
                )
            }
        }
    }

    private fun lintGradle(file: File, issues: MutableList<DiagnosticIssue>) {
        val text = file.readText()
        if (!text.contains("compileSdk")) {
            issues.add(
                DiagnosticIssue(
                    file = file,
                    line = 1,
                    message = "Gradle build file should declare compileSdk",
                    severity = DiagnosticSeverity.WARNING,
                    ruleId = "gradle-no-compilesdk"
                )
            )
        }
        if (!text.contains("dependencies {")) {
            issues.add(
                DiagnosticIssue(
                    file = file,
                    line = 1,
                    message = "Gradle build file missing dependencies block",
                    severity = DiagnosticSeverity.INFO,
                    ruleId = "gradle-no-deps"
                )
            )
        }
    }

    private fun lintSourceCode(file: File, issues: MutableList<DiagnosticIssue>) {
        val lines = file.readText().lines()
        var braceBalance = 0
        var parenBalance = 0

        lines.forEachIndexed { index, line ->
            val clean = line.replace("//.*".toRegex(), "")
            braceBalance += clean.count { it == '{' } - clean.count { it == '}' }
            parenBalance += clean.count { it == '(' } - clean.count { it == ')' }

            if (braceBalance < 0) {
                issues.add(
                    DiagnosticIssue(
                        file = file,
                        line = index + 1,
                        message = "Unexpected closing brace '}'",
                        severity = DiagnosticSeverity.ERROR,
                        ruleId = "syntax-brace-mismatch"
                    )
                )
                braceBalance = 0
            }
            if (parenBalance < 0) {
                issues.add(
                    DiagnosticIssue(
                        file = file,
                        line = index + 1,
                        message = "Unexpected closing parenthesis ')'",
                        severity = DiagnosticSeverity.ERROR,
                        ruleId = "syntax-paren-mismatch"
                    )
                )
                parenBalance = 0
            }
        }

        if (braceBalance > 0) {
            issues.add(
                DiagnosticIssue(
                    file = file,
                    line = lines.size,
                    message = "Unclosed block: missing $braceBalance closing brace(s) '}'",
                    severity = DiagnosticSeverity.ERROR,
                    ruleId = "syntax-unclosed-braces"
                )
            )
        }
    }

    private fun lintXmlFile(file: File, issues: MutableList<DiagnosticIssue>) {
        val text = file.readText()
        val lines = text.lines()

        // Check root tag xmlns
        if (file.parentFile.name == "layout") {
            if (!text.contains("xmlns:android=\"http://schemas.android.com/apk/res/android\"")) {
                issues.add(
                    DiagnosticIssue(
                        file = file,
                        line = 1,
                        message = "Layout XML root element missing 'xmlns:android' namespace declaration",
                        severity = DiagnosticSeverity.WARNING,
                        ruleId = "xml-missing-xmlns",
                        quickFixText = "xmlns:android=\"http://schemas.android.com/apk/res/android\""
                    )
                )
            }
        }

        // Basic tag balance
        val tagPattern = "<([a-zA-Z0-9_:-]+)[^>]*>|<\\/([a-zA-Z0-9_:-]+)>".toRegex()
        val stack = Stack<String>()
        tagPattern.findAll(text).forEach { match ->
            val full = match.value
            if (full.endsWith("/>") || full.startsWith("<?") || full.startsWith("<!--")) {
                // self closing
                return@forEach
            }
            if (full.startsWith("</")) {
                val tag = match.groupValues[2]
                if (stack.isNotEmpty() && stack.peek() == tag) {
                    stack.pop()
                }
            } else {
                val tag = match.groupValues[1]
                stack.push(tag)
            }
        }

        if (stack.isNotEmpty()) {
            issues.add(
                DiagnosticIssue(
                    file = file,
                    line = lines.size,
                    message = "Unclosed XML tag: <${stack.peek()}>",
                    severity = DiagnosticSeverity.ERROR,
                    ruleId = "xml-unclosed-tag"
                )
            )
        }
    }
}
