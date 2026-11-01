package com.example.apkmanager

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.model.ApkItem
import com.example.model.Project
import java.io.File

class ApkManagerService(private val context: Context) {

    fun scanApks(projects: List<Project>): List<ApkItem> {
        val list = mutableListOf<ApkItem>()

        projects.forEach { proj ->
            val apkDir = File(proj.rootDir, "build/outputs/apk")
            if (apkDir.exists()) {
                apkDir.walkTopDown().filter { it.isFile && it.extension.lowercase() == "apk" }.forEach { file ->
                    val variant = if (file.name.contains("release", ignoreCase = true)) "Release" else "Debug"
                    list.add(
                        ApkItem(
                            file = file,
                            name = file.name,
                            sizeBytes = file.length(),
                            lastModified = file.lastModified(),
                            buildVariant = variant,
                            packageName = proj.packageName
                        )
                    )
                }
            }
        }

        return list.sortedByDescending { it.lastModified }
    }

    fun installApk(file: File) {
        if (!file.exists()) return
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    }

    fun shareApk(file: File) {
        if (!file.exists()) return
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.android.package-archive"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        val chooser = Intent.createChooser(intent, "Share APK").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(chooser)
    }

    fun deleteApk(file: File): Boolean {
        return file.delete()
    }
}
