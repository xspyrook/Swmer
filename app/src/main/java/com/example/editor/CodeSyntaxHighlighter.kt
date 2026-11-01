package com.example.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import java.util.regex.Pattern

enum class SyntaxLanguage {
    KOTLIN, JAVA, XML, GRADLE, JSON, MARKDOWN, HTML, CSS, PLAIN
}

object CodeSyntaxHighlighter {

    // Dark IDE Theme Palette (Darcula / New UI)
    private val KeywordDark = Color(0xFFCC7832)
    private val TypeDark = Color(0xFF4EC9B0)
    private val StringDark = Color(0xFF6A8759)
    private val NumberDark = Color(0xFF6897BB)
    private val CommentDark = Color(0xFF808080)
    private val AnnotationDark = Color(0xFFBBB529)
    private val TagDark = Color(0xFFE8BF6A)
    private val AttributeDark = Color(0xFF9876AA)
    private val SearchMatchDark = Color(0xFF5E4B20)

    // Light Theme Palette
    private val KeywordLight = Color(0xFF0033B3)
    private val TypeLight = Color(0xFF00627A)
    private val StringLight = Color(0xFF067D17)
    private val NumberLight = Color(0xFF1750EB)
    private val CommentLight = Color(0xFF8C8C8C)
    private val AnnotationLight = Color(0xFF9E880D)
    private val TagLight = Color(0xFF000080)
    private val AttributeLight = Color(0xFF660E7A)
    private val SearchMatchLight = Color(0xFFFFEE55)

    fun detectLanguage(fileName: String): SyntaxLanguage {
        val lower = fileName.lowercase()
        if (lower.contains(".gradle") || lower.endsWith(".gradle")) {
            return SyntaxLanguage.GRADLE
        }
        val ext = lower.substringAfterLast('.', "")
        return when (ext) {
            "kt", "kts" -> SyntaxLanguage.KOTLIN
            "java" -> SyntaxLanguage.JAVA
            "xml" -> SyntaxLanguage.XML
            "gradle" -> SyntaxLanguage.GRADLE
            "json" -> SyntaxLanguage.JSON
            "md", "markdown" -> SyntaxLanguage.MARKDOWN
            "html", "htm" -> SyntaxLanguage.HTML
            "css" -> SyntaxLanguage.CSS
            else -> SyntaxLanguage.PLAIN
        }
    }

    fun highlightCode(
        code: String,
        language: SyntaxLanguage,
        isDark: Boolean,
        searchQuery: String = ""
    ): AnnotatedString {
        if (code.isEmpty()) return AnnotatedString("")
        val keywordColor = if (isDark) KeywordDark else KeywordLight
        val typeColor = if (isDark) TypeDark else TypeLight
        val stringColor = if (isDark) StringDark else StringLight
        val numberColor = if (isDark) NumberDark else NumberLight
        val commentColor = if (isDark) CommentDark else CommentLight
        val annotationColor = if (isDark) AnnotationDark else AnnotationLight
        val tagColor = if (isDark) TagDark else TagLight
        val attributeColor = if (isDark) AttributeDark else AttributeLight
        val searchColor = if (isDark) SearchMatchDark else SearchMatchLight

        return buildAnnotatedString {
            append(code)

            when (language) {
                SyntaxLanguage.KOTLIN, SyntaxLanguage.GRADLE, SyntaxLanguage.JAVA -> {
                    // Strings
                    highlightPattern(code, "\"(\\\\.|[^\"])*\"|'(\\\\.|[^'])*'", stringColor)
                    // Comments
                    highlightPattern(code, "//.*|/\\*[\\s\\S]*?\\*/", commentColor, italic = true)
                    // Annotations
                    highlightPattern(code, "@[A-Za-z0-9_]+", annotationColor)
                    // Keywords
                    val keywords = if (language == SyntaxLanguage.JAVA) {
                        "\\b(public|private|protected|class|interface|enum|extends|implements|static|final|void|int|boolean|float|double|char|long|byte|short|new|return|if|else|switch|case|default|break|continue|for|while|do|try|catch|finally|throw|throws|import|package|this|super)\\b"
                    } else {
                        "\\b(package|import|fun|val|var|class|interface|object|enum|sealed|data|override|private|protected|public|internal|if|else|when|for|while|return|break|continue|is|in|as|this|super|typealias|constructor|init|companion|by|get|set|try|catch|finally|throw)\\b"
                    }
                    highlightPattern(code, keywords, keywordColor, bold = true)
                    // Common types
                    highlightPattern(code, "\\b(String|Int|Boolean|Float|Double|Long|List|Map|Set|Unit|Any|Bundle|Activity|Context|Modifier|Composable|View|TextView|Button)\\b", typeColor)
                    // Numbers
                    highlightPattern(code, "\\b\\d+(\\.\\d+)?[fFL]?\\b", numberColor)
                }

                SyntaxLanguage.XML, SyntaxLanguage.HTML -> {
                    // Comments
                    highlightPattern(code, "<!--[\\s\\S]*?-->", commentColor, italic = true)
                    // Strings in attributes
                    highlightPattern(code, "\"([^\"]*)\"|'([^']*)'", stringColor)
                    // Tags
                    highlightPattern(code, "</?[a-zA-Z0-9_:-]+", tagColor, bold = true)
                    // Attribute names
                    highlightPattern(code, "[a-zA-Z0-9_:-]+(?=\\=)", attributeColor)
                }

                SyntaxLanguage.JSON -> {
                    // Strings
                    highlightPattern(code, "\"(\\\\.|[^\"])*\"", stringColor)
                    // Booleans / null
                    highlightPattern(code, "\\b(true|false|null)\\b", keywordColor, bold = true)
                    // Numbers
                    highlightPattern(code, "-?\\b\\d+(\\.\\d+)?\\b", numberColor)
                }

                SyntaxLanguage.MARKDOWN -> {
                    // Headers
                    highlightPattern(code, "(?m)^#{1,6}\\s+.*$", tagColor, bold = true)
                    // Code blocks
                    highlightPattern(code, "```[\\s\\S]*?```|`[^`]+`", stringColor)
                    // Links
                    highlightPattern(code, "\\[[^\\]]+\\]\\([^\\)]+\\)", keywordColor)
                }

                SyntaxLanguage.CSS -> {
                    highlightPattern(code, "/\\*[\\s\\S]*?\\*/", commentColor)
                    highlightPattern(code, "[.#]?[a-zA-Z0-9_-]+(?=\\s*\\{)", tagColor)
                    highlightPattern(code, "[a-zA-Z-]+(?=\\s*:)", attributeColor)
                    highlightPattern(code, ":\\s*([^;]+);", stringColor)
                }

                SyntaxLanguage.PLAIN -> {
                    // Plain text
                }
            }

            // Search query highlight
            if (searchQuery.isNotBlank() && searchQuery.length >= 2) {
                try {
                    val p = Pattern.compile(Pattern.quote(searchQuery), Pattern.CASE_INSENSITIVE)
                    val m = p.matcher(code)
                    while (m.find()) {
                        addStyle(
                            SpanStyle(background = searchColor, fontWeight = FontWeight.Bold),
                            m.start(),
                            m.end()
                        )
                    }
                } catch (_: Exception) {}
            }
        }
    }

    private fun AnnotatedString.Builder.highlightPattern(
        code: String,
        regexPattern: String,
        color: Color,
        bold: Boolean = false,
        italic: Boolean = false
    ) {
        try {
            val pattern = Pattern.compile(regexPattern)
            val matcher = pattern.matcher(code)
            while (matcher.find()) {
                addStyle(
                    SpanStyle(
                        color = color,
                        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
                        fontFamily = FontFamily.Monospace
                    ),
                    matcher.start(),
                    matcher.end()
                )
            }
        } catch (_: Exception) {}
    }
}
