package dz.latchi.ai.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dz.latchi.ai.data.Chat
import dz.latchi.ai.data.Msg
import dz.latchi.ai.data.ModelInfo
import dz.latchi.ai.data.Store
import dz.latchi.ai.net.Api
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    store: Store,
    chat: Chat,
    providerName: String,
    model: String,
    modelCtx: String,
    onChatChange: (Chat) -> Unit,
    onNewChat: () -> Unit,
    onOpenDrawer: () -> Unit,
    onPickModel: () -> Unit,
    onSettings: () -> Unit,
) {
    val ctx = LocalContext.current
    val clip = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var messages by remember(chat.id) { mutableStateOf(chat.messages) }
    var input by remember { mutableStateOf("") }
    var attached by remember { mutableStateOf<String?>(null) }   // base64
    var streaming by remember { mutableStateOf(false) }
    var job by remember { mutableStateOf<Job?>(null) }
    var showChatMenu by remember { mutableStateOf(false) }

    val settings = remember { store.settings }
    val provider = remember { store.providers().firstOrNull { it.id == store.activeProviderId } }

    // حفظ تلقائي
    LaunchedEffect(messages) {
        if (messages !== chat.messages) {
            onChatChange(chat.copy(messages = messages, updated = System.currentTimeMillis()))
        }
    }

    // تمرير لتحت
    LaunchedEffect(messages.size, streaming) {
        if (messages.isNotEmpty()) {
            runCatching { listState.animateScrollToItem(messages.lastIndex) }
        }
    }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            val bmp = ctx.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it)
            } ?: return@rememberLauncherForActivityResult
            // تصغير باش ما نبعثوش صورة ضخمة
            val max = 1024
            val w = bmp.width; val h = bmp.height
            val scaled = if (w > max || h > max) {
                val r = max.toFloat() / maxOf(w, h)
                Bitmap.createScaledBitmap(bmp, (w * r).toInt(), (h * r).toInt(), true)
            } else bmp
            val bos = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 82, bos)
            attached = Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP)
        } catch (_: Exception) {}
    }

    fun stop() {
        job?.cancel(); job = null; streaming = false
    }

    fun send() {
        val txt = input.trim()
        if ((txt.isEmpty() && attached == null) || streaming) return
        val p = provider ?: run {
            messages = messages + Msg("assistant", "⚠️ ما كاينش مزوّد. روح للإعدادات وزيد واحد.", error = true)
            return
        }
        if (provider.apiKey.isBlank()) {
            messages = messages + Msg("assistant", "⚠️ المفتاح فارغ. روح للإعدادات ودخّل API Key.", error = true)
            return
        }
        if (model.isBlank()) {
            messages = messages + Msg("assistant", "⚠️ ما اختريتش موديل. اضغط على شريحة الموديل فـالفوق.", error = true)
            return
        }

        val userMsg = Msg("user", txt, image = attached)
        val withUser = messages + userMsg
        messages = withUser + Msg("assistant", "")
        input = ""
        val img = attached
        attached = null
        streaming = true

        job = scope.launch {
            val history = withUser.takeLast(24)
            val acc = StringBuilder()
            var lastPush = 0L

            val res = Api.chat(
                p = p,
                model = model,
                history = history,
                system = settings.systemPrompt,
                temperature = settings.temperature,
                maxTokens = settings.maxTokens,
            ) { delta ->
                acc.append(delta)
                val now = System.currentTimeMillis()
                if (now - lastPush > 40) {
                    lastPush = now
                    messages = messages.dropLast(1) + Msg("assistant", acc.toString())
                }
            }

            val final = if (res.isSuccess) {
                Msg("assistant", acc.toString().ifBlank { res.getOrNull() ?: "" })
            } else {
                val e = res.exceptionOrNull()?.message ?: "خطأ"
                Msg("assistant", if (acc.isEmpty()) "⚠️ $e" else acc.toString() + "\n\n⚠️ $e", error = true)
            }
            messages = messages.dropLast(1) + final
            streaming = false
            img?.let { }   // الصورة تبقى فـالرسالة
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Column(Modifier.background(Color(0xF00B0D18)).statusBarsPadding()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, "المحادثات", tint = TextMain)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Latchi AI",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMain,
                        )
                        Text(
                            providerName.ifBlank { "ما كاينش مزوّد" },
                            fontSize = 10.sp,
                            color = if (provider != null) OkGreen else TextMuted,
                        )
                    }
                    IconButton(onClick = onNewChat) {
                        Icon(Icons.Default.Edit, "جديد", tint = Violet, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Tune, "إعدادات", tint = TextMain, modifier = Modifier.size(21.dp))
                    }
                }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp).padding(bottom = 8.dp),
                ) {
                    ModelChip(
                        model = model.ifBlank { "اختار موديل" },
                        ctx = modelCtx,
                        onClick = onPickModel,
                    )
                }
            }
        },
        bottomBar = {
            InputBar(
                input = input,
                onInput = { input = it },
                streaming = streaming,
                hasImage = attached != null,
                onAttach = { pickImage.launch("image/*") },
                onSend = { send() },
                onStop = { stop() },
            )
        },
    ) { pad ->
        Box(
            Modifier
                .fillMaxSize()
                .background(BgBrush)
                .padding(pad)
        ) {
            if (messages.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.AutoAwesome,
                    title = "ابدا المحادثة",
                    sub = "اكتب سؤالك، ولا ابعث صورة.\nالموديل: ${model.ifBlank { "ماشي مختار" }}",
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(messages, key = { it.at.toString() + it.role + it.content.hashCode() }) { m ->
                        MessageBubble(
                            m = m,
                            streaming = streaming && m === messages.lastOrNull() && m.role == "assistant",
                            onCopy = { clip.setText(AnnotatedString(m.content)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(m: Msg, streaming: Boolean, onCopy: () -> Unit) {
    val isUser = m.role == "user"
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        if (!isUser) {
            Box(
                Modifier.size(28.dp).clip(CircleShape).background(BrandBrush),
                contentAlignment = Alignment.Center,
            ) { Text("L", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black) }
            Spacer(Modifier.width(8.dp))
        }

        Column(
            Modifier.widthIn(max = 320.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
        ) {
            // صورة مرفقة
            m.image?.let { b64 ->
                val bmp = remember(b64) {
                    runCatching {
                        val bytes = Base64.decode(b64, Base64.NO_WRAP)
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    }.getOrNull()
                }
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .widthIn(max = 200.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.FillWidth,
                    )
                    Spacer(Modifier.height(6.dp))
                }
            }

            Surface(
                shape = RoundedCornerShape(
                    topStart = 18.dp, topEnd = 18.dp,
                    bottomStart = if (isUser) 18.dp else 5.dp,
                    bottomEnd = if (isUser) 5.dp else 18.dp,
                ),
                color = if (isUser) Color.Transparent else CardBg,
                border = if (isUser) null
                else androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = .06f)),
            ) {
                Box(
                    Modifier
                        .background(if (isUser) UserBrush else Brush.linearGradient(listOf(CardBg, CardBg)))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    if (m.content.isEmpty() && streaming) {
                        TypingDots()
                    } else {
                        SelectionContainer {
                            Text(
                                text = if (isUser) AnnotatedString(m.content)
                                else mdToAnnotated(m.content),
                                fontSize = 14.sp,
                                lineHeight = 21.sp,
                                color = if (m.error) ErrRed
                                else if (isUser) Color.White else TextMain,
                            )
                        }
                    }
                }
            }

            if (!streaming && m.content.isNotEmpty()) {
                Row(
                    Modifier.padding(top = 3.dp, start = 4.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        android.text.format.DateFormat.format("HH:mm", m.at).toString(),
                        fontSize = 9.sp,
                        color = TextMuted.copy(alpha = .7f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.Default.ContentCopy, "نسخ",
                        tint = TextMuted.copy(alpha = .6f),
                        modifier = Modifier.size(12.dp).clickable { onCopy() },
                    )
                }
            }
        }
    }
}

@Composable
private fun InputBar(
    input: String,
    onInput: (String) -> Unit,
    streaming: Boolean,
    hasImage: Boolean,
    onAttach: () -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color(0xF00B0D18))
            .navigationBarsPadding()
            .imePadding()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(if (hasImage) Violet.copy(alpha = .25f) else CardHi)
                    .clickable { onAttach() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Image, "صورة",
                    tint = if (hasImage) Violet else TextMuted,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(8.dp))

            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                color = CardBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = .07f)),
            ) {
                TextField(
                    value = input,
                    onValueChange = onInput,
                    placeholder = {
                        Text("اكتب رسالة…", color = TextMuted.copy(alpha = .8f), fontSize = 14.sp)
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp, max = 130.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = TextMain,
                        unfocusedTextColor = TextMain,
                        cursorColor = Violet,
                    ),
                    maxLines = 6,
                )
            }
            Spacer(Modifier.width(8.dp))

            Box(
                Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(if (streaming) ErrRed else BrandBrush)
                    .clickable { if (streaming) onStop() else onSend() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (streaming) Icons.Default.Stop else Icons.Default.ArrowUpward,
                    if (streaming) "وقف" else "إرسال",
                    tint = Color.White,
                    modifier = Modifier.size(21.dp),
                )
            }
        }
        if (hasImage) {
            Row(
                Modifier.padding(horizontal = 14.dp).padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.AttachFile, null, tint = Violet, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(4.dp))
                Text("صورة مرفقة", fontSize = 10.sp, color = Violet)
            }
        }
    }
}
