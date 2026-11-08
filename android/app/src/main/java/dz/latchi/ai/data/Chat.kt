package dz.latchi.ai.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** رسالة وحدة */
data class Msg(
    val role: String,              // "user" | "assistant"
    val text: String,
    val image: String? = null,     // base64 (صورة مولّدة ولا مرفقة)
    val model: String? = null,
    val ts: Long = System.currentTimeMillis(),
) {
    fun toJson() = JSONObject()
        .put("role", role).put("text", text)
        .put("image", image ?: "").put("model", model ?: "")
        .put("ts", ts)

    companion object {
        fun from(j: JSONObject) = Msg(
            role = j.optString("role", "user"),
            text = j.optString("text", ""),
            image = j.optString("image", "").ifBlank { null },
            model = j.optString("model", "").ifBlank { null },
            ts = j.optLong("ts", 0L),
        )
    }
}

/** محادثة */
data class Chat(
    val id: String = UUID.randomUUID().toString().take(10),
    var title: String = "",
    var msgs: MutableList<Msg> = mutableListOf(),
    var updated: Long = System.currentTimeMillis(),
) {
    fun toJson() = JSONObject()
        .put("id", id).put("title", title).put("updated", updated)
        .put("msgs", JSONArray().apply { msgs.forEach { put(it.toJson()) } })

    companion object {
        fun from(j: JSONObject) = Chat(
            id = j.optString("id", UUID.randomUUID().toString().take(10)),
            title = j.optString("title", ""),
            updated = j.optLong("updated", 0L),
        ).also { c ->
            val a = j.optJSONArray("msgs") ?: JSONArray()
            for (i in 0 until a.length()) a.optJSONObject(i)?.let { c.msgs.add(Msg.from(it)) }
        }
    }
}

/** تخزين محلّي */
object ChatStore {
    private const val PREF = "latchi_v2"
    private const val KEY_CHATS = "chats"
    private const val KEY_LANG = "lang"
    private const val KEY_ACTIVE = "active"

    fun load(ctx: Context): MutableList<Chat> {
        val p = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val raw = p.getString(KEY_CHATS, "[]") ?: "[]"
        val out = mutableListOf<Chat>()
        try {
            val a = JSONArray(raw)
            for (i in 0 until a.length()) a.optJSONObject(i)?.let { out.add(Chat.from(it)) }
        } catch (_: Exception) {}
        return out
    }

    fun save(ctx: Context, chats: List<Chat>) {
        val a = JSONArray()
        chats.take(60).forEach { a.put(it.toJson()) }
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putString(KEY_CHATS, a.toString()).apply()
    }

    fun lang(ctx: Context): Lang =
        Lang.of(ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY_LANG, "ar"))

    fun setLang(ctx: Context, l: Lang) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putString(KEY_LANG, l.code).apply()
    }

    fun active(ctx: Context): String? =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY_ACTIVE, null)

    fun setActive(ctx: Context, id: String) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putString(KEY_ACTIVE, id).apply()
    }
}
