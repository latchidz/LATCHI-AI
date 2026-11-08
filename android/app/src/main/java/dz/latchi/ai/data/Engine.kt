package dz.latchi.ai.data

/**
 * ═══════════════════════════════════════════════════════════════
 *  محرّك LATCHI AI — مخفي تماماً عن الواجهة
 *  كل الإعدادات هنا. المستخدم ما يشوف حتى اسم موديل ولا مفتاح.
 * ═══════════════════════════════════════════════════════════════
 */
object Engine {

    const val CHAT_URL  = "https://integrate.api.nvidia.com/v1/chat/completions"
    const val GENAI_URL = "https://ai.api.nvidia.com/v1/genai/"

    /** المفاتيح — المحرّك كيدور بيناتهم إذا واحد طاح */
    private val KEYS = listOf(
        "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",
    )

    fun key(i: Int): String = KEYS[i % KEYS.size]

    /** سلسلة النص — الأسرع أولاً، والتبديل تلقائي */
    val TEXT_CHAIN = listOf(
        "nvidia/nemotron-3.5-bolt-e4b",
        "nvidia/nemotron-3-ultra-550b-a55b",
        "nvidia/nemotron-3.5-super-vl-120b-a12b",
        "nvidia/nemotron-3-nano-omni-30b-a3b-reasoning",
        "minimaxai/minimax-m3.1-flash",
        "meta/muse-glimmer-30b",
        "nvidia/nemotron-3.5-lightning-30b-a3b",
        "stepfun-ai/step-5-preview",
        "mistralai/mistral-large-4-1t-a52b",
        "poolside/laguna-xs-2.1",
        "meta/llama-3.2-90b-vision-instruct",
    )

    /** سلسلة الرؤية */
    val VISION_CHAIN = listOf(
        "nvidia/nemotron-3.5-super-vl-120b-a12b",
        "nvidia/nemotron-3-nano-omni-30b-a3b-reasoning",
        "stepfun-ai/step-5-preview",
        "meta/llama-3.2-90b-vision-instruct",
    )

    const val IMAGE_MODEL  = "black-forest-labs/flux.1-dev"
    const val MAX_TOKENS   = 2000
    const val MAX_ATTEMPTS = 4
    const val HISTORY      = 24

    /** كلمات طلب الرسم */
    val IMAGE_WORDS = listOf(
        "ارسم", "رسم", "صمم", "صوّر", "ولد صورة", "ولّد صورة", "صنع صورة",
        "اصنع صورة", "عطيني صورة", "أعطني صورة", "صورة لـ",
        "draw", "generate an image", "create an image", "make an image",
        "paint", "illustrate", "image of", "picture of",
        "dessine", "génère une image", "crée une image",
    )

    fun wantsImage(t: String): Boolean {
        val low = t.lowercase().trim()
        if (low.length > 400) return false
        return IMAGE_WORDS.any { low.contains(it) }
    }

    fun cleanPrompt(t: String): String {
        var s = t.trim()
        val low = s.lowercase()
        val hit = IMAGE_WORDS.sortedByDescending { it.length }.firstOrNull { low.startsWith(it) }
        if (hit != null) {
            s = s.substring(hit.length).trim()
            s = s.replace(Regex("^(لي|ني|لنا|نا)\\s+"), "").trim()
        }
        return s.ifBlank { t }.trim(' ', ':', '،', '.', '-')
    }
}
