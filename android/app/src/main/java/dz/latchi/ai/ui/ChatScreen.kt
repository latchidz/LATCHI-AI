package dz.latchi.ai.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dz.latchi.ai.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.text.TextStyle

// ═══════════════════════════════════════════════════════════
//  الشاشة الرئيسية
// ═══════════════════════════════════════════════════════════
@Composable
fun ChatScreen(vm: ChatVm, onOpenSettings: () -> Unit) {
    val ctx = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Pal.bg,
        topBar = {
            TopBar(
                title = vm.activeTitle(),
                onMenu = { showMenu = true },
                onNew = { vm.newChat(ctx) },
                onSettings = onOpenSettings,
            )
        },
        bottomBar = { Composer(vm) },
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            if (vm.msgs.isEmpty()) EmptyState(onPick = { vm.send(ctx, it) })
            else ChatList(vm)
        }
    }

    if (showMenu) {
        DrawerSheet(
            chats = vm.chats,
            activeId = vm.activeId,
            onPick = { showMenu = false; vm.open(ctx, it) },
            onDelete = { vm.remove(ctx, it) },
            onClose = { showMenu = false },
        )
    }
}

// ═══════════════════════════════════════════════════════════
//  الشريط العلوي — الواجهة الاحترافية
// ═══════════════════════════════════════════════════════════
@Composable
private fun TopBar(title: String, onMenu: () -> Unit, onNew: () -> Unit, onSettings: () -> Unit) {
    Surface(color = Pal.bg2, tonalElevation = 0.dp) {
        Column {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().height(58.dp).padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconBtn(Icons.Filled.Menu, onMenu)
                Spacer(Modifier.width(6.dp))
                Box(
                    Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(BrandBrush),
                    contentAlignment = Alignment.Center,
                ) { Text("L", color = Color.White, fontWeight = FontWeight.Black, fontSize = 17.sp) }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(L.appName, color = Pal.txt, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(
                        title.ifBlank { L.tagline },
                        color = Pal.txt3, fontSize = 11.sp, maxLines = 1,
                    )
                }
                IconBtn(Icons.Filled.AddComment, onNew)
                IconBtn(Icons.Filled.Settings, onSettings)
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Pal.line))
        }
    }
}

@Composable
fun IconBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = Pal.txt2, modifier = Modifier.size(21.dp)) }
}

// ═══════════════════════════════════════════════════════════
//  الشاشة الفارغة
// ═══════════════════════════════════════════════════════════
@Composable
private fun EmptyState(onPick: (String) -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(72.dp).clip(RoundedCornerShape(22.dp)).background(BrandBrush),
            contentAlignment = Alignment.Center,
        ) {
            Text("L", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(18.dp))
        Text(L.hello, color = Pal.txt, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            L.helloSub, color = Pal.txt2, fontSize = 13.5.sp,
            textAlign = TextAlign.Center, lineHeight = 21.sp,
        )
        Spacer(Modifier.height(26.dp))
        val items = listOf(
            Triple("🎓", L.sug1Title, L.sug1),
            Triple("🎨", L.sug2Title, L.sug2),
            Triple("💻", L.sug3Title, L.sug3),
            Triple("🌐", L.sug4Title, L.sug4),
        )
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            items.forEach { (em, ttl, txt) ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Pal.bg2,
                    modifier = Modifier.fillMaxWidth().clickable { onPick(txt) },
                ) {
                    Row(
                        Modifier.padding(13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(em, fontSize = 19.sp)
                        Spacer(Modifier.width(11.dp))
                        Column {
                            Text(ttl, color = Pal.txt, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(txt, color = Pal.txt3, fontSize = 11.5.sp, maxLines = 2, lineHeight = 16.sp)
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
//  قائمة الرسائل
// ═══════════════════════════════════════════════════════════
@Composable
private fun ChatList(vm: ChatVm) {
    val listState = rememberLazyListState()
    LaunchedEffect(vm.msgs.size, vm.streamText.length) {
        if (vm.msgs.isNotEmpty()) {
            listState.animateScrollToItem((vm.msgs.size - 1).coerceAtLeast(0))
        }
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(vm.msgs, key = { it.ts.toString() + it.role + it.text.length }) { m ->
            MessageRow(m)
        }
        if (vm.streaming) {
            item {
                Column {
                    if (vm.streamText.isNotEmpty()) {
                        AssistantBubble(Msg("assistant", vm.streamText, model = vm.streamModel), live = true)
                    } else {
                        TypingRow(vm.statusText.ifBlank { L.thinking })
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageRow(m: Msg) {
    if (m.role == "user") UserBubble(m) else AssistantBubble(m)
}

@Composable
private fun UserBubble(m: Msg) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Spacer(Modifier.width(40.dp))
        Surface(color = Pal.userBubble, shape = RoundedCornerShape(16.dp, 5.dp, 16.dp, 16.dp)) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                if (m.image != null) {
                    Base64Image(m.image, Modifier.padding(bottom = 8.dp))
                }
                if (m.text.isNotBlank())
                    Text(m.text, color = Pal.txt, fontSize = 15.sp, lineHeight = 24.sp)
            }
        }
    }
}

@Composable
private fun AssistantBubble(m: Msg, live: Boolean = false) {
    val clip = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth()) {
        Box(
            Modifier.size(29.dp).clip(RoundedCornerShape(9.dp)).background(BrandBrush),
            contentAlignment = Alignment.Center,
        ) { Text("L", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp) }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            if (m.text.isNotBlank()) {
                Markdown(m.text, Pal.txt)
                if (live) Cursor()
            }
            if (m.image != null) {
                Spacer(Modifier.height(8.dp))
                Base64Image(m.image, Modifier.fillMaxWidth(0.92f))
            }
            if (!live && m.text.isNotBlank()) {
                Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (copied) L.copied else L.copy,
                        color = if (copied) Green else Pal.txt3, fontSize = 11.sp,
                        modifier = Modifier.clickable {
                            clip.setText(AnnotatedString(m.text))
                            copied = true
                        }.padding(4.dp),
                    )
                }
            }
        }
        Spacer(Modifier.width(30.dp))
    }
}

@Composable
private fun Cursor() {
    val t = rememberInfiniteTransition(label = "c")
    val a by t.animateFloat(
        1f, 0f, infiniteRepeatable(tween(760, easing = LinearEasing), RepeatMode.Reverse),
        label = "a",
    )
    Box(Modifier.padding(top = 3.dp).size(width = 2.dp, height = 15.dp)
        .alpha(a).background(Blue))
}

@Composable
private fun TypingRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(29.dp).clip(RoundedCornerShape(9.dp)).background(BrandBrush),
            contentAlignment = Alignment.Center,
        ) { Text("L", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp) }
        Spacer(Modifier.width(10.dp))
        val t = rememberInfiniteTransition(label = "t")
        val a by t.animateFloat(0.35f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "a")
        Text(text, color = Pal.txt2, fontSize = 13.5.sp, modifier = Modifier.alpha(a))
    }
}

@Composable
fun Base64Image(b64: String, modifier: Modifier = Modifier) {
    var bmp by remember(b64) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(b64) {
        bmp = withContext(Dispatchers.IO) {
            try {
                val raw = Base64.decode(b64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(raw, 0, raw.size)
            } catch (e: Exception) { null }
        }
    }
    val b = bmp
    if (b != null) {
        Image(
            bitmap = b.asImageBitmap(),
            contentDescription = null,
            modifier = modifier.clip(RoundedCornerShape(13.dp)).border(1.dp, Pal.line2, RoundedCornerShape(13.dp)),
            contentScale = ContentScale.FillWidth,
        )
    } else {
        Box(
            Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(13.dp))
                .background(Pal.bg3), contentAlignment = Alignment.Center,
        ) { CircularProgressIndicator(color = Blue, modifier = Modifier.size(24.dp), strokeWidth = 2.dp) }
    }
}

// ═══════════════════════════════════════════════════════════
//  حقل الإدخال
// ═══════════════════════════════════════════════════════════
@Composable
private fun Composer(vm: ChatVm) {
    val ctx = LocalContext.current
    Column(Modifier.background(Pal.bg).navigationBarsPadding()) {
        AnimatedVisibility(visible = vm.attached != null) {
            vm.attached?.let { b64 ->
                Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp)) {
                    Box {
                        Base64Image(b64, Modifier.size(62.dp))
                        Box(
                            Modifier.align(Alignment.TopEnd).size(20.dp).clip(CircleShape)
                                .background(Color(0xCC000000)).clickable { vm.attached = null },
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Filled.Close, null, tint = Color.White, modifier = Modifier.size(13.dp)) }
                    }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(20.dp))
                    .background(Pal.bg2).border(1.dp, Pal.line2, RoundedCornerShape(20.dp)),
            ) {
                Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(5.dp)) {
                    Box(
                        Modifier.size(38.dp).clip(CircleShape)
                            .clickable(enabled = !vm.streaming) { vm.pickImage() },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Filled.AddPhotoAlternate, null, tint = Pal.txt2, modifier = Modifier.size(21.dp)) }
                    BasicTextField(
                        value = vm.input,
                        onValueChange = { vm.input = it },
                        textStyle = TextStyle(color = Pal.txt, fontSize = 15.sp, lineHeight = 22.sp),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(Blue),
                        maxLines = 6,
                        modifier = Modifier.weight(1f).padding(vertical = 9.dp, horizontal = 2.dp),
                        decorationBox = { inner ->
                            if (vm.input.isEmpty())
                                Text(L.inputHint, color = Pal.txt3, fontSize = 14.5.sp)
                            inner()
                        },
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            val can = !vm.streaming && (vm.input.isNotBlank() || vm.attached != null)
            Box(
                Modifier.size(45.dp).clip(CircleShape)
                    .background(if (can) BrandBrush else Brush.linearGradient(
                        listOf(Pal.bg3, Pal.bg3)))
                    .clickable(enabled = can) { vm.send(ctx, vm.input) },
                contentAlignment = Alignment.Center,
            ) {
                if (vm.streaming)
                    CircularProgressIndicator(color = Blue, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                else
                    Icon(Icons.Filled.ArrowUpward, null, tint = if (can) Color.White else Pal.txt3,
                        modifier = Modifier.size(21.dp))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
//  درج المحادثات
// ═══════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DrawerSheet(
    chats: List<Chat>,
    activeId: String,
    onPick: (String) -> Unit,
    onDelete: (String) -> Unit,
    onClose: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onClose, containerColor = Pal.bg2) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 28.dp)) {
            Text(L.newChat, color = Pal.txt, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            if (chats.isEmpty()) {
                Text("—", color = Pal.txt3, fontSize = 13.sp)
            } else {
                LazyColumn(Modifier.heightIn(max = 380.dp)) {
                    items(chats, key = { it.id }) { c ->
                        Row(
                            Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (c.id == activeId) Pal.bg3 else Color.Transparent)
                                .clickable { onPick(c.id) }
                                .padding(horizontal = 12.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                c.title.ifBlank { L.newChat },
                                color = Pal.txt, fontSize = 13.5.sp,
                                modifier = Modifier.weight(1f), maxLines = 1,
                            )
                            Icon(
                                Icons.Filled.DeleteOutline, null, tint = Pal.txt3,
                                modifier = Modifier.size(18.dp).clickable { onDelete(c.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}
