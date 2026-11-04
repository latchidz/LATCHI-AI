package dz.latchi.ai.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * تخزين محلي بسيط: SharedPreferences للمزوّدين والإعدادات + ملفات JSON للمحادثات.
 * بلا مكتبات خارجية ⇒ خفيف وسريع.
 */
class Store(ctx: Context) {

    private val prefs = ctx.getSharedPreferences("latchi_ai", Context.MODE_PRIVATE)
    private val dir: File = File(ctx.filesDir, "chats").apply { mkdirs() }

    // ═══════ المزوّدون ═══════
    fun providers(): MutableList<Provider> {
        val out = mutableListOf<Provider>()
        val raw = prefs.getString("providers", null) ?: return out
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) arr.optJSONObject(i)?.let { out.add(Provider.from(it)) }
        } catch (_: Exception) {}
        return out
    }

    fun saveProviders(list: List<Provider>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        prefs.edit().putString("providers", arr.toString()).apply()
    }

    // ═══════ الموديلات (كاش) ═══════
    fun models(providerId: String): List<ModelInfo> {
        val raw = prefs.getString("models_$providerId", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { arr.optJSONObject(it)?.let(ModelInfo::from) }
        } catch (_: Exception) { emptyList() }
    }

    fun saveModels(providerId: String, list: List<ModelInfo>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        prefs.edit().putString("models_$providerId", arr.toString()).apply()
    }

    // ═══════ الاختيار الحالي ═══════
    var activeProviderId: String
        get() = prefs.getString("active_provider", "") ?: ""
        set(v) { prefs.edit().putString("active_provider", v).apply() }

    var activeModel: String
        get() = prefs.getString("active_model", "") ?: ""
        set(v) { prefs.edit().putString("active_model", v).apply() }

    // ═══════ الإعدادات ═══════
    var settings: Settings
        get() = Settings(
            systemPrompt = prefs.getString("sys_prompt", Settings().systemPrompt) ?: "",
            temperature = prefs.getFloat("temp", 0.7f).toDouble(),
            maxTokens = prefs.getInt("max_tok", 2048),
            freeOnly = prefs.getBoolean("free_only", true),
        )
        set(v) {
            prefs.edit()
                .putString("sys_prompt", v.systemPrompt)
                .putFloat("temp", v.temperature.toFloat())
                .putInt("max_tok", v.maxTokens)
                .putBoolean("free_only", v.freeOnly)
                .apply()
        }

    // ═══════ المحادثات ═══════
    fun chats(): List<Chat> {
        val out = ArrayList<Chat>()
        dir.listFiles { f -> f.name.startsWith("c_") && f.name.endsWith(".json") }?.forEach { f ->
            try { out.add(Chat.from(JSONObject(f.readText()))) } catch (_: Exception) {}
        }
        return out.sortedByDescending { it.updated }
    }

    fun saveChat(c: Chat) {
        try { File(dir, "c_${c.id}.json").writeText(c.toJson().toString()) } catch (_: Exception) {}
    }

    fun deleteChat(id: String) {
        try { File(dir, "c_$id.json").delete() } catch (_: Exception) {}
    }

    fun clearAll() {
        dir.listFiles()?.forEach { it.delete() }
    }
}

/** إعدادات جاهزة للمزوّدين المعروفين. */
data class Preset(
    val id: String,
    val name: String,
    val baseUrl: String,
    val note: String,
)

val PRESETS = listOf(
    Preset("openrouter", "OpenRouter", "https://openrouter.ai/api/v1", "14 موديل مجاني · مفتاح واحد"),
    Preset("groq", "Groq", "https://api.groq.com/openai/v1", "الأسرع فـالعالم"),
    Preset("nvidia", "NVIDIA NIM", "https://integrate.api.nvidia.com/v1", "رصيد مجاني"),
    Preset("mistral", "Mistral", "https://api.mistral.ai/v1", "طبقة مجانية"),
    Preset("cerebras", "Cerebras", "https://api.cerebras.ai/v1", "سريع بزاف"),
    Preset("custom", "مخصص", "", "أي API متوافق مع OpenAI"),
)
