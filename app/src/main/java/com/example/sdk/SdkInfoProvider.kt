package com.example.sdk

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import com.example.model.SdkInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class SdkInfoProvider(private val context: Context) {

    fun getSdkInfo(): SdkInfo {
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager?.getMemoryInfo(memInfo)

        val totalRamMb = (memInfo.totalMem / (1024 * 1024))
        val availRamMb = (memInfo.availMem / (1024 * 1024))

        val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown"

        return SdkInfo(
            androidVersion = "Android ${Build.VERSION.RELEASE}",
            apiLevel = Build.VERSION.SDK_INT,
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
            architecture = abi,
            totalRamMb = totalRamMb,
            availableRamMb = availRamMb,
            kotlinVersion = "2.2.10",
            gradleVersion = "9.1.1",
            agpVersion = "9.1.1",
            targetPlatform = "Android SDK 36 (VanillaIceCream)"
        )
    }

    suspend fun checkRepoConnectivity(repoUrl: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL(repoUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            conn.requestMethod = "HEAD"
            val code = conn.responseCode
            code in 200..399
        } catch (_: Exception) {
            false
        }
    }
}
