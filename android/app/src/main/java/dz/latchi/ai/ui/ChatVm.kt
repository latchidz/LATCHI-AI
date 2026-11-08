package dz.latchi.ai.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dz.latchi.ai.data.*
import dz.latchi.ai.net.Api
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * عقل الواجهة — كل التفاصيل مخفية هنا.
 */
class ChatVm(private val scope: CoroutineScope, private val pickerOut: () -> Unit) {

    var chats by mutableStateOf<List<Chat>>(emptyList())
        private set
    var activeId by mutableStateOf("")
        private set
    var msgs by mutableStateOf<List<Msg>>(emptyList())
        private set

    var input by mutableStateOf("")
    var attached by mutableStateOf<String?>(null)
    var streaming by mutableStateOf(false)
        private set
    var streamText by mutableStateOf("")
        private set
    var streamModel by mutableStateOf("")
        private set
    var statusText by mutableStateOf("")
        private set

    private var job: Job? = null

    // ═══ تحميل ═══
    fun init(ctx: Context) {
        chats = ChatStore.load(ctx).sortedByDescending { it.updated }
        val act = ChatStore.active(ctx)
        val c = chats.firstOrNull { it.id == act } ?: chats.firstOrNull()
        if (c == null) newChat(ctx) else open(ctx, c.id)
    }

    fun activeTitle(): String = chats.firstOrNull { it.id == activeId }?.title ?: ""

    fun open(ctx: Context, id: String) {
        val c = chats.firstOrNull { it.id == id } ?: return
        activeId = id
        msgs = c.msgs.toList()
        streamText = ""; streamModel = ""
        ChatStore.setActive(ctx, id)
    }

    fun newChat(ctx: Context) {
        val c = Chat(id = java.util.UUID.randomUUID().toString().take(10), title = "")
        chats = (listOf(c) + chats).take(60)
        activeId = c.id
        msgs = emptyList()
        streamText = ""
        persist(ctx)
        ChatStore.setActive(ctx, c.id)
    }

    fun remove(ctx: Context, id: String) {
        chats = chats.filterNot { it.id == id }
        persist(ctx)
        if (activeId == id) {
            val c = chats.firstOrNull()
            if (c != null) open(ctx, c.id) else newChat(ctx)
        }
    }

    private fun persist(ctx: Context) {
        ChatStore.save(ctx, chats.sortedByDescending { it.updated })
    }

    fun pickImage() = pickerOut()

    fun onImagePicked(b64: String) { attached = b64 }

    // ═══ الإرسال ═══
    fun send(ctx: Context, text: String) {
        val t = text.trim()
        val img = attached
        if (t.isEmpty() && img == null) return
        if (streaming) return
        if (activeId.isBlank()) newChat(ctx)

        if (chats.none { it.id == activeId }) newChat(ctx)
        val cur: Chat = chats.firstOrNull { it.id == activeId } ?: return
        val userMsg = Msg("user", t, image = img)
        cur.msgs.add(userMsg)
        if (cur.title.isBlank()) cur.title = if (t.isNotBlank()) t.take(38) else "📷"
        cur.updated = System.currentTimeMillis()

        msgs = cur.msgs.toList()
        chats = chats.toList()
        input = ""; attached = null
        statusText = L.thinking
        streamText = ""; streamModel = ""
        streaming = true
        persist(ctx)

        val chat = cur
        job = scope.launch {
            var finalText = ""
            var finalModel = ""
            var generatedImage: String? = null
            try {
                val history = chat.msgs.toList()
                val lang = L.current.code

                // ① صورة مرفقة ⇒ وصف أولاً
                var visionNote: String? = null
                if (img != null) {
                    statusText = L.reading
                    visionNote = withContext(Dispatchers.IO) {
                        Api.describe(img, "image/jpeg", t.ifBlank { "وصّف هاد الصورة بالتفصيل." })
                    }
                }

                // ② طلب رسم؟
                if (img == null && Engine.wantsImage(t)) {
                    statusText = L.drawing
                    val prompt = withContext(Dispatchers.IO) { Api.buildImagePrompt(t) }
                    val b64 = withContext(Dispatchers.IO) { Api.genImage(prompt) }
                    if (b64 != null) {
                        generatedImage = b64
                        finalText = ""
                        finalModel = "img"
                    } else {
                        finalText = L.error
                    }
                }

                // ③ نص
                if (generatedImage == null) {
                    statusText = L.writing
                    val res = withContext(Dispatchers.IO) {
                        Api.streamChat(
                            history = history,
                            extraSystem = visionNote?.let { "المستخدم رفع صورة، وهذا وصفها:\n$it" },
                            lang = lang,
                            onDelta = { p -> streamText += p },
                            onModel = { m -> streamModel = m },
                            onSwitch = { _ -> statusText = L.switching },
                            cancelled = { false },
                        )
                    }
                    finalText = res.text
                    finalModel = res.model
                }
            } catch (e: Exception) {
                if (finalText.isBlank()) finalText = L.error + ": " + L.retryAll
            } finally {
                withContext(Dispatchers.Main) {
                    chat.msgs.add(
                        Msg("assistant", finalText, image = generatedImage,
                            model = finalModel.ifBlank { null })
                    )
                    chat.updated = System.currentTimeMillis()
                    msgs = chat.msgs.toList()
                    chats = chats.toList()
                    streamText = ""; streamModel = ""
                    statusText = ""
                    streaming = false
                    persist(ctx)
                }
            }
        }
    }
}
