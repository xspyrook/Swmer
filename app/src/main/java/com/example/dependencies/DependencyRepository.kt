package com.example.dependencies

import com.example.model.DependencyItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class DependencyRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    val popularLibraries: List<DependencyItem> = listOf(
        DependencyItem("androidx.compose.material3", "material3", "1.3.1", "implementation", "Material 3 UI components for Compose"),
        DependencyItem("androidx.navigation", "navigation-compose", "2.8.9", "implementation", "Jetpack Navigation for Compose"),
        DependencyItem("androidx.room", "room-runtime", "2.7.0", "implementation", "SQLite object mapping library"),
        DependencyItem("androidx.room", "room-ktx", "2.7.0", "implementation", "Kotlin Coroutines & Flow extensions for Room"),
        DependencyItem("com.squareup.retrofit2", "retrofit", "2.12.0", "implementation", "Type-safe HTTP client for Android and Java"),
        DependencyItem("com.squareup.retrofit2", "converter-moshi", "2.12.0", "implementation", "Moshi converter for Retrofit"),
        DependencyItem("com.squareup.okhttp3", "okhttp", "4.12.0", "implementation", "Efficient HTTP & HTTP/2 client"),
        DependencyItem("com.squareup.okhttp3", "logging-interceptor", "4.12.0", "implementation", "OkHttp request logging interceptor"),
        DependencyItem("io.coil-kt", "coil-compose", "2.7.0", "implementation", "Image loading for Jetpack Compose"),
        DependencyItem("org.jetbrains.kotlinx", "kotlinx-coroutines-android", "1.10.2", "implementation", "Kotlin Coroutines support for Android UI"),
        DependencyItem("androidx.lifecycle", "lifecycle-viewmodel-compose", "2.8.7", "implementation", "ViewModel integration for Jetpack Compose"),
        DependencyItem("androidx.datastore", "datastore-preferences", "1.1.7", "implementation", "Key-value data storage solution"),
        DependencyItem("com.google.android.material", "material", "1.12.0", "implementation", "Modular and customizable Material Design components")
    )

    fun parseProjectDependencies(appGradleFile: File): List<DependencyItem> {
        if (!appGradleFile.exists()) return emptyList()
        val text = appGradleFile.readText()
        val list = mutableListOf<DependencyItem>()

        // Find dependencies { ... } block
        val depIndex = text.indexOf("dependencies {")
        if (depIndex == -1) return emptyList()
        val block = text.substring(depIndex)

        // Regex for implementation("group:name:version") or implementation(libs....)
        val pattern = Pattern.compile("(implementation|api|ksp|testImplementation)\\s*\\([\"']([^:\"']+):([^:\"']+)(?::([^\"']+))?[\"']\\)")
        val matcher = pattern.matcher(block)
        while (matcher.find()) {
            val scope = matcher.group(1) ?: "implementation"
            val group = matcher.group(2) ?: ""
            val name = matcher.group(3) ?: ""
            val version = matcher.group(4) ?: ""
            list.add(
                DependencyItem(
                    group = group,
                    name = name,
                    version = version,
                    configuration = scope,
                    isOfficial = group.startsWith("androidx") || group.startsWith("com.google")
                )
            )
        }
        return list
    }

    suspend fun searchMavenCentral(query: String): List<DependencyItem> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        try {
            val url = "https://search.maven.org/solrsearch/select?q=${query.trim()}&rows=12&wt=json"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()

            val body = response.body?.string() ?: return@withContext emptyList()
            val json = JSONObject(body)
            val docs = json.getJSONObject("response").getJSONArray("docs")
            val results = mutableListOf<DependencyItem>()

            for (i in 0 until docs.length()) {
                val doc = docs.getJSONObject(i)
                val g = doc.optString("g", "")
                val a = doc.optString("a", "")
                val v = doc.optString("latestVersion", "1.0.0")
                if (g.isNotEmpty() && a.isNotEmpty()) {
                    results.add(
                        DependencyItem(
                            group = g,
                            name = a,
                            version = v,
                            description = "Maven Central artifact: $g:$a"
                        )
                    )
                }
            }
            results
        } catch (_: Exception) {
            // Fallback to local filter of popular libraries if offline
            popularLibraries.filter {
                it.name.contains(query, ignoreCase = true) || it.group.contains(query, ignoreCase = true)
            }
        }
    }

    fun addDependencyToGradle(appGradleFile: File, item: DependencyItem): Boolean {
        if (!appGradleFile.exists()) return false
        val content = appGradleFile.readText()
        val depLine = if (item.version.isNotBlank()) {
            "    ${item.configuration}(\"${item.group}:${item.name}:${item.version}\")\n"
        } else {
            "    ${item.configuration}(\"${item.group}:${item.name}\")\n"
        }

        // Check if already present
        if (content.contains("${item.group}:${item.name}")) {
            return false
        }

        val depIndex = content.lastIndexOf("dependencies {")
        if (depIndex == -1) return false
        val insertPos = depIndex + "dependencies {".length

        val updated = content.substring(0, insertPos) + "\n" + depLine + content.substring(insertPos)
        appGradleFile.writeText(updated)
        return true
    }

    fun removeDependencyFromGradle(appGradleFile: File, item: DependencyItem): Boolean {
        if (!appGradleFile.exists()) return false
        val content = appGradleFile.readText()
        val lines = content.lines().filterNot { line ->
            line.contains(item.group) && line.contains(item.name)
        }
        appGradleFile.writeText(lines.joinToString("\n"))
        return true
    }
}
