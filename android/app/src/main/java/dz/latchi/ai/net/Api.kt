package dz.latchi.ai.net

import dz.latchi.ai.data.Msg
import dz.latchi.ai.data.ModelInfo
import dz.latchi.ai.data.Provider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * عميل OpenAI-compatible بسيط: SSE streaming + قائمة الموديلات.
 * بلا مكتبات — HttpURLConnection فقط ⇒ خفيف.
 */
object Api {

    private fun open(url: String, p: Provider): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 300_000
            setRequestProperty("Authorization", "Bearer ${p.apiKey}")
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("HTTP-Referer", "https://github.com/latchidz")
            setRequestProperty("X-Title", "Latchi AI")
        }

    // ═══════ قائمة الموديلات ═══════
    suspend fun listModels(p: Provider): Result<List<ModelInfo>> = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            conn = open("${p.baseUrl.trimEnd('/')}/models", p)
            conn.requestMethod = "GET"
            val code = conn.responseCode
            if (code == 401 || code == 403)
                return@withContext Result.failure(Exception("المفتاح ماشي صحيح ($code)"))
            if (code !in 200..299)
                return@withContext Result.failure(Exception("HTTP $code"))

            val body = conn.inputStream.bufferedReader().readText()
            val root = JSONObject(body)
            val arr = root.optJSONArray("data") ?: root.optJSONArray("models") ?: JSONArray()
            val out = ArrayList<ModelInfo>(arr.length())

            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val id = o.optString("id").ifBlank { o.optString("name") }
                if (id.isBlank()) continue
                val name = o.optString("name").ifBlank { id }
                val topCtx: Int = o.optJSONObject("top_provider")?.optInt("context_length") ?: 0
                val ctx: Int = o.optInt("context_length").takeIf { it > 0 } ?: topCtx
                val free = o.optString("id").endsWith(":free") ||
                        (o.optJSONObject("pricing")?.let {
                            it.optString("prompt").toDoubleOrNull() == 0.0 &&
                                    it.optString("completion").toDoubleOrNull() == 0.0
                        } ?: false)
                // architecture كائن ⇒ input_modalities مصفوفة جوّاه
                val mods = o.optJSONObject("architecture")?.optJSONArray("input_modalities")
                var hasImg = false
                if (mods != null) {
                    var k = 0
                    while (k < mods.length()) {
                        if (mods.optString(k) == "image") { hasImg = true; break }
                        k++
                    }
                }
                val vision = hasImg || id.contains("vl") || id.contains("vision") ||
                        id.contains("omni") || id.contains("llava")
                out.add(ModelInfo(id = id, name = name, context = ctx, free = free, vision = vision))
            }
            Result.success(out.distinctBy { it.id }.sortedBy { it.id })
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            try { conn?.disconnect() } catch (_: Exception) {}
        }
    }

    // ═══════ محادثة streaming ═══════
    /**
     * يبعث المحادثة ويستقبل الرد حرف بحرف.
     * @return النص الكامل
     */
    suspend fun chat(
        p: Provider,
        model: String,
        history: List<Msg>,
        system: String,
        temperature: Double,
        maxTokens: Int,
        onDelta: (String) -> Unit,
    ): Result<String> = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        var reader: BufferedReader? = null
        try {
            val msgs = JSONArray()
            if (system.isNotBlank())
                msgs.put(JSONObject().put("role", "system").put("content", system))
            history.forEach { msgs.put(it.toApi()) }

            val body = JSONObject()
                .put("model", model)
                .put("messages", msgs)
                .put("stream", true)
                .put("temperature", temperature)
                .put("max_tokens", maxTokens)
                .toString()

            conn = open("${p.baseUrl.trimEnd('/')}/chat/completions", p)
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Accept", "text/event-stream")
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

            val code = conn.responseCode
            if (code !in 200..299) {
                val err = try {
                    conn.errorStream?.bufferedReader()?.readText() ?: ""
                } catch (_: Exception) { "" }
                val friendly = when (code) {
                    401 -> "المفتاح ماشي صحيح — تحقق منو فـالإعدادات"
                    402 -> "الرصيد خلص فـهذا المزود"
                    403 -> "ممنوع — الموديل ما عندكش حق الوصول"
                    404 -> "الموديل ماشي موجود فـهذا المزود"
                    429 -> "طلبات كثيرة (429) — استنى شوية ولا بدّل موديل"
                    else -> "خطأ HTTP $code"
                }
                val detail = try {
                    JSONObject(err).optJSONObject("error")?.optString("message") ?: err
                } catch (_: Exception) { err }
                return@withContext Result.failure(
                    Exception(if (detail.isBlank()) friendly else "$friendly\n$detail")
                )
            }

            val sb = StringBuilder()
            reader = conn.inputStream.bufferedReader()
            var line: String?
            while (true) {
                line = reader.readLine() ?: break
                if (line.isEmpty()) continue
                if (!line.startsWith("data:")) continue
                val data = line.removePrefix("data:").trim()
                if (data == "[DONE]") break
                try {
                    val j = JSONObject(data)
                    val choices = j.optJSONArray("choices") ?: continue
                    val d = choices.optJSONObject(0) ?: continue
                    val piece = d.optJSONObject("delta")?.optString("content")
                        ?: d.optJSONObject("message")?.optString("content")
                        ?: ""
                    if (piece.isNotEmpty()) {
                        sb.append(piece)
                        onDelta(piece)
                    }
                } catch (_: Exception) {
                    // سطر مقطوع — نكملو
                }
            }
            if (sb.isEmpty()) Result.failure(Exception("الرد جاء فارغ — جرّب موديل آخر"))
            else Result.success(sb.toString())
        } catch (e: Exception) {
            val msg = when {
                e.message?.contains("timeout", true) == true -> "انتهى الوقت — الشبكة بطيئة"
                e.message?.contains("Unable to resolve host", true) == true -> "ما كاينش إنترنت"
                else -> e.message ?: "خطأ شبكة"
            }
            Result.failure(Exception(msg))
        } finally {
            try { reader?.close() } catch (_: Exception) {}
            try { conn?.disconnect() } catch (_: Exception) {}
        }
    }
}
