package dz.latchi.ai.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dz.latchi.ai.data.ModelInfo

/** شعار التطبيق (SVG مش مسموح ⇒ نرسم بالـCanvas/Compose). */
@Composable
fun BrandMark(size: Int = 40) {
    Box(
        Modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size / 3.2).dp))
            .background(BrandBrush),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "L",
            color = Color.White,
            fontSize = (size * 0.5).sp,
            fontWeight = FontWeight.Black,
        )
    }
}

/** ثلاثة نقاط متحركة = يكتب. */
@Composable
fun TypingDots() {
    val t = rememberInfiniteTransition(label = "dots")
    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { i ->
            val a by t.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(650, delayMillis = i * 140, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "d$i",
            )
            Box(
                Modifier
                    .padding(horizontal = 2.5.dp)
                    .size(7.dp)
                    .alpha(a)
                    .clip(CircleShape)
                    .background(Violet)
            )
        }
    }
}

/** شريحة الموديل. */
@Composable
fun ModelChip(model: String, ctx: String, onClick: () -> Unit, enabled: Boolean = true) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        color = CardHi,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = .07f)),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(OkGreen))
            Spacer(Modifier.width(7.dp))
            Text(
                model.substringAfterLast('/').ifBlank { "اختار موديل" },
                fontSize = 12.sp,
                color = TextMain,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
            if (ctx.isNotBlank() && ctx != "—") {
                Spacer(Modifier.width(6.dp))
                Text(ctx, fontSize = 10.sp, color = TextMuted)
            }
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Default.ExpandMore, null, tint = TextMuted, modifier = Modifier.size(16.dp))
        }
    }
}

/** صف موديل فـالقائمة. */
@Composable
fun ModelRow(
    m: ModelInfo,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(if (selected) Violet.copy(alpha = .13f) else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (m.free) OkGreen.copy(alpha = .14f) else Violet.copy(alpha = .14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (m.vision) Icons.Default.Image else Icons.Default.Memory,
                null,
                tint = if (m.free) OkGreen else Violet,
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(m.label, fontSize = 13.sp, color = TextMain, fontWeight = FontWeight.Medium,
                maxLines = 1)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(m.id, fontSize = 10.sp, color = TextMuted, maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false))
                Spacer(Modifier.width(6.dp))
                Text(m.ctxHuman(), fontSize = 9.sp, color = TextMuted)
            }
        }
        if (m.free) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = OkGreen.copy(alpha = .15f),
            ) {
                Text("مجاني", fontSize = 9.sp, color = OkGreen,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }
        }
        if (selected) {
            Spacer(Modifier.width(6.dp))
            Icon(Icons.Default.CheckCircle, null, tint = Violet, modifier = Modifier.size(17.dp))
        }
    }
}

/** شاشة فارغة بلباقة. */
@Composable
fun EmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, sub: String) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(64.dp).clip(RoundedCornerShape(20.dp))
                .background(CardHi),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = Violet, modifier = Modifier.size(30.dp)) }
        Spacer(Modifier.height(16.dp))
        Text(title, fontSize = 15.sp, color = TextMain, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(sub, fontSize = 12.sp, color = TextMuted, textAlign = TextAlign.Center,
            lineHeight = 18.sp)
    }
}

/** بطاقة زجاجية. */
@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = CardBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = .06f)),
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

val BrandGradient = Brush.linearGradient(listOf(Violet, Cyan))
