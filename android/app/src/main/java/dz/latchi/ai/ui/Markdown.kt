package dz.latchi.ai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * عارض Markdown خفيف: عناوين · نقاط · **عريض** · `كود` · ```بلوك كود``` · اقتباس
 */
@Composable
fun Markdown(text: String, color: Color, modifier: Modifier = Modifier) {
    val blocks = remember(text) { parseBlocks(text) }
    Column(modifier = modifier) {
        for (b in blocks) {
            when (b.type) {
                BType.CODE -> CodeBlock(b.text)
                BType.H1 -> MdText(b.text, color, 19.sp, FontWeight.Bold)
                BType.H2 -> MdText(b.text, color, 17.sp, FontWeight.Bold)
                BType.H3 -> MdText(b.text, color, 15.5.sp, FontWeight.SemiBold)
                BType.BULLET -> Row(Modifier.padding(vertical = 1.dp)) {
                    Text("•", color = Blue, fontSize = 15.sp,
                        modifier = Modifier.padding(end = 8.dp, top = 1.dp))
                    MdText(b.text, color, 15.sp, FontWeight.Normal, Modifier.weight(1f))
                }
                BType.QUOTE -> Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    Box(Modifier.width(3.dp).height(20.dp)
                        .background(Blue, RoundedCornerShape(2.dp)))
                    Spacer(Modifier.width(10.dp))
                    MdText(b.text, Pal.txt2, 14.sp, FontWeight.Normal, Modifier.weight(1f))
                }
                BType.NORMAL -> MdText(b.text, color, 15.sp, FontWeight.Normal)
            }
        }
    }
}

@Composable
private fun MdText(
    text: String,
    color: Color,
    size: TextUnit,
    weight: FontWeight,
    modifier: Modifier = Modifier,
) {
    Text(
        text = inline(text),
        color = color,
        fontSize = size,
        fontWeight = weight,
        lineHeight = (size.value * 1.62f).sp,
        modifier = modifier.padding(vertical = 3.dp),
    )
}

@Composable
private fun CodeBlock(code: String) {
    val scroll = rememberScrollState()
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(if (Pal.dark) Color(0xFF0B1120) else Color(0xFFF2F5FA), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(
            text = code.trimEnd(),
            color = if (Pal.dark) Color(0xFFC4D4EC) else Color(0xFF243247),
            fontSize = 12.6.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = 19.sp,
            modifier = Modifier.horizontalScroll(scroll),
        )
    }
}

private enum class BType { NORMAL, H1, H2, H3, BULLET, QUOTE, CODE }
private data class Block(val type: BType, val text: String)

private val BULLET_RE = Regex("^[-*+]\\s+")
private val NUM_RE = Regex("^\\d+[.)]\\s+")

private fun parseBlocks(src: String): List<Block> {
    val out = mutableListOf<Block>()
    val lines = src.replace("\r\n", "\n").split("\n")
    var i = 0
    val para = StringBuilder()

    fun flush() {
        val t = para.toString().trim()
        if (t.isNotEmpty()) out.add(Block(BType.NORMAL, t))
        para.setLength(0)
    }

    while (i < lines.size) {
        val l = lines[i].trim()

        if (l.startsWith("```")) {
            flush()
            val code = StringBuilder()
            i++
            while (i < lines.size && !lines[i].trim().startsWith("```")) {
                code.append(lines[i]).append("\n")
                i++
            }
            out.add(Block(BType.CODE, code.toString()))
            i++
            continue
        }

        when {
            l.startsWith("#### ") -> { flush(); out.add(Block(BType.H3, l.removePrefix("#### "))) }
            l.startsWith("### ")  -> { flush(); out.add(Block(BType.H3, l.removePrefix("### "))) }
            l.startsWith("## ")   -> { flush(); out.add(Block(BType.H2, l.removePrefix("## "))) }
            l.startsWith("# ")    -> { flush(); out.add(Block(BType.H1, l.removePrefix("# "))) }
            l.startsWith("> ")    -> { flush(); out.add(Block(BType.QUOTE, l.removePrefix("> "))) }
            l.matches(Regex("^[-*+]\\s+.*")) ->
                { flush(); out.add(Block(BType.BULLET, l.replaceFirst(BULLET_RE, ""))) }
            l.matches(Regex("^\\d+[.)]\\s+.*")) ->
                { flush(); out.add(Block(BType.BULLET, l.replaceFirst(NUM_RE, ""))) }
            l.isEmpty() -> flush()
            else -> { if (para.isNotEmpty()) para.append("\n"); para.append(l) }
        }
        i++
    }
    flush()
    return out
}

/** **عريض** · *مائل* · `كود` */
private fun inline(text: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        when {
            text.startsWith("**", i) -> {
                val end = text.indexOf("**", i + 2)
                if (end > 0) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(inline(text.substring(i + 2, end)))
                    }
                    i = end + 2
                } else { append(text[i]); i++ }
            }
            text[i] == '`' -> {
                val end = text.indexOf('`', i + 1)
                if (end > 0) {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = if (Pal.dark) Color(0xFF1B2437) else Color(0xFFE6ECF6),
                            color = if (Pal.dark) Color(0xFFA9C8F0) else Color(0xFF14538F),
                        )
                    ) { append(text.substring(i + 1, end)) }
                    i = end + 1
                } else { append(text[i]); i++ }
            }
            text[i] == '*' && i + 1 < text.length && text[i + 1] != ' ' -> {
                val end = text.indexOf('*', i + 1)
                if (end > 0) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                } else { append(text[i]); i++ }
            }
            else -> { append(text[i]); i++ }
        }
    }
}
