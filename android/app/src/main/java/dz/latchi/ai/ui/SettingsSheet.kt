package dz.latchi.ai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dz.latchi.ai.data.L
import dz.latchi.ai.data.Lang

/**
 * الإعدادات — غير اللغة وحول التطبيق.
 * المحرّك والموديلات والمفاتيح: مخفيين تماماً.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(onLang: (Lang) -> Unit, onClose: () -> Unit) {
    var showAbout by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onClose,
        containerColor = Pal.bg2,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 34.dp)
        ) {
            Text(L.settings, color = Pal.txt, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(20.dp))

            // ═══ اللغة ═══
            Text(L.language, color = Pal.txt3, fontSize = 11.5.sp, fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp)
            Spacer(Modifier.height(9.dp))
            Surface(color = Pal.bg3, shape = RoundedCornerShape(15.dp)) {
                Column {
                    Lang.entries.forEachIndexed { i, l ->
                        if (i > 0) Box(
                            Modifier.fillMaxWidth().height(1.dp).background(Pal.line)
                        )
                        Row(
                            Modifier.fillMaxWidth().clickable { onLang(l) }
                                .padding(horizontal = 15.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(l.flag, fontSize = 19.sp)
                            Spacer(Modifier.width(13.dp))
                            Text(l.label, color = Pal.txt, fontSize = 14.5.sp,
                                modifier = Modifier.weight(1f))
                            if (L.current == l)
                                Box(
                                    Modifier.size(22.dp).clip(CircleShape).background(Blue),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Filled.Check, null, tint = Color.White,
                                        modifier = Modifier.size(14.dp))
                                }
                        }
                    }
                }
            }

            Spacer(Modifier.height(22.dp))

            // ═══ حول ═══
            Surface(
                color = Pal.bg3, shape = RoundedCornerShape(15.dp),
                modifier = Modifier.fillMaxWidth().clickable { showAbout = !showAbout },
            ) {
                Column(Modifier.padding(15.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("ℹ️", fontSize = 17.sp)
                        Spacer(Modifier.width(12.dp))
                        Text(L.about, color = Pal.txt, fontSize = 14.5.sp, modifier = Modifier.weight(1f))
                        Text("2.0", color = Pal.txt3, fontSize = 12.sp)
                    }
                    if (showAbout) {
                        Spacer(Modifier.height(12.dp))
                        Text(L.aboutText, color = Pal.txt2, fontSize = 12.8.sp, lineHeight = 21.sp)
                    }
                }
            }

            Spacer(Modifier.height(26.dp))
            Text(
                L.madeIn + " 🇩🇿",
                color = Pal.txt3, fontSize = 11.5.sp,
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
            )
        }
    }
}
