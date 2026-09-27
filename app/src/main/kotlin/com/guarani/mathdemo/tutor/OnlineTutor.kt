package com.guarani.mathdemo.tutor

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import javax.net.ssl.HttpsURLConnection

data class TutorConnection(val url: String, val token: String) {
    val configured: Boolean get() = runCatching {
        val parsed = URL(url.trim())
        parsed.protocol == "https" && parsed.host.isNotBlank() && parsed.userInfo == null &&
            parsed.ref == null && token.isNotBlank()
    }.getOrDefault(false)
}

data class PendingTutorQuestion(
    val id: String = UUID.randomUUID().toString(),
    val question: String,
    val accepted: Boolean = false,
    val answer: String? = null,
)

// @spec spec://modules/android/PROP-010-android-demo-architecture#privacy
class TutorOutbox(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("tutor_online", Context.MODE_PRIVATE)

    fun connection() = TutorConnection(prefs.getString("url", "") ?: "", prefs.getString("token", "") ?: "")

    fun saveConnection(connection: TutorConnection): Boolean = prefs.edit()
        .putString("url", connection.url.trim().trimEnd('/'))
        .putString("token", connection.token.trim())
        .commit()

    fun pending(): PendingTutorQuestion? = runCatching {
        val value = JSONObject(prefs.getString("pending", null) ?: return null)
        PendingTutorQuestion(
            id = value.getString("id"),
            question = value.getString("question"),
            accepted = value.optBoolean("accepted"),
            answer = value.optString("answer").ifBlank { null },
        )
    }.getOrNull()

    fun savePending(question: PendingTutorQuestion): Boolean = prefs.edit().putString("pending", JSONObject().apply {
        put("id", question.id)
        put("question", question.question)
        put("accepted", question.accepted)
        put("answer", question.answer)
    }.toString()).commit()

    fun clearPending(): Boolean = prefs.edit().remove("pending").commit()
}

data class TutorStatus(val status: String, val answer: String?, val error: String?)

class TutorHttpException(val code: Int, val reason: String) : IOException(reason)

// @spec spec://modules/android/PROP-010-android-demo-architecture#online
class OnlineTutor {
    suspend fun submit(connection: TutorConnection, question: PendingTutorQuestion): TutorStatus =
        request(connection, question.id, question.question)

    suspend fun status(connection: TutorConnection, id: String): TutorStatus =
        request(connection, id, null)

    private suspend fun request(connection: TutorConnection, id: String, question: String?): TutorStatus =
        withContext(Dispatchers.IO) {
            require(connection.configured)
            val endpoint = URL("${connection.url.trim().trimEnd('/')}/requests/$id")
            val http = endpoint.openConnection() as HttpsURLConnection
            try {
                http.instanceFollowRedirects = false
                http.connectTimeout = 8_000
                http.readTimeout = 12_000
                http.setRequestProperty("Authorization", "Bearer ${connection.token}")
                http.setRequestProperty("Accept", "application/json")
                if (question != null) {
                    http.requestMethod = "POST"
                    http.doOutput = true
                    http.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    http.outputStream.use { it.write(JSONObject().put("question", question).toString().toByteArray(Charsets.UTF_8)) }
                }
                val code = http.responseCode
                val stream = if (code in 200..299) http.inputStream else http.errorStream
                val body = stream?.bufferedReader()?.use { it.readText().take(16_000) }.orEmpty()
                val json = runCatching { JSONObject(body) }.getOrDefault(JSONObject())
                if (code !in 200..299) throw TutorHttpException(code, json.optString("error", "http_error"))
                TutorStatus(json.optString("status"), json.optString("answer").ifBlank { null },
                    json.optString("error").ifBlank { null })
            } finally {
                http.disconnect()
            }
        }
}
