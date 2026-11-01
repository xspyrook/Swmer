package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.FileProvider
import com.example.model.Project
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.IdeScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.StudioViewModel
import java.io.File

class MainActivity : ComponentActivity() {

    private val viewModel: StudioViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val currentProject by viewModel.currentProject.collectAsState()
                    val projects by viewModel.projects.collectAsState()
                    val customApiKey by viewModel.customApiKey.collectAsState()

                    if (currentProject != null) {
                        IdeScreen(
                            viewModel = viewModel,
                            project = currentProject!!,
                            onNavigateBack = { viewModel.closeProject() },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        DashboardScreen(
                            projects = projects,
                            customApiKey = customApiKey,
                            onOpenProject = { project -> viewModel.openProject(project) },
                            onCreateProject = { name, pkg, template, minSdk ->
                                viewModel.createNewProject(name, pkg, template, minSdk)
                            },
                            onDeleteProject = { project -> viewModel.deleteProject(project) },
                            onExportProjectZip = { project -> exportProjectZip(project) },
                            onImportProjectZip = { uri, name -> importProjectZip(uri, name) },
                            onSaveCustomApiKey = { key -> viewModel.setCustomApiKey(key) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }

    private fun exportProjectZip(project: Project) {
        val zipFile = File(cacheDir, "${project.name}.zip")
        val ok = viewModel.fileManager.exportProjectToZip(project, zipFile)
        if (ok) {
            try {
                val uri: Uri = FileProvider.getUriForFile(
                    this,
                    "$packageName.fileprovider",
                    zipFile
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/zip"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "${project.name} Source Code")
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                startActivity(Intent.createChooser(intent, "تصدير المشروع"))
            } catch (e: Exception) {
                Toast.makeText(this, "تعذر مشاركة ملف ZIP: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, "فشل ضغط ملفات المشروع", Toast.LENGTH_SHORT).show()
        }
    }

    private fun importProjectZip(uri: Uri, name: String) {
        try {
            contentResolver.openInputStream(uri)?.use { stream ->
                val project = viewModel.fileManager.importProjectFromZip(stream, name)
                if (project != null) {
                    viewModel.loadProjects()
                    viewModel.openProject(project)
                    Toast.makeText(this, "تم استيراد المشروع بنجاح!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "فشل استيراد المشروع من ملف ZIP", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "خطأ في قراءة ملف ZIP: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
