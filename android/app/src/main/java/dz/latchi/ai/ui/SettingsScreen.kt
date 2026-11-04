package dz.latchi.ai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dz.latchi.ai.data.ModelInfo
import dz.latchi.ai.data.PRESETS
import dz.latchi.ai.data.Preset
import dz.latchi.ai.data.Provider
import dz.latchi.ai.data.Settings
import dz.latchi.ai.data.Store
import dz.latchi.ai.net.Api
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    store: Store,
    onClose: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var providers by remember { mutableStateOf(store.providers()) }
    var settings by remember { mutableStateOf(store.settings) }
    var editing by remember { mutableStateOf<Provider?>(null) }
    var adding by remember { mutableStateOf(false) }
    var testing by remember { mutableStateOf<String?>(null) }
    var toast by remember { mutableStateOf<String?>(null) }

    fun persist(list: List<Provider>) {
        providers = list.toMutableList()
        store.saveProviders(list)
    }

    Scaffold(
        containerColor = BgDeep,
        topBar = {
            TopAppBar(
                title = { Text("الإعدادات", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.ArrowBack, "رجوع", tint = TextMain)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BgSoft, titleContentColor = TextMain)
            )
        },
    ) { pad ->
        Column(
            Modifier.fillMaxSize().background(BgBrush).padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(14.dp)
        ) {
            // ═══ المزوّدون ═══
            SectionTitle("المزوّدون", Icons.Default.Cloud)
            Spacer(Modifier.height(8.dp))

            if (providers.isEmpty()) {
                GlassCard(Modifier.fillMaxWidth()) {
                    Text("ما كاينش مزوّد بعد.", fontSize = 13.sp, color = TextMain)
                    Spacer(Modifier.height(4.dp))
                    Text("زيد واحد وابدأ. OpenRouter يعطيك 14 موديل مجاني بمفتاح واحد.",
                        fontSize = 11.sp, color = TextMuted, lineHeight = 17.sp)
                }
            }

            providers.forEach { p ->
                GlassCard(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(34.dp).clip(RoundedCornerShape(11.dp))
                                .background(Violet.copy(alpha = .16f)),
                            contentAlignment = Alignment.Center,
                        ) { Text(p.name.take(1).uppercase(), color = Violet,
                            fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                        Spacer(Modifier.width(11.dp))
                        Column(Modifier.weight(1f)) {
                            Text(p.name, fontSize = 13.5.sp, color = TextMain,
                                fontWeight = FontWeight.Medium)
                            Text(p.baseUrl, fontSize = 9.5.sp, color = TextMuted,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (p.presetId == dz.latchi.ai.data.FREE_PRESET_ID) {
                            Surface(shape = RoundedCornerShape(7.dp),
                                color = OkGreen.copy(alpha = .15f)) {
                                Text("بلا مفتاح", fontSize = 9.sp, color = OkGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                            }
                            Spacer(Modifier.width(4.dp))
                        }
                        if (store.activeProviderId == p.id) {
                            Surface(shape = RoundedCornerShape(7.dp),
                                color = OkGreen.copy(alpha = .16f)) {
                                Text("نشط", fontSize = 9.sp, color = OkGreen,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MiniBtn("اختبار", Icons.Default.Bolt, testing == p.id) {
                            scope.launch {
                                testing = p.id
                                val r = Api.listModels(p)
                                testing = null
                                if (r.isSuccess) {
                                    store.saveModels(p.id, r.getOrThrow())
                                    toast = "✓ ${p.name}: ${r.getOrThrow().size} موديل"
                                } else toast = "✗ ${r.exceptionOrNull()?.message}"
                            }
                        }
                        MiniBtn("تعديل", Icons.Default.Edit, false) { editing = p }
                        if (store.activeProviderId != p.id)
                            MiniBtn("تنشيط", Icons.Default.Check, false) {
                                store.activeProviderId = p.id
                                toast = "✓ ${p.name} تنشّط"
                                providers = store.providers()
                            }
                        MiniBtn("حذف", Icons.Default.Delete, false, danger = true) {
                            persist(providers.filter { it.id != p.id })
                            if (store.activeProviderId == p.id) store.activeProviderId = ""
                        }
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
            Button(
                onClick = { adding = true },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Violet),
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("زيد مزوّد", fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(22.dp))

            // ═══ التوليد ═══
            SectionTitle("التوليد", Icons.Default.Tune)
            Spacer(Modifier.height(8.dp))
            GlassCard(Modifier.fillMaxWidth()) {
                Text("System prompt", fontSize = 12.sp, color = TextMuted)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = settings.systemPrompt,
                    onValueChange = {
                        settings = settings.copy(systemPrompt = it); store.settings = settings
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 90.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.5.sp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Violet, unfocusedBorderColor = Color.White.copy(alpha = .1f),
                        focusedTextColor = TextMain, unfocusedTextColor = TextMain,
                    ),
                )
                Spacer(Modifier.height(14.dp))
                Text("Temperature  ${"%.1f".format(settings.temperature)}", fontSize = 12.sp, color = TextMuted)
                Slider(
                    value = settings.temperature.toFloat(),
                    onValueChange = {
                        settings = settings.copy(temperature = it.toDouble()); store.settings = settings
                    },
                    valueRange = 0f..2f, steps = 19,
                    colors = SliderDefaults.colors(thumbColor = Violet, activeTrackColor = Violet),
                )
                Spacer(Modifier.height(4.dp))
                Text("Max tokens  ${settings.maxTokens}", fontSize = 12.sp, color = TextMuted)
                Slider(
                    value = settings.maxTokens.toFloat(),
                    onValueChange = {
                        settings = settings.copy(maxTokens = it.toInt()); store.settings = settings
                    },
                    valueRange = 256f..8192f, steps = 31,
                    colors = SliderDefaults.colors(thumbColor = Cyan, activeTrackColor = Cyan),
                )
            }

            Spacer(Modifier.height(30.dp))
            Text(
                "Latchi AI 1.0.0 · مفتوح المصدر\nموديلات سحابية · بلا استهلاك للجهاز",
                fontSize = 10.sp, color = TextMuted.copy(alpha = .6f),
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    if (adding || editing != null) {
        ProviderDialog(
            initial = editing,
            onDismiss = { adding = false; editing = null },
            onSave = { p ->
                val list = if (editing != null)
                    providers.map { if (it.id == p.id) p else it }
                else providers + p
                persist(list)
                if (store.activeProviderId.isBlank()) store.activeProviderId = p.id
                adding = false; editing = null
                toast = "✓ ${p.name} تحفظ"
            },
        )
    }

    toast?.let {
        LaunchedEffect(it) {
            kotlinx.coroutines.delay(2600)
            toast = null
        }
        Box(
            Modifier.fillMaxSize().padding(bottom = 40.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Surface(shape = RoundedCornerShape(14.dp), color = CardHi,
                shadowElevation = 8.dp) {
                Text(it, fontSize = 12.sp, color = TextMain,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp))
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Violet, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(7.dp))
        Text(text, fontSize = 13.sp, color = TextMain, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun MiniBtn(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    loading: Boolean,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (danger) ErrRed.copy(alpha = .1f) else CardHi,
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically) {
            if (loading) {
                CircularProgressIndicator(Modifier.size(11.dp), strokeWidth = 1.5.dp,
                    color = Violet)
            } else {
                Icon(icon, null, tint = if (danger) ErrRed else TextMuted,
                    modifier = Modifier.size(12.dp))
            }
            Spacer(Modifier.width(5.dp))
            Text(label, fontSize = 10.5.sp, color = if (danger) ErrRed else TextMuted)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProviderDialog(
    initial: Provider?,
    onDismiss: () -> Unit,
    onSave: (Provider) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var baseUrl by remember { mutableStateOf(initial?.baseUrl ?: "") }
    var apiKey by remember { mutableStateOf(initial?.apiKey ?: "") }
    var presetId by remember { mutableStateOf(initial?.presetId ?: "openrouter") }
    var showKey by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BgSoft,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(if (initial != null) "تعديل المزوّد" else "مزوّد جديد",
                fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("اختار مزوّد", fontSize = 11.sp, color = TextMuted)
                Spacer(Modifier.height(7.dp))
                PRESETS.forEach { pr ->
                    val on = presetId == pr.id
                    Row(
                        Modifier.fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(if (on) Violet.copy(alpha = .16f) else CardBg)
                            .clickable {
                                presetId = pr.id
                                if (pr.baseUrl.isNotBlank()) {
                                    name = pr.name; baseUrl = pr.baseUrl
                                }
                            }
                            .padding(horizontal = 11.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(pr.name, fontSize = 12.sp, color = TextMain,
                                fontWeight = FontWeight.Medium)
                            Text(pr.note, fontSize = 9.5.sp, color = TextMuted)
                        }
                        if (on) Icon(Icons.Default.CheckCircle, null, tint = Violet,
                            modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(Modifier.height(12.dp))
                Field("الاسم", name) { name = it }
                Spacer(Modifier.height(8.dp))
                Field("Base URL", baseUrl) { baseUrl = it }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                    visualTransformation = if (showKey) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        IconButton(onClick = { showKey = !showKey }) {
                            Icon(if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                null, tint = TextMuted, modifier = Modifier.size(17.dp))
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Violet, unfocusedBorderColor = Color.White.copy(alpha = .1f),
                        focusedTextColor = TextMain, unfocusedTextColor = TextMain,
                        focusedLabelColor = Violet, unfocusedLabelColor = TextMuted,
                    ),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank() && baseUrl.isNotBlank())
                        onSave(
                            Provider(
                                id = initial?.id ?: java.util.UUID.randomUUID().toString().take(8),
                                name = name.trim(),
                                baseUrl = baseUrl.trim().trimEnd('/'),
                                apiKey = apiKey.trim(),
                                presetId = presetId,
                            )
                        )
                },
                enabled = name.isNotBlank() && baseUrl.isNotBlank(),
            ) { Text("حفظ", color = Violet, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء", color = TextMuted) }
        },
    )
}

@Composable
private fun Field(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, fontSize = 11.sp) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Violet, unfocusedBorderColor = Color.White.copy(alpha = .1f),
            focusedTextColor = TextMain, unfocusedTextColor = TextMain,
            focusedLabelColor = Violet, unfocusedLabelColor = TextMuted,
        ),
    )
}

/** ورقة اختيار الموديل. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSheet(
    store: Store,
    current: String,
    onPick: (ModelInfo) -> Unit,
    onDismiss: () -> Unit,
) {
    var models by remember { mutableStateOf(store.models(store.activeProviderId)) }
    var query by remember { mutableStateOf("") }
    var freeOnly by remember { mutableStateOf(store.settings.freeOnly) }
    var loading by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun reload() {
        val p = store.providers().firstOrNull { it.id == store.activeProviderId } ?: run {
            err = "ما كاينش مزوّد نشط"; return
        }
        scope.launch {
            loading = true; err = null
            val r = Api.listModels(p)
            loading = false
            if (r.isSuccess) {
                models = r.getOrThrow()
                store.saveModels(p.id, models)
            } else err = r.exceptionOrNull()?.message
        }
    }

    LaunchedEffect(Unit) { if (models.isEmpty()) reload() }

    val filtered = models.filter {
        (!freeOnly || it.free) &&
                (query.isBlank() || it.id.contains(query, true) || it.name.contains(query, true))
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = BgSoft,
        dragHandle = { BottomSheetDefaults.DragHandle(color = TextMuted) },
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("اختار الموديل", fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    color = TextMain)
                Spacer(Modifier.weight(1f))
                Text("${filtered.size}", fontSize = 11.sp, color = TextMuted)
                Spacer(Modifier.width(10.dp))
                IconButton(onClick = { reload() }) {
                    if (loading) CircularProgressIndicator(Modifier.size(15.dp), strokeWidth = 1.5.dp,
                        color = Violet)
                    else Icon(Icons.Default.Refresh, "تحديث", tint = Violet,
                        modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("ابحث…", fontSize = 12.sp, color = TextMuted) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.5.sp),
                shape = RoundedCornerShape(13.dp),
                leadingIcon = { Icon(Icons.Default.Search, null, tint = TextMuted,
                    modifier = Modifier.size(17.dp)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Violet, unfocusedBorderColor = Color.White.copy(alpha = .09f),
                    focusedTextColor = TextMain, unfocusedTextColor = TextMain,
                ),
            )

            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = freeOnly,
                    onClick = {
                        freeOnly = !freeOnly
                        store.settings = store.settings.copy(freeOnly = freeOnly)
                    },
                    label = { Text("مجاني فقط", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = OkGreen.copy(alpha = .16f),
                        selectedLabelColor = OkGreen,
                        labelColor = TextMuted,
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true, selected = freeOnly,
                        borderColor = Color.White.copy(alpha = .08f),
                        selectedBorderColor = OkGreen.copy(alpha = .4f),
                    ),
                )
            }

            err?.let {
                Text("⚠️ $it", fontSize = 11.sp, color = ErrRed,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            }

            if (filtered.isEmpty() && !loading) {
                Text(
                    if (models.isEmpty()) "اضغط 🔄 باش تجيب القائمة من المزوّد"
                    else "ما كاينش نتيجة",
                    fontSize = 12.sp, color = TextMuted,
                    modifier = Modifier.padding(24.dp),
                )
            }

            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 460.dp)) {
                items(filtered, key = { it.id }) { m ->
                    ModelRow(m = m, selected = m.id == current) {
                        onPick(m); onDismiss()
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}
