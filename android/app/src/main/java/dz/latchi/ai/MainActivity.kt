package dz.latchi.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dz.latchi.ai.data.Chat
import dz.latchi.ai.data.ModelInfo
import dz.latchi.ai.data.Store
import dz.latchi.ai.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LatchiAITheme { App() }
        }
    }
}

@Composable
private fun App() {
    val ctx = LocalContext.current
    val store = remember {
        Store(ctx).also { it.ensureDefault() }
    }

    var chats by remember { mutableStateOf(store.chats()) }
    var current by remember { mutableStateOf<Chat?>(chats.firstOrNull()) }
    var showSettings by remember { mutableStateOf(false) }
    var showDrawer by remember { mutableStateOf(false) }
    var showModels by remember { mutableStateOf(false) }
    var model by remember { mutableStateOf(store.activeModel) }
    var ctxLabel by remember { mutableStateOf("") }

    val providers = remember { store.providers() }
    val active = providers.firstOrNull { it.id == store.activeProviderId }

    // إذا ما كاينش محادثة، نصنعو وحدة
    LaunchedEffect(Unit) {
        if (current == null) {
            val c = Chat()
            store.saveChat(c)
            chats = store.chats()
            current = c
        }
        store.ensureDefault()
        // جيب تسمية السياق للموديل المختار
        val cached = store.models(store.activeProviderId)
        cached.firstOrNull { it.id == model }?.let { ctxLabel = it.ctxHuman() }
    }

    // الشاشة الرئيسية
    Box(Modifier.fillMaxSize().background(BgDeep)) {
        val c = current
        if (c != null && !showSettings) {
            ChatScreen(
                store = store,
                chat = c,
                providerName = active?.name ?: "",
                model = model,
                modelCtx = ctxLabel,
                onChatChange = { updated ->
                    current = updated
                    store.saveChat(updated)
                    chats = store.chats()
                },
                onNewChat = {
                    val nc = Chat()
                    store.saveChat(nc)
                    chats = store.chats()
                    current = nc
                    showDrawer = false
                },
                onOpenDrawer = { showDrawer = true },
                onPickModel = { showModels = true },
                onSettings = { showSettings = true },
            )
        } else if (c == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Violet)
            }
        }

        if (showSettings) {
            SettingsScreen(store = store, onClose = { showSettings = false })
        }

        // ═══ الدرج ═══
        if (showDrawer) {
            Drawer(
                chats = chats,
                currentId = current?.id ?: "",
                onPick = { ch ->
                    current = ch
                    store.activeProviderId = ch.providerId.ifBlank { store.activeProviderId }
                    if (ch.model.isNotBlank()) model = ch.model
                    showDrawer = false
                },
                onDelete = { ch ->
                    store.deleteChat(ch.id)
                    chats = store.chats()
                    if (current?.id == ch.id) {
                        val nc = chats.firstOrNull() ?: Chat().also { store.saveChat(it) }
                        current = nc
                        chats = store.chats()
                    }
                },
                onNew = {
                    val nc = Chat()
                    store.saveChat(nc)
                    chats = store.chats()
                    current = nc
                    showDrawer = false
                },
                onSettings = { showSettings = true; showDrawer = false },
                onClose = { showDrawer = false },
            )
        }

        // ═══ اختيار الموديل ═══
        if (showModels) {
            ModelSheet(
                store = store,
                current = model,
                onPick = { m: ModelInfo ->
                    model = m.id
                    store.activeModel = m.id
                    ctxLabel = m.ctxHuman()
                    current?.let { ch ->
                        store.saveChat(ch.copy(model = m.id))
                        current = ch.copy(model = m.id)
                    }
                },
                onDismiss = { showModels = false },
            )
        }
    }
}

@Composable
private fun Drawer(
    chats: List<Chat>,
    currentId: String,
    onPick: (Chat) -> Unit,
    onDelete: (Chat) -> Unit,
    onNew: () -> Unit,
    onSettings: () -> Unit,
    onClose: () -> Unit,
) {
    Box(
        Modifier.fillMaxSize()
            .background(Color.Black.copy(alpha = .55f))
            .clickable { onClose() }
    ) {
        Column(
            Modifier
                .fillMaxHeight()
                .width(300.dp)
                .background(BgSoft)
                .clickable(enabled = false) {}
                .statusBarsPadding()
        ) {
            // رأس
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BrandMark(38)
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text("Latchi AI", fontSize = 15.sp, fontWeight = FontWeight.Bold,
                        color = TextMain)
                    Text("${chats.size} محادثة", fontSize = 10.sp, color = TextMuted)
                }
            }

            // جديد
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(Violet.copy(alpha = .16f))
                    .clickable { onNew() }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Add, null, tint = Violet, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(9.dp))
                Text("محادثة جديدة", fontSize = 12.5.sp, color = Violet,
                    fontWeight = FontWeight.Medium)
            }

            Spacer(Modifier.height(12.dp))
            Text("السجل", fontSize = 10.sp, color = TextMuted,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp))

            LazyColumn(Modifier.weight(1f)) {
                items(chats, key = { it.id }) { ch ->
                    val on = ch.id == currentId
                    Row(
                        Modifier.fillMaxWidth()
                            .background(if (on) Violet.copy(alpha = .1f) else Color.Transparent)
                            .clickable { onPick(ch) }
                            .padding(horizontal = 16.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.ChatBubbleOutline, null,
                            tint = if (on) Violet else TextMuted, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(ch.title, fontSize = 12.5.sp,
                                color = TextMain, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${ch.messages.size} رسالة", fontSize = 9.5.sp, color = TextMuted)
                        }
                        Icon(Icons.Default.DeleteOutline, "حذف", tint = TextMuted,
                            modifier = Modifier.size(15.dp).clickable { onDelete(ch) })
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth()
                    .clickable { onSettings() }
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Settings, null, tint = TextMuted, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(10.dp))
                Text("الإعدادات", fontSize = 12.5.sp, color = TextMuted)
            }
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}
