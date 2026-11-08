package dz.latchi.ai.data

import androidx.compose.runtime.mutableStateOf

/** اللغات المدعومة */
enum class Lang(val code: String, val label: String, val flag: String) {
    AR("ar", "العربية", "🇩🇿"),
    FR("fr", "Français", "🇫🇷"),
    EN("en", "English", "🇬🇧");

    companion object {
        fun of(c: String?) = entries.firstOrNull { it.code == c } ?: AR
    }
}

object L {
    var current by mutableStateOf(Lang.AR)
        private set

    fun set(l: Lang) { current = l }

    private fun t(ar: String, fr: String, en: String) = when (current) {
        Lang.AR -> ar; Lang.FR -> fr; Lang.EN -> en
    }

    val appName get() = "Latchi AI"

    // ═══ شاشة الترحيب ═══
    val tagline get() = t("ذكاء اصطناعي يفهمك", "L'IA qui vous comprend", "AI that understands you")
    val hello get() = t("واش نعاونك فيه؟", "Comment puis-je aider ?", "How can I help?")
    val helloSub get() = t(
        "اكتب سؤالك، ولا اطلب صورة، ولا ابعث لقطة — ونتولّى الباقي.",
        "Posez une question, demandez une image ou envoyez une photo.",
        "Ask anything, request an image, or send a photo."
    )

    // ═══ اقتراحات ═══
    val sug1Title get() = t("تعلّم", "Apprendre", "Learn")
    val sug1 get() = t("اشرحلي كيفاش نتعلم البرمجة من الصفر", "Explique-moi comment apprendre la programmation", "How do I start learning to code?")
    val sug2Title get() = t("رسم", "Créer", "Create")
    val sug2 get() = t("ارسم صورة سيارة كلاسيكية فـساحة جزائرية وقت الغروب", "Dessine une voiture classique dans un square algérien au coucher du soleil", "Draw a classic car in an Algerian square at sunset")
    val sug3Title get() = t("كود", "Code", "Code")
    val sug3 get() = t("اكتبلي كود بايثون يقرا ملف Excel ويحسب المجموع", "Écris un script Python qui lit un fichier Excel", "Write Python code that reads an Excel file")
    val sug4Title get() = t("ترجمة", "Traduction", "Translate")
    val sug4 get() = t("ترجملي هاد النص للفرنسية", "Traduis ce texte en arabe", "Translate this text to Arabic")

    // ═══ الواجهة ═══
    val inputHint get() = t("اكتب رسالتك…", "Écrivez votre message…", "Type your message…")
    val newChat get() = t("محادثة جديدة", "Nouvelle conversation", "New chat")
    val send get() = t("إرسال", "Envoyer", "Send")
    val attach get() = t("صورة", "Photo", "Photo")
    val settings get() = t("الإعدادات", "Paramètres", "Settings")
    val language get() = t("اللغة", "Langue", "Language")
    val about get() = t("حول التطبيق", "À propos", "About")
    val close get() = t("إغلاق", "Fermer", "Close")
    val copy get() = t("نسخ", "Copier", "Copy")
    val copied get() = t("✓ تنسخ", "✓ Copié", "✓ Copied")
    val delete get() = t("حذف", "Supprimer", "Delete")
    val cancel get() = t("إلغاء", "Annuler", "Cancel")
    val you get() = t("أنا", "Moi", "Me")

    // ═══ الحالات ═══
    val thinking get() = t("كنفكّر…", "Réflexion…", "Thinking…")
    val writing get() = t("كنكتب…", "Écriture…", "Writing…")
    val drawing get() = t("كنولّد الصورة…", "Génération de l'image…", "Generating image…")
    val reading get() = t("كنقرا الصورة…", "Lecture de l'image…", "Reading image…")
    val switching get() = t("بدّل تلقائياً", "Bascule auto", "Auto-switched")
    val error get() = t("وقعت مشكلة", "Une erreur est survenue", "Something went wrong")
    val retryAll get() = t("كل المحرّكات تعبانة حالياً، عاود من بعد شوية.",
        "Tous les moteurs sont occupés, réessayez.", "All engines are busy, try again shortly.")

    // ═══ حول ═══
    val aboutText get() = t(
        "Latchi AI · نسخة 2.0\n\nمحرّك ذكي جزائري يختار الموديل الأنسب لكل سؤال، وإذا تعطّل موديل كيبدّل بوحدو ويكمّل نفس المحادثة بلا ما تحسّ.\n\nصنع بحب فـالجزائر 🇩🇿",
        "Latchi AI · version 2.0\n\nUn moteur intelligent algérien qui choisit le meilleur modèle pour chaque question et bascule automatiquement en cas de panne, sans interrompre la conversation.\n\nFait avec passion en Algérie 🇩🇿",
        "Latchi AI · version 2.0\n\nAn Algerian smart engine that picks the best model for each question and switches automatically on failure, continuing the same conversation seamlessly.\n\nMade with love in Algeria 🇩🇿"
    )

    val madeIn get() = t("صنع فـالجزائر", "Fait en Algérie", "Made in Algeria")
}
