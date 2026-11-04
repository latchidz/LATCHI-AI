package dz.latchi.ai.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

/**
 * Markdown مبسّط — بلا مكتبات.
 * يدعم: **غامق** · *مائل* · `كود` · ### عنوان · - قائمة · ```كتلة كود```
 */
fun mdToAnnotated(
    text: String,
    codeBg: Color = Color(0xFF0A0C14),
    accent: Color = Violet,
    codeFg: Color = Color(0xFF7DE2FF),
): AnnotatedString = buildAnnotatedString {
    val lines = text.split("\n")
    var inCode = false
    val codeBuf = StringBuilder()

    fun flushCode() {
        if (codeBuf.isNotEmpty()) {
            withStyle(
                SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    color = codeFg,
                    background = codeBg,
                    fontSize = androidx.compose.ui.unit.TextUnit(
                        13f, androidx.compose.ui.unit.TextUnitType.Sp
                    ),
                )
            ) { append(codeBuf.toString()) }
            codeBuf.clear()
        }
    }

    lines.forEachIndexed { idx, raw ->
        val line = raw
        if (line.trimStart().startsWith("```")) {
            if (inCode) { flushCode(); inCode = false }
            else inCode = true
            if (idx != lines.lastIndex) append("\n")
            return@forEachIndexed
        }
        if (inCode) {
            codeBuf.append(line)
            if (idx != lines.lastIndex) codeBuf.append("\n")
            return@forEachIndexed
        }

        // عنوان
        val h = when {
            line.startsWith("### ") -> 3
            line.startsWith("## ") -> 2
            line.startsWith("# ") -> 1
            else -> 0
        }
        val body = if (h > 0) line.substring(h + 1) else line

        when {
            h == 1 -> withStyle(SpanStyle(fontWeight = FontWeight.Bold,
                fontSize = androidx.compose.ui.unit.TextUnit(19f, androidx.compose.ui.unit.TextUnitType.Sp),
                color = accent)) { append(body) }
            h == 2 -> withStyle(SpanStyle(fontWeight = FontWeight.Bold,
                fontSize = androidx.compose.ui.unit.TextUnit(17f, androidx.compose.ui.unit.TextUnitType.Sp),
                color = accent)) { append(body) }
            h == 3 -> withStyle(SpanStyle(fontWeight = FontWeight.SemiBold,
                fontSize = androidx.compose.ui.unit.TextUnit(15f, androidx.compose.ui.unit.TextUnitType.Sp),
                color = accent)) { append(body) }
            line.trimStart().startsWith("- ") || line.trimStart().startsWith("* ") -> {
                append("  •  ")
                inline(this, line.trimStart().substring(2), codeBg, codeFg)
            }
            line.trimStart().matches(Regex("^\\d+[.)]\\s.*")) -> {
                val num = line.trimStart().takeWhile { it.isDigit() }
                append("  $num. ")
                inline(this, line.trimStart().dropWhile { it.isDigit() || it == '.' || it == ')' || it == ' ' }, codeBg, codeFg)
            }
            line.startsWith("> ") -> withStyle(SpanStyle(color = TextMuted, fontStyle = FontStyle.Italic)) {
                append(line.substring(2))
            }
            line.trim() == "---" || line.trim() == "***" -> withStyle(SpanStyle(color = CardHi)) {
                append("────────────────")
            }
            else -> inline(this, line, codeBg, codeFg)
        }
        if (idx != lines.lastIndex) append("\n")
    }
    flushCode()
}

private fun inline(
    b: androidx.compose.ui.text.AnnotatedString.Builder,
    s: String,
    codeBg: Color,
    codeFg: Color,
) {
    var i = 0
    val n = s.length
    while (i < n) {
        // كود مضمّن
        if (s[i] == '`') {
            val end = s.indexOf('`', i + 1)
            if (end > i) {
                withStyle(SpanStyle(fontFamily = FontFamily.Monospace, color = codeFg, background = codeBg)) {
                    b.append(s.substring(i + 1, end))
                }
                i = end + 1
                continue
            }
        }
        // غامق
        if (i + 1 < n && s[i] == '*' && s[i + 1] == '*') {
            val end = s.indexOf("**", i + 2)
            if (end > i) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    b.append(s.substring(i + 2, end))
                }
                i = end + 2
                continue
            }
        }
        // مائل
        if (s[i] == '*') {
            val end = s.indexOf('*', i + 1)
            if (end > i) {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    b.append(s.substring(i + 1, end))
                }
                i = end + 1
                continue
            }
        }
        // رابط [نص](رابط)
        if (s[i] == '[') {
            val close = s.indexOf(']', i)
            if (close > i && close + 1 < n && s[close + 1] == '(') {
                val paren = s.indexOf(')', close)
                if (paren > close) {
                    val label = s.substring(i + 1, close)
                    val url = s.substring(close + 2, paren)
                    withStyle(SpanStyle(color = Cyan, fontWeight = FontWeight.Medium)) {
                        b.append(label)
                    }
                    withStyle(SpanStyle(color = TextMuted)) { b.append(" ⟨$url⟩") }
                    i = paren + 1
                    continue
                }
            }
        }
        b.append(s[i])
        i++
    }
}
