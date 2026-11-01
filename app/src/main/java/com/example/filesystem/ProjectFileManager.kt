package com.example.filesystem

import android.content.Context
import com.example.model.FileNode
import com.example.model.Project
import com.example.model.TemplateType
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class ProjectFileManager(private val context: Context) {

    private val projectsRoot: File
        get() = File(context.filesDir, "projects").apply { if (!exists()) mkdirs() }

    private val metadataFile: File
        get() = File(projectsRoot, "projects_metadata.json")

    fun getAllProjects(): List<Project> {
        val list = mutableListOf<Project>()
        if (!metadataFile.exists()) {
            return list
        }
        try {
            val jsonStr = metadataFile.readText()
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val dir = File(obj.getString("path"))
                if (dir.exists()) {
                    list.add(
                        Project(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            packageName = obj.getString("packageName"),
                            rootDir = dir,
                            templateType = runCatching {
                                TemplateType.valueOf(obj.getString("templateType"))
                            }.getOrDefault(TemplateType.COMPOSE),
                            minSdk = obj.optInt("minSdk", 26),
                            targetSdk = obj.optInt("targetSdk", 36),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                            lastModifiedAt = obj.optLong("lastModifiedAt", System.currentTimeMillis())
                        )
                    )
                }
            }
        } catch (_: Exception) {
            // Ignore corrupted metadata and scan folders
        }

        // If list is empty but directories exist, recover them
        if (list.isEmpty()) {
            projectsRoot.listFiles { f -> f.isDirectory }?.forEach { dir ->
                val proj = Project(
                    id = dir.name,
                    name = dir.name,
                    packageName = "com.my." + dir.name.lowercase().replace("[^a-z0-9]".toRegex(), ""),
                    rootDir = dir,
                    templateType = TemplateType.COMPOSE
                )
                list.add(proj)
            }
            saveProjectsMetadata(list)
        }
        return list.sortedByDescending { it.lastModifiedAt }
    }

    fun saveProjectsMetadata(projects: List<Project>) {
        try {
            val array = JSONArray()
            projects.forEach { p ->
                val obj = JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("packageName", p.packageName)
                    put("path", p.rootDir.absolutePath)
                    put("templateType", p.templateType.name)
                    put("minSdk", p.minSdk)
                    put("targetSdk", p.targetSdk)
                    put("createdAt", p.createdAt)
                    put("lastModifiedAt", p.lastModifiedAt)
                }
                array.put(obj)
            }
            metadataFile.writeText(array.toString(2))
        } catch (_: Exception) {
        }
    }

    fun createProject(
        name: String,
        packageName: String,
        templateType: TemplateType,
        minSdk: Int = 26
    ): Project {
        val safeDirName = name.replace("[^a-zA-Z0-9_]".toRegex(), "_")
        var projectDir = File(projectsRoot, safeDirName)
        var count = 1
        while (projectDir.exists()) {
            projectDir = File(projectsRoot, "${safeDirName}_$count")
            count++
        }
        projectDir.mkdirs()

        val project = Project(
            id = UUID.randomUUID().toString(),
            name = name,
            packageName = packageName,
            rootDir = projectDir,
            templateType = templateType,
            minSdk = minSdk,
            targetSdk = 36
        )

        // Scaffold standard Android Gradle structure
        scaffoldProjectStructure(project)

        val existing = getAllProjects().toMutableList()
        existing.add(0, project)
        saveProjectsMetadata(existing)

        return project
    }

    fun updateProjectTimestamp(projectId: String) {
        val projects = getAllProjects().map {
            if (it.id == projectId) it.copy(lastModifiedAt = System.currentTimeMillis()) else it
        }
        saveProjectsMetadata(projects)
    }

    fun deleteProject(project: Project): Boolean {
        val deleted = project.rootDir.deleteRecursively()
        val projects = getAllProjects().filter { it.id != project.id }
        saveProjectsMetadata(projects)
        return deleted
    }

    private fun scaffoldProjectStructure(project: Project) {
        val root = project.rootDir
        val pkgPath = project.packageName.replace('.', '/')

        // Root files
        File(root, "settings.gradle.kts").writeText(
            """
            pluginManagement {
                repositories {
                    google()
                    mavenCentral()
                    gradlePluginPortal()
                }
            }
            dependencyResolutionManagement {
                repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
                repositories {
                    google()
                    mavenCentral()
                }
            }
            rootProject.name = "${project.name}"
            include(":app")
            """.trimIndent()
        )

        File(root, "build.gradle.kts").writeText(
            """
            // Top-level build file
            plugins {
                id("com.android.application") version "8.7.0" apply false
                id("org.jetbrains.kotlin.android") version "2.0.21" apply false
                id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
            }
            """.trimIndent()
        )

        File(root, "gradle.properties").writeText(
            """
            org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
            android.useAndroidX=true
            android.nonTransitiveRClass=true
            kotlin.code.style=official
            """.trimIndent()
        )

        val appDir = File(root, "app").apply { mkdirs() }
        val mainDir = File(appDir, "src/main").apply { mkdirs() }
        val resDir = File(mainDir, "res").apply { mkdirs() }
        val valuesDir = File(resDir, "values").apply { mkdirs() }

        // Strings
        File(valuesDir, "strings.xml").writeText(
            """
            <resources>
                <string name="app_name">${project.name}</string>
                <string name="welcome_message">Welcome to ${project.name} built with Android Studio Mobile!</string>
            </resources>
            """.trimIndent()
        )

        // Colors
        File(valuesDir, "colors.xml").writeText(
            """
            <resources>
                <color name="primary">#00875A</color>
                <color name="primary_variant">#006644</color>
                <color name="secondary">#00B8D9</color>
                <color name="background">#FAFBFC</color>
            </resources>
            """.trimIndent()
        )

        // Manifest
        File(mainDir, "AndroidManifest.xml").writeText(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                package="${project.packageName}">

                <application
                    android:allowBackup="true"
                    android:icon="@mipmap/ic_launcher"
                    android:label="@string/app_name"
                    android:roundIcon="@mipmap/ic_launcher_round"
                    android:supportsRtl="true"
                    android:theme="@android:style/Theme.Material.Light.NoActionBar">
                    
                    <activity
                        android:name=".MainActivity"
                        android:exported="true">
                        <intent-filter>
                            <action android:name="android.intent.action.MAIN" />
                            <category android:name="android.intent.category.LAUNCHER" />
                        </intent-filter>
                    </activity>
                </application>
            </manifest>
            """.trimIndent()
        )

        when (project.templateType) {
            TemplateType.COMPOSE -> {
                val javaDir = File(mainDir, "java/$pkgPath").apply { mkdirs() }
                File(appDir, "build.gradle.kts").writeText(
                    """
                    plugins {
                        id("com.android.application")
                        id("org.jetbrains.kotlin.android")
                        id("org.jetbrains.kotlin.plugin.compose")
                    }

                    android {
                        namespace = "${project.packageName}"
                        compileSdk = ${project.targetSdk}

                        defaultConfig {
                            applicationId = "${project.packageName}"
                            minSdk = ${project.minSdk}
                            targetSdk = ${project.targetSdk}
                            versionCode = 1
                            versionName = "1.0"
                        }

                        buildFeatures {
                            compose = true
                        }

                        compileOptions {
                            sourceCompatibility = JavaVersion.VERSION_17
                            targetCompatibility = JavaVersion.VERSION_17
                        }
                    }

                    dependencies {
                        implementation(platform("androidx.compose:compose-bom:2024.09.00"))
                        implementation("androidx.compose.ui:ui")
                        implementation("androidx.compose.material3:material3")
                        implementation("androidx.activity:activity-compose:1.9.2")
                        implementation("androidx.core:core-ktx:1.13.1")
                    }
                    """.trimIndent()
                )

                File(javaDir, "MainActivity.kt").writeText(
                    """
                    package ${project.packageName}

                    import android.os.Bundle
                    import androidx.activity.ComponentActivity
                    import androidx.activity.compose.setContent
                    import androidx.compose.foundation.layout.*
                    import androidx.compose.material3.*
                    import androidx.compose.runtime.*
                    import androidx.compose.ui.Alignment
                    import androidx.compose.ui.Modifier
                    import androidx.compose.ui.unit.dp

                    class MainActivity : ComponentActivity() {
                        override fun onCreate(savedInstanceState: Bundle?) {
                            super.onCreate(savedInstanceState)
                            setContent {
                                MaterialTheme {
                                    Surface(
                                        modifier = Modifier.fillMaxSize(),
                                        color = MaterialTheme.colorScheme.background
                                    ) {
                                        MainScreen()
                                    }
                                }
                            }
                        }
                    }

                    @Composable
                    fun MainScreen() {
                        var count by remember { mutableStateOf(0) }
                        
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Hello from ${project.name}!",
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Developed entirely on Android with AS Mobile IDE.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(onClick = { count++ }) {
                                Text("Clicks: ${'$'}count")
                            }
                        }
                    }
                    """.trimIndent()
                )
            }
            TemplateType.XML_VIEW -> {
                val layoutDir = File(resDir, "layout").apply { mkdirs() }
                val javaDir = File(mainDir, "java/$pkgPath").apply { mkdirs() }

                File(appDir, "build.gradle.kts").writeText(
                    """
                    plugins {
                        id("com.android.application")
                        id("org.jetbrains.kotlin.android")
                    }

                    android {
                        namespace = "${project.packageName}"
                        compileSdk = ${project.targetSdk}

                        defaultConfig {
                            applicationId = "${project.packageName}"
                            minSdk = ${project.minSdk}
                            targetSdk = ${project.targetSdk}
                            versionCode = 1
                            versionName = "1.0"
                        }

                        buildFeatures {
                            viewBinding = true
                        }
                    }

                    dependencies {
                        implementation("androidx.core:core-ktx:1.13.1")
                        implementation("androidx.appcompat:appcompat:1.7.0")
                        implementation("com.google.android.material:material:1.12.0")
                        implementation("androidx.constraintlayout:constraintlayout:2.1.4")
                    }
                    """.trimIndent()
                )

                File(layoutDir, "activity_main.xml").writeText(
                    """
                    <?xml version="1.0" encoding="utf-8"?>
                    <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
                        android:layout_width="match_parent"
                        android:layout_height="match_parent"
                        android:orientation="vertical"
                        android:gravity="center"
                        android:padding="24dp">

                        <TextView
                            android:id="@+id/titleText"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="@string/welcome_message"
                            android:textSize="20sp"
                            android:textStyle="bold"
                            android:gravity="center" />

                        <Button
                            android:id="@+id/actionButton"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:layout_marginTop="24dp"
                            android:text="Click Me" />
                    </LinearLayout>
                    """.trimIndent()
                )

                File(javaDir, "MainActivity.kt").writeText(
                    """
                    package ${project.packageName}

                    import android.os.Bundle
                    import android.widget.Button
                    import android.widget.TextView
                    import android.widget.Toast
                    import androidx.appcompat.app.AppCompatActivity

                    class MainActivity : AppCompatActivity() {
                        private var counter = 0

                        override fun onCreate(savedInstanceState: Bundle?) {
                            super.onCreate(savedInstanceState)
                            setContentView(R.layout.activity_main)

                            val title = findViewById<TextView>(R.id.titleText)
                            val button = findViewById<Button>(R.id.actionButton)

                            button.setOnClickListener {
                                counter++
                                title.text = "Clicked ${'$'}counter times!"
                                Toast.makeText(this, "Button Clicked!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    """.trimIndent()
                )
            }
            TemplateType.JAVA_XML -> {
                val layoutDir = File(resDir, "layout").apply { mkdirs() }
                val javaDir = File(mainDir, "java/$pkgPath").apply { mkdirs() }

                File(appDir, "build.gradle.kts").writeText(
                    """
                    plugins {
                        id("com.android.application")
                    }

                    android {
                        namespace = "${project.packageName}"
                        compileSdk = ${project.targetSdk}

                        defaultConfig {
                            applicationId = "${project.packageName}"
                            minSdk = ${project.minSdk}
                            targetSdk = ${project.targetSdk}
                            versionCode = 1
                            versionName = "1.0"
                        }
                    }

                    dependencies {
                        implementation("androidx.appcompat:appcompat:1.7.0")
                        implementation("com.google.android.material:material:1.12.0")
                    }
                    """.trimIndent()
                )

                File(layoutDir, "activity_main.xml").writeText(
                    """
                    <?xml version="1.0" encoding="utf-8"?>
                    <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
                        android:layout_width="match_parent"
                        android:layout_height="match_parent"
                        android:orientation="vertical"
                        android:gravity="center">

                        <TextView
                            android:id="@+id/javaText"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="Java Android Project"
                            android:textSize="22sp" />
                    </LinearLayout>
                    """.trimIndent()
                )

                File(javaDir, "MainActivity.java").writeText(
                    """
                    package ${project.packageName};

                    import android.os.Bundle;
                    import android.widget.TextView;
                    import androidx.appcompat.app.AppCompatActivity;

                    public class MainActivity extends AppCompatActivity {
                        @Override
                        protected void onCreate(Bundle savedInstanceState) {
                            super.onCreate(savedInstanceState);
                            setContentView(R.layout.activity_main);
                            
                            TextView text = findViewById(R.id.javaText);
                            text.setText("Hello from Java Android App!");
                        }
                    }
                    """.trimIndent()
                )
            }
            TemplateType.EMPTY -> {
                File(appDir, "build.gradle.kts").writeText(
                    """
                    plugins {
                        id("com.android.application")
                        id("org.jetbrains.kotlin.android")
                    }

                    android {
                        namespace = "${project.packageName}"
                        compileSdk = ${project.targetSdk}
                    }
                    """.trimIndent()
                )
            }
        }
    }

    fun buildFileTree(rootDir: File, expandedPaths: Set<String> = emptySet()): FileNode {
        return buildNodeRecursive(rootDir, expandedPaths)
    }

    private fun buildNodeRecursive(file: File, expandedPaths: Set<String>): FileNode {
        if (!file.isDirectory) {
            return FileNode(file = file, isDirectory = false)
        }

        val isExpanded = expandedPaths.contains(file.absolutePath)
        val children = if (isExpanded || file.parentFile == projectsRoot || file.name == "app" || file.name == "src") {
            file.listFiles()?.map { child ->
                buildNodeRecursive(child, expandedPaths)
            }?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })) ?: emptyList()
        } else {
            emptyList()
        }

        return FileNode(
            file = file,
            isDirectory = true,
            children = children,
            isExpanded = isExpanded
        )
    }

    fun readFile(file: File): String {
        return if (file.exists() && file.isFile) file.readText() else ""
    }

    fun writeFile(file: File, content: String): Boolean {
        return try {
            file.parentFile?.mkdirs()
            file.writeText(content)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun createNewFile(parentDir: File, name: String): File? {
        return try {
            val file = File(parentDir, name)
            if (file.createNewFile()) file else null
        } catch (_: Exception) {
            null
        }
    }

    fun createNewFolder(parentDir: File, name: String): File? {
        return try {
            val dir = File(parentDir, name)
            if (dir.mkdirs()) dir else null
        } catch (_: Exception) {
            null
        }
    }

    fun deleteFile(file: File): Boolean {
        return if (file.isDirectory) file.deleteRecursively() else file.delete()
    }

    fun renameFile(file: File, newName: String): File? {
        val target = File(file.parentFile, newName)
        return if (file.renameTo(target)) target else null
    }

    fun exportProjectToZip(project: Project, outputZipFile: File): Boolean {
        return try {
            outputZipFile.parentFile?.mkdirs()
            ZipOutputStream(BufferedOutputStream(FileOutputStream(outputZipFile))).use { zos ->
                val basePath = project.rootDir.absolutePath
                project.rootDir.walkTopDown().forEach { file ->
                    val relativePath = file.absolutePath.removePrefix(basePath).removePrefix("/")
                    if (relativePath.isNotEmpty()) {
                        if (file.isDirectory) {
                            zos.putNextEntry(ZipEntry("$relativePath/"))
                            zos.closeEntry()
                        } else {
                            zos.putNextEntry(ZipEntry(relativePath))
                            FileInputStream(file).use { fis ->
                                fis.copyTo(zos)
                            }
                            zos.closeEntry()
                        }
                    }
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun importProjectFromZip(inputStream: InputStream, projectName: String): Project? {
        return try {
            val safeName = projectName.replace("[^a-zA-Z0-9_]".toRegex(), "_")
            val targetDir = File(projectsRoot, safeName).apply { mkdirs() }
            ZipInputStream(BufferedInputStream(inputStream)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val file = File(targetDir, entry.name)
                    if (entry.isDirectory) {
                        file.mkdirs()
                    } else {
                        file.parentFile?.mkdirs()
                        FileOutputStream(file).use { fos ->
                            zis.copyTo(fos)
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            val proj = Project(
                id = UUID.randomUUID().toString(),
                name = projectName,
                packageName = "com.imported." + safeName.lowercase(),
                rootDir = targetDir,
                templateType = TemplateType.COMPOSE
            )
            val list = getAllProjects().toMutableList()
            list.add(0, proj)
            saveProjectsMetadata(list)
            proj
        } catch (_: Exception) {
            null
        }
    }
}
