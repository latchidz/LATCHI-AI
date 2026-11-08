package dz.latchi.ai.net

import dz.latchi.ai.data.Engine
import dz.latchi.ai.data.Msg
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * ═══════════════════════════════════════════════════════════════
 *  عميل NVIDIA NIM — بث SSE · رؤية · توليد صور
 *  كل شيء مخفي: الواجهة ما كتشوف حتى مفتاح ولا موديل.
 * ═══════════════════════════════════════════════════════════════
 */
object Api {

    // ── فتح اتصال ─────────────────────────────────────────
    private fun open(url: String, key: String, timeout: Int = 180_000): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = timeout
            requestMethod = "POST"
            setRequestProperty("Authorization", "Bearer $key")
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
        }

    private fun postJson(url: String, key: String, body: String, timeout: Int = 180_000): HttpURLConnection {
        val c = open(url, key, timeout)
        c.doOutput = true
        c.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        return c
    }

    // ── بناء الرسائل ──────────────────────────────────────
    private fun messages(history: List<Msg>, extra: String?, system: String): JSONArray {
        val arr = JSONArray()
        arr.put(JSONObject().put("role", "system").put("content", system))
        history.takeLast(Engine.HISTORY).forEach { m ->
            if (m.text.isNotBlank())
                arr.put(JSONObject().put("role", m.role).put("content", m.text))
        }
        if (!extra.isNullOrBlank())
            arr.put(JSONObject().put("role", "system").put("content", extra))
        return arr
    }

    fun systemPrompt(lang: String): String = when (lang) {
        "fr" -> "Tu es « Latchi AI », un assistant intelligent algérien. Réponds en français, " +
                "de façon claire, directe et utile. Utilise le markdown et les blocs de code quand c'est utile."
        "en" -> "You are \"Latchi AI\", a smart Algerian assistant. Reply in English, clearly and " +
                "directly. Use markdown and code blocks when helpful."
        else -> "أنت «لاشي أي آي» مساعد ذكي جزائري. ردّ بالدارجة الجزائرية المفهومة بحروف عربية واضحة. " +
                "إذا كتب المستخدم بلغة أخرى ردّ بنفس لغته. كن مباشراً بلا حشو، واستعمل التنسيق كي يساعد."
    }

    // ═══════════════════════════════════════════════════════
    //  بث المحادثة — مع تبديل تلقائي بين الموديلات
    // ═══════════════════════════════════════════════════════
    class StreamResult(val text: String, val model: String, val switches: Int)

    fun streamChat(
        history: List<Msg>,
        extraSystem: String?,
        lang: String,
        onDelta: (String) -> Unit,
        onModel: (String) -> Unit,
        onSwitch: (String) -> Unit,
        cancelled: () -> Boolean,
    ): StreamResult {
        val msgs = messages(history, extraSystem, systemPrompt(lang))
        var lastErr = ""
        var attempts = 0

        for (model in Engine.TEXT_CHAIN) {
            if (attempts >= Engine.MAX_ATTEMPTS || cancelled()) break
            attempts++
            val key = Engine.key(attempts)
            val body = JSONObject()
                .put("model", model)
                .put("messages", msgs)
                .put("max_tokens", Engine.MAX_TOKENS)
                .put("temperature", 0.7)
                .put("top_p", 0.95)
                .put("stream", true)
                .toString()

            var conn: HttpURLConnection? = null
            try {
                conn = postJson(Engine.CHAT_URL, key, body)
                val code = conn.responseCode
                if (code != 200) {
                    val err = try {
                        conn.errorStream?.bufferedReader()?.readText()?.take(150) ?: ""
                    } catch (_: Exception) { "" }
                    lastErr = "HTTP $code $err"
                    if (code == 401) break
                    continue
                }

                val sb = StringBuilder()
                val br = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8))
                var switched = false
                br.use { r ->
                    var line: String?
                    while (true) {
                        line = r.readLine() ?: break
                        if (cancelled()) break
                        val l = line.trim()
                        if (!l.startsWith("data:")) continue
                        val data = l.substring(5).trim()
                        if (data == "[DONE]") break
                        try {
                            val j = JSONObject(data)
                            val ch = j.optJSONArray("choices") ?: continue
                            if (ch.length() == 0) continue
                            val d = ch.getJSONObject(0).optJSONObject("delta") ?: continue
                            var piece = d.optString("content", "")
                            if (piece.isEmpty()) piece = d.optString("reasoning_content", "")
                            if (piece.isNotEmpty()) {
                                if (!switched) { switched = true; onModel(model) }
                                sb.append(piece)
                                onDelta(piece)
                            }
                        } catch (_: Exception) { continue }
                    }
                }
                if (sb.isNotEmpty()) return StreamResult(sb.toString(), model, attempts - 1)
                lastErr = "رد فارغ"
            } catch (e: Exception) {
                lastErr = "${e.javaClass.simpleName}: ${e.message ?: ""}"
            } finally {
                try { conn?.disconnect() } catch (_: Exception) {}
            }
            if (attempts < Engine.MAX_ATTEMPTS && !cancelled()) onSwitch(lastErr)
        }
        throw Exception(lastErr.ifBlank { "no-engine" })
    }

    // ═══════════════════════════════════════════════════════
    //  الرؤية — وصف صورة
    // ═══════════════════════════════════════════════════════
    fun describe(imageB64: String, mime: String, question: String): String? {
        val content = JSONArray()
            .put(JSONObject().put("type", "text")
                .put("text", question.ifBlank { "وصّف هاد الصورة بالتفصيل." }))
            .put(JSONObject().put("type", "image_url")
                .put("image_url", JSONObject().put("url", "data:$mime;base64,$imageB64")))

        val msgArr = JSONArray().put(JSONObject().put("role", "user").put("content", content))

        for ((i, model) in Engine.VISION_CHAIN.take(3).withIndex()) {
            var conn: HttpURLConnection? = null
            try {
                val body = JSONObject()
                    .put("model", model).put("messages", msgArr)
                    .put("max_tokens", Engine.MAX_TOKENS).put("temperature", 0.4)
                    .toString()
                conn = postJson(Engine.CHAT_URL, Engine.key(i), body, 120_000)
                if (conn.responseCode != 200) continue
                val txt = conn.inputStream.bufferedReader().readText()
                val j = JSONObject(txt)
                val m = j.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")
                val c = m?.optString("content", "") ?: ""
                if (c.isNotBlank()) return c
            } catch (_: Exception) {
            } finally {
                try { conn?.disconnect() } catch (_: Exception) {}
            }
        }
        return null
    }

    // ═══════════════════════════════════════════════════════
    //  تحويل الطلب لبرومبت إنجليزي
    // ═══════════════════════════════════════════════════════
    fun buildImagePrompt(userText: String): String {
        val base = Engine.cleanPrompt(userText)
        try {
            val sys = "You convert image requests into ONE detailed English image-generation prompt. " +
                    "Output ONLY the prompt, no quotes, no explanation. Include subject, setting, " +
                    "lighting, mood, camera/lens, style, quality keywords. Max 60 words."
            val arr = JSONArray()
                .put(JSONObject().put("role", "system").put("content", sys))
                .put(JSONObject().put("role", "user").put("content", base))
            val body = JSONObject()
                .put("model", Engine.TEXT_CHAIN[0]).put("messages", arr)
                .put("max_tokens", Engine.MAX_TOKENS).put("temperature", 0.4)
                .toString()
            val conn = postJson(Engine.CHAT_URL, Engine.key(0), body, 90_000)
            if (conn.responseCode == 200) {
                val j = JSONObject(conn.inputStream.bufferedReader().readText())
                val c = j.optJSONArray("choices")?.optJSONObject(0)
                    ?.optJSONObject("message")?.optString("content", "")?.trim()?.trim('"') ?: ""
                conn.disconnect()
                if (c.length in 8..400) return c
            } else conn.disconnect()
        } catch (_: Exception) {}
        return base
    }

    // ═══════════════════════════════════════════════════════
    //  توليد صورة — FLUX
    // ═══════════════════════════════════════════════════════
    fun genImage(prompt: String): String? {
        val prompts = JSONArray().put(JSONObject().put("text", prompt))
        val body = JSONObject()
            .put("text_prompts", prompts)
            .put("cfg_scale", 7)
            .put("seed", 0)
            .put("steps", 30)
            .toString()
        var conn: HttpURLConnection? = null
        return try {
            conn = postJson(Engine.GENAI_URL + Engine.IMAGE_MODEL, Engine.key(0), body, 300_000)
            if (conn.responseCode != 200) return null
            val txt = conn.inputStream.bufferedReader().readText()
            JSONObject(txt).optJSONArray("artifacts")
                ?.optJSONObject(0)?.optString("base64", "")?.ifBlank { null }
        } catch (_: Exception) {
            null
        } finally {
            try { conn?.disconnect() } catch (_: Exception) {}
        }
    }

    // ═══════════════════════════════════════════════════════
    //  فحص سريع
    // ═══════════════════════════════════════════════════════
    fun ping(): Boolean {
        return try {
            val arr = JSONArray().put(JSONObject().put("role", "user").put("content", "نعم"))
            val body = JSONObject()
                .put("model", Engine.TEXT_CHAIN[0]).put("messages", arr)
                .put("max_tokens", 2000).toString()
            val conn = postJson(Engine.CHAT_URL, Engine.key(0), body, 20_000)
            val ok = conn.responseCode == 200
            conn.disconnect()
            ok
        } catch (_: Exception) { false }
    }
}
