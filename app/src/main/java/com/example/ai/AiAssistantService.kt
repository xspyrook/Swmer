package com.example.ai

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AiMessage(
    val sender: String, // "user" or "assistant"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val proposedCode: String? = null
)

class AiAssistantService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    fun getApiKey(userCustomKey: String?): String {
        if (!userCustomKey.isNullOrBlank()) return userCustomKey.trim()
        return try {
            val field = BuildConfig::class.java.getField("GEMINI_API_KEY")
            val value = field.get(null) as? String
            if (value.isNullOrBlank() || value == "MY_GEMINI_API_KEY") "" else value
        } catch (_: Exception) {
            ""
        }
    }

    suspend fun sendMessage(
        prompt: String,
        contextCode: String = "",
        userCustomKey: String? = null,
        languageArabic: Boolean = true
    ): AiMessage = withContext(Dispatchers.IO) {
        val apiKey = getApiKey(userCustomKey)

        if (apiKey.isNotBlank()) {
            try {
                return@withContext callGeminiApi(prompt, contextCode, apiKey, languageArabic)
            } catch (e: Exception) {
                // Fallback to offline rule-based assistant
                return@withContext generateOfflineResponse(prompt, contextCode, languageArabic, fallbackError = e.message)
            }
        } else {
            return@withContext generateOfflineResponse(prompt, contextCode, languageArabic)
        }
    }

    private fun callGeminiApi(
        prompt: String,
        contextCode: String,
        apiKey: String,
        languageArabic: Boolean
    ): AiMessage {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"

        val systemInstruction = if (languageArabic) {
            "أنت مهندس ومساعد برمجي متقدم لبيئة تطوير Android Studio Mobile IDE. أجب بدقة باللغة العربية مع شروحات برمجية وكود Kotlin/XML عند الحاجة. إذا اقترحت كوداً ضعه داخل كتل ```kotlin أو ```xml."
        } else {
            "You are an expert Android developer assistant in Android Studio Mobile IDE. Provide precise Kotlin and XML solutions with explanation."
        }

        val fullText = buildString {
            append(systemInstruction)
            append("\n\n")
            if (contextCode.isNotBlank()) {
                append("Project / File Context:\n```\n")
                append(contextCode.take(2000))
                append("\n```\n\n")
            }
            append("User Query: ")
            append(prompt)
        }

        val json = JSONObject().apply {
            val contents = JSONArray()
            val contentObj = JSONObject().apply {
                val parts = JSONArray()
                parts.put(JSONObject().put("text", fullText))
                put("parts", parts)
            }
            contents.put(contentObj)
            put("contents", contents)
        }

        val requestBody = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw IllegalStateException("Empty response")

        if (!response.isSuccessful) {
            throw IllegalStateException("API error: ${response.code}")
        }

        val resJson = JSONObject(responseBody)
        val text = resJson.getJSONArray("candidates")
            .getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
            .getJSONObject(0)
            .getString("text")

        val extractedCode = extractCodeBlock(text)
        return AiMessage(sender = "assistant", content = text, proposedCode = extractedCode)
    }

    private fun generateOfflineResponse(
        prompt: String,
        contextCode: String,
        languageArabic: Boolean,
        fallbackError: String? = null
    ): AiMessage {
        val lower = prompt.lowercase()

        val responseText: String
        var code: String? = null

        if (lower.contains("compose") || lower.contains("واجهة") || lower.contains("ui")) {
            responseText = if (languageArabic) {
                "إليك نموذج واجهة Jetpack Compose حديثة مع Material 3 تتضمن بطاقة وحقل إدخال وزر تفاعلي:"
            } else {
                "Here is a modern Jetpack Compose Material 3 UI component:"
            }
            code = """
                @Composable
                fun CustomCardScreen() {
                    var text by remember { mutableStateOf("") }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("بطاقة تفاعلية", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = text,
                                onValueChange = { text = it },
                                label = { Text("أدخل النص") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = { /* Action */ }) {
                                Text("تأكيد")
                            }
                        }
                    }
                }
            """.trimIndent()
        } else if (lower.contains("gradle") || lower.contains("dependency") || lower.contains("اعتماد")) {
            responseText = if (languageArabic) {
                "لإضافة مكتبات إلى ملف `app/build.gradle.kts`، يمكنك إضافة الأسطر التالية داخل كتلة `dependencies`:"
            } else {
                "To add dependencies to `app/build.gradle.kts`, use:"
            }
            code = """
                dependencies {
                    // Jetpack Compose & Navigation
                    implementation("androidx.navigation:navigation-compose:2.8.9")
                    // Networking with Retrofit & OkHttp
                    implementation("com.squareup.retrofit2:retrofit:2.12.0")
                    implementation("com.squareup.okhttp3:okhttp:4.12.0")
                    // Image Loading with Coil
                    implementation("io.coil-kt:coil-compose:2.7.0")
                }
            """.trimIndent()
        } else if (lower.contains("error") || lower.contains("خطأ") || lower.contains("مترجم") || lower.contains("build")) {
            responseText = if (languageArabic) {
                "عند حدوث خطأ في البناء:\n1. تأكد من توازن الأقواس { } و ( ) في جميع ملفات Kotlin.\n2. تحقق من أن ملف AndroidManifest.xml يحتوي على خاصية android:exported=\"true\" للأنشطة الرئيسية.\n3. تحقق من وجود معرفات الحزم الصحيحة في build.gradle.kts."
            } else {
                "Build troubleshooting tips:\n1. Verify matching curly braces { } in Kotlin sources.\n2. Ensure AndroidManifest.xml specifies android:exported=\"true\" on launcher activity.\n3. Check dependencies syntax in build.gradle.kts."
            }
        } else {
            responseText = if (languageArabic) {
                "أهلاً بك في مساعد Android Studio Mobile الذكي! يمكنك سؤالي عن شرح الكود، إنشاء واجهات Jetpack Compose، إصلاح أخطاء البناء، أو توليد إعدادات Gradle.\n(ملاحظة: يمكنك إدخال مفتاح Gemini API الخاص بك من الإعدادات للتمتع بكامل قدرات الذكاء الاصطناعي السحابي)."
            } else {
                "Welcome to Android Studio Mobile AI Assistant! Ask me to explain code, generate Compose UI, fix build issues, or add Gradle libraries. (You can also configure your Gemini API key in Settings for full cloud AI)."
            }
        }

        val note = if (fallbackError != null) "\n\n[وضع العمل المحلي: تعذر الاتصال بـ API: $fallbackError]" else ""
        return AiMessage(sender = "assistant", content = responseText + note, proposedCode = code)
    }

    private fun extractCodeBlock(text: String): String? {
        val pattern = "```(?:kotlin|xml|java|gradle)?([\\s\\S]*?)```".toRegex()
        val match = pattern.find(text)
        return match?.groups?.get(1)?.value?.trim()
    }
}
