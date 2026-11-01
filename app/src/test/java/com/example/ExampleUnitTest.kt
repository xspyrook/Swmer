package com.example

import com.example.editor.CodeSyntaxHighlighter
import com.example.editor.SyntaxLanguage
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun syntaxLanguageDetection_isCorrect() {
        assertEquals(SyntaxLanguage.KOTLIN, CodeSyntaxHighlighter.detectLanguage("MainActivity.kt"))
        assertEquals(SyntaxLanguage.XML, CodeSyntaxHighlighter.detectLanguage("activity_main.xml"))
        assertEquals(SyntaxLanguage.GRADLE, CodeSyntaxHighlighter.detectLanguage("build.gradle.kts"))
        assertEquals(SyntaxLanguage.JSON, CodeSyntaxHighlighter.detectLanguage("settings.json"))
    }

    @Test
    fun syntaxHighlighting_generatesAnnotatedString() {
        val code = "package com.example\nfun main() { val x = 42 }"
        val result = CodeSyntaxHighlighter.highlightCode(code, SyntaxLanguage.KOTLIN, isDark = true)
        assertEquals(code, result.text)
        assertTrue(result.spanStyles.isNotEmpty())
    }
}
