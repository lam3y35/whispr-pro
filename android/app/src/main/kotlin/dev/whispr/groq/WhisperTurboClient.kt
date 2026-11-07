package dev.whispr.groq

import android.content.Context
import org.json.JSONObject
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/**
 * Client for Whisper Large-v3-Turbo (Groq / OpenAI API).
 * Provides lightning-fast (0.15s), human-accuracy speech recognition
 * for conversational Arabic (Egyptian, Gulf, Levantine, Maghrebi), English,
 * and mixed bilingual speech with automated native punctuation.
 */
object WhisperTurboClient {

    private const val GROQ_ENDPOINT = "https://api.groq.com/openai/v1/audio/transcriptions"
    private const val OPENAI_ENDPOINT = "https://api.openai.com/v1/audio/transcriptions"

    fun getApiKey(context: Context): String {
        val prefs = context.getSharedPreferences("whispr_pro_prefs", Context.MODE_PRIVATE)
        return prefs.getString("whisper_api_key", "")?.trim().orEmpty()
    }

    fun setApiKey(context: Context, key: String) {
        val prefs = context.getSharedPreferences("whispr_pro_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("whisper_api_key", key.trim()).apply()
    }

    fun hasApiKey(context: Context): Boolean {
        return getApiKey(context).isNotEmpty()
    }

    fun getModelPreference(context: Context): String {
        val prefs = context.getSharedPreferences("whispr_pro_prefs", Context.MODE_PRIVATE)
        return prefs.getString("model_preference", "whisper-large-v3") ?: "whisper-large-v3"
    }

    fun setModelPreference(context: Context, pref: String) {
        val prefs = context.getSharedPreferences("whispr_pro_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("model_preference", pref).apply()
    }

    fun getEngineMode(context: Context): String {
        val prefs = context.getSharedPreferences("whispr_pro_prefs", Context.MODE_PRIVATE)
        return prefs.getString("engine_mode", "cloud") ?: "cloud"
    }

    fun setEngineMode(context: Context, mode: String) {
        val prefs = context.getSharedPreferences("whispr_pro_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("engine_mode", mode).apply()
    }

    sealed class Result {
        data class Success(val text: String) : Result()
        data class Error(val message: String, val code: Int? = null) : Result()
    }

    /**
     * Uploads the recorded audio file to Whisper Large-v3-Turbo and returns the transcribed text.
     */
    fun transcribe(
        context: Context,
        audioFile: File,
        language: String = "auto"
    ): Result {
        val apiKey = getApiKey(context)
        if (apiKey.isEmpty()) {
            return Result.Error("API Key missing. Please paste your free Groq API key in Whispr Pro.")
        }

        val isGroq = apiKey.startsWith("gsk_") || !apiKey.startsWith("sk-")
        val endpointUrl = if (isGroq) GROQ_ENDPOINT else OPENAI_ENDPOINT
        val modelPref = getModelPreference(context)
        val model = if (isGroq) {
            if (modelPref == "whisper-large-v3" || modelPref == "large_v3") "whisper-large-v3" else "whisper-large-v3-turbo"
        } else {
            "whisper-1"
        }

        val boundary = "----WhisprFormBoundary${System.currentTimeMillis()}"
        val lineEnd = "\r\n"
        val twoHyphens = "--"

        var conn: HttpsURLConnection? = null
        try {
            // Build multipart payload in memory first so Content-Length and Content-Type are exact
            val bodyBuffer = java.io.ByteArrayOutputStream()
            val dos = DataOutputStream(bodyBuffer)

            // 1. Model field
            writeFormField(dos, boundary, "model", model)

            // 2. Response format
            writeFormField(dos, boundary, "response_format", "json")

            // 3. Temperature: 0 enforces deterministic greedy decoding, preventing Whisper from
            // randomly hallucinating translations or language drifts.
            writeFormField(dos, boundary, "temperature", "0")

            // 4. Balanced bilingual conditioning prompt:
            // Whisper Large-v3 decoder mimics this style and vocabulary. Providing both natural English
            // and Egyptian Arabic sentences prevents Whisper from translating English speech into Arabic,
            // while preserving correct punctuation, capital letters, question marks, and hamzas in both languages.
            val prompt = if (language == "en") {
                "Hello, how are you? Good morning. Please transcribe clearly with proper capitalization, apostrophes, and punctuation."
            } else {
                "Hello, how are you? Good morning. السلام عليكم، ازيك عامل ايه؟ تمام الحمد لله، شكراً لك."
            }
            writeFormField(dos, boundary, "prompt", prompt)

            // 5. Language parameter:
            // CRITICAL: We deliberately NEVER pass language="ar" to Whisper's API because Whisper with
            // language="ar" forces non-Arabic speech (like English) to be translated into Arabic rather
            // than transcribed verbatim. When language is omitted, Whisper automatically detects whether
            // the user spoke English, Arabic, or mixed code-switching, and transcribes verbatim in that exact language.

            // 5. Audio file
            dos.writeBytes("$twoHyphens$boundary$lineEnd")
            dos.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"${audioFile.name}\"$lineEnd")
            dos.writeBytes("Content-Type: audio/mp4$lineEnd")
            dos.writeBytes(lineEnd)

            val fileInputStream = FileInputStream(audioFile)
            val buffer = ByteArray(4096)
            var bytesRead: Int
            while (fileInputStream.read(buffer).also { bytesRead = it } != -1) {
                dos.write(buffer, 0, bytesRead)
            }
            fileInputStream.close()
            dos.writeBytes(lineEnd)

            // Final boundary
            dos.writeBytes("$twoHyphens$boundary$twoHyphens$lineEnd")
            dos.flush()
            dos.close()

            val bodyBytes = bodyBuffer.toByteArray()

            val url = URL(endpointUrl)
            conn = (url.openConnection() as HttpsURLConnection).apply {
                requestMethod = "POST"
                doInput = true
                doOutput = true
                useCaches = false
                connectTimeout = 15000
                readTimeout = 30000
                setFixedLengthStreamingMode(bodyBytes.size)
                setRequestProperty("Authorization", "Bearer $apiKey")
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                setRequestProperty("Accept", "application/json")
            }

            conn.outputStream.use { os ->
                os.write(bodyBytes)
                os.flush()
            }

            val responseCode = conn.responseCode
            if (responseCode == 200) {
                val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseStr)
                val text = json.optString("text", "").trim()
                return Result.Success(text)
            } else {
                val errorStr = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $responseCode"
                val errMessage = try {
                    val errJson = JSONObject(errorStr)
                    errJson.optJSONObject("error")?.optString("message", errorStr) ?: errorStr
                } catch (_: Exception) {
                    errorStr
                }
                return Result.Error(errMessage, responseCode)
            }
        } catch (e: Exception) {
            return Result.Error(e.localizedMessage ?: "Connection failed")
        } finally {
            conn?.disconnect()
            try {
                audioFile.delete()
            } catch (_: Exception) {}
        }
    }

    /**
     * Verifies if the provided API key is valid.
     */
    fun testKey(apiKey: String): Result {
        if (apiKey.isBlank()) return Result.Error("Key is empty")
        val isGroq = apiKey.startsWith("gsk_") || !apiKey.startsWith("sk-")
        val testUrl = if (isGroq) "https://api.groq.com/openai/v1/models" else "https://api.openai.com/v1/models"
        var conn: HttpsURLConnection? = null
        return try {
            val url = URL(testUrl)
            conn = (url.openConnection() as HttpsURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Authorization", "Bearer ${apiKey.trim()}")
            }
            val code = conn.responseCode
            if (code == 200) {
                Result.Success("Key verified successfully! (المفتاح صالح وجاهز للاستخدام)")
            } else {
                val errorStr = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $code"
                Result.Error("Verification failed ($code): $errorStr", code)
            }
        } catch (e: Exception) {
            Result.Error(e.localizedMessage ?: "Network error testing key")
        } finally {
            conn?.disconnect()
        }
    }

    private fun writeFormField(dos: DataOutputStream, boundary: String, name: String, value: String) {
        val lineEnd = "\r\n"
        val twoHyphens = "--"
        dos.writeBytes("$twoHyphens$boundary$lineEnd")
        dos.writeBytes("Content-Disposition: form-data; name=\"$name\"$lineEnd")
        dos.writeBytes("Content-Type: text/plain; charset=UTF-8$lineEnd")
        dos.writeBytes(lineEnd)
        dos.write(value.toByteArray(Charsets.UTF_8))
        dos.writeBytes(lineEnd)
    }
}
