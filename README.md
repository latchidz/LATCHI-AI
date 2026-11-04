# 🤖 Latchi AI

تطبيق ذكاء اصطناعي كامل للأندرويد — أي API متوافق مع OpenAI.

## 📥 التحميل
**[LatchiAI-1.0.0.apk](https://github.com/latchidz/LATCHI-AI/releases/latest/download/LatchiAI-1.0.0.apk)**

## ✨ المزايا

| | |
|---|---|
| 🤖 **اختار الموديل** | قائمة حقيقية من `/models` — مجاني/مدفوع · يشوف الصور |
| ⚡ **Streaming** | الرد يبان حرف بحرف |
| 🖼️ **صور** | ابعث لقطة والموديل يشوفها |
| 🎨 **Markdown** | كود · عناوين · قوائم · bold |
| 💾 **محادثات** | محفوظة محلياً · سجل كامل |
| ⚙️ **تحكم** | temperature · max tokens · system prompt |
| 🌙 **واجهة** | داكنة · عربي RTL · خفيفة جداً |

## 🚀 البدء (3 دقايق)

```
1. ثبّت APK
2. ⚙️ Settings ⇒ «زيد مزوّد» ⇒ اختار OpenRouter
3. المفتاح من openrouter.ai/keys
4. اختار موديل مجاني (يبدا بـ :free)
5. اكتب ✅
```

## 🔌 مزوّدون مدعومون

| | Base URL |
|---|---|
| OpenRouter | `https://openrouter.ai/api/v1` |
| Groq | `https://api.groq.com/openai/v1` |
| NVIDIA NIM | `https://integrate.api.nvidia.com/v1` |
| Mistral | `https://api.mistral.ai/v1` |
| Cerebras | `https://api.cerebras.ai/v1` |

## 🏗️ البناء

```bash
cd android
gradle assembleRelease
```
أو ادفع لـ`main` ⇒ GitHub Actions يبني APK موقّع تلقائياً.

## 📦 التقنيات
Kotlin · Jetpack Compose · Material 3 · HttpURLConnection · صفر مكتبات خارجية للشبكة
