package dz.latchi.ai.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** مزوّد API (OpenRouter · Groq · NVIDIA · …). */
data class Provider(
    val id: String = UUID.randomUUID().toString().take(8),
    val name: String,
    val baseUrl: String,
    val apiKey: String = "",
    val presetId: String = "custom",
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id).put("name", name).put("baseUrl", baseUrl)
        .put("apiKey", apiKey).put("presetId", presetId)

    companion object {
        fun from(j: JSONObject) = Provider(
            id = j.optString("id", UUID.randomUUID().toString().take(8)),
            name = j.optString("name", "مزوّد"),
            baseUrl = j.optString("baseUrl", "").trimEnd('/'),
            apiKey = j.optString("apiKey", ""),
            presetId = j.optString("presetId", "custom"),
        )
    }
}

/** موديل من /models. */
data class ModelInfo(
    val id: String,
    val name: String = "",
    val context: Int = 0,
    val free: Boolean = false,
    val vision: Boolean = false,
) {
    val label: String get() = name.ifBlank { id }

    fun ctxHuman(): String = when {
        context >= 1_000_000 -> "%.0fM".format(context / 1_000_000.0)
        context >= 1000 -> "%.0fK".format(context / 1000.0)
        context > 0 -> "$context"
        else -> "—"
    }

    fun toJson(): JSONObject = JSONObject()
        .put("id", id).put("name", name).put("context", context)
        .put("free", free).put("vision", vision)

    companion object {
        fun from(j: JSONObject) = ModelInfo(
            id = j.optString("id"),
            name = j.optString("name", ""),
            context = j.optInt("context", 0),
            free = j.optBoolean("free", false),
            vision = j.optBoolean("vision", false),
        )
    }
}

/** رسالة فـمحادثة. */
data class Msg(
    val role: String,            // user | assistant | system
    val content: String,
    val at: Long = System.currentTimeMillis(),
    val image: String? = null,   // base64 JPEG (بلا البادئة)
    val error: Boolean = false,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("role", role).put("content", content).put("at", at)
        .apply { image?.let { put("image", it) } }
        .put("error", error)

    /** يحوّل لـAPI. الصور تصير content array للموديلات اللي تشوف. */
    fun toApi(): JSONObject {
        val o = JSONObject().put("role", role)
        if (image.isNullOrEmpty() || role != "user") {
            o.put("content", content)
        } else {
            val arr = JSONArray()
            arr.put(JSONObject().put("type", "text").put("text", content.ifBlank { " " }))
            arr.put(
                JSONObject().put("type", "image_url")
                    .put("image_url", JSONObject().put("url", "data:image/jpeg;base64,$image"))
            )
            o.put("content", arr)
        }
        return o
    }

    companion object {
        fun from(j: JSONObject) = Msg(
            role = j.optString("role", "user"),
            content = j.optString("content", ""),
            at = j.optLong("at", System.currentTimeMillis()),
            image = if (j.has("image")) j.optString("image") else null,
            error = j.optBoolean("error", false),
        )
    }
}

/** محادثة محفوظة. */
data class Chat(
    val id: String = UUID.randomUUID().toString().take(12),
    val title: String = "محادثة جديدة",
    val updated: Long = System.currentTimeMillis(),
    val providerId: String = "",
    val model: String = "",
    val messages: List<Msg> = emptyList(),
) {
    fun toJson(): JSONObject {
        val arr = JSONArray()
        messages.forEach { arr.put(it.toJson()) }
        return JSONObject()
            .put("id", id).put("title", title).put("updated", updated)
            .put("providerId", providerId).put("model", model)
            .put("messages", arr)
    }

    companion object {
        fun from(j: JSONObject): Chat {
            val arr = j.optJSONArray("messages") ?: JSONArray()
            val ms = ArrayList<Msg>(arr.length())
            for (i in 0 until arr.length()) arr.optJSONObject(i)?.let { ms.add(Msg.from(it)) }
            return Chat(
                id = j.optString("id", UUID.randomUUID().toString().take(12)),
                title = j.optString("title", "محادثة"),
                updated = j.optLong("updated", System.currentTimeMillis()),
                providerId = j.optString("providerId", ""),
                model = j.optString("model", ""),
                messages = ms,
            )
        }
    }
}

/** إعدادات التوليد. */
data class Settings(
    val systemPrompt: String = "أنت مساعد ذكي. جاوب بالدارجة الجزائرية إذا السؤال بالعربية، وباللغة اللي يكتب بيها المستعمل. كون دقيق ومختصر.",
    val temperature: Double = 0.7,
    val maxTokens: Int = 2048,
    val freeOnly: Boolean = true,
)
