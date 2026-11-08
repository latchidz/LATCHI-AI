package dz.latchi.ai

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dz.latchi.ai.data.ChatStore
import dz.latchi.ai.data.L
import dz.latchi.ai.data.Lang
import dz.latchi.ai.ui.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private var pickCallback: ((String) -> Unit)? = null

    private val picker = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            Thread {
                val b64 = readImage(uri)
                runOnUiThread { if (b64 != null) pickCallback?.invoke(b64) }
            }.start()
        }
    }

    private fun readImage(uri: Uri): String? = try {
        val input = contentResolver.openInputStream(uri) ?: return null
        var bytes = input.readBytes()
        input.close()
        // نصغّرو إذا كبير بزاف
        if (bytes.size > 1_800_000) {
            val bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (bmp != null) {
                val scale = 1400f / maxOf(bmp.width, bmp.height)
                val w = (bmp.width * scale).toInt().coerceAtLeast(1)
                val h = (bmp.height * scale).toInt().coerceAtLeast(1)
                val small = android.graphics.Bitmap.createScaledBitmap(bmp, w, h, true)
                val bos = java.io.ByteArrayOutputStream()
                small.compress(android.graphics.Bitmap.CompressFormat.JPEG, 88, bos)
                bytes = bos.toByteArray()
            }
        }
        Base64.encodeToString(bytes, Base64.NO_WRAP)
    } catch (e: Exception) { null }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val lang = ChatStore.lang(this)
        L.set(lang)
        Pal.dark = isSystemInDarkTheme()

        setContent {
            var splash by remember { mutableStateOf(true) }
            LaunchedEffect(Unit) { delay(1500); splash = false }

            LatchiTheme(dark = Pal.dark) {
                Surface(color = Pal.bg, modifier = Modifier.fillMaxSize()) {
                    if (splash) SplashScreen()
                    else {
                        val scope = rememberCoroutineScope()
                        val vm = remember {
                            ChatVm(scope) {
                                picker.launch(PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                        }
                        LaunchedEffect(Unit) { vm.init(this@MainActivity) }
                        var showSettings by remember { mutableStateOf(false) }

                        // ربط الاختيار بالـVM
                        LaunchedEffect(vm) {
                            pickCallback = { b64 -> vm.onImagePicked(b64) }
                        }

                        ChatScreen(vm, onOpenSettings = { showSettings = true })

                        if (showSettings) {
                            SettingsSheet(
                                onLang = { l ->
                                    L.set(l)
                                    ChatStore.setLang(this@MainActivity, l)
                                    showSettings = false
                                },
                                onClose = { showSettings = false },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * شاشة الترحيب — الهوية البصرية
 */
@Composable
fun SplashScreen() {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(120); visible = true }
    val a by animateFloatAsState(if (visible) 1f else 0f, tween(700), label = "a")
    val s by animateFloatAsState(if (visible) 1f else 0.9f, tween(700), label = "s")

    Box(Modifier.fillMaxSize().background(Color(0xFF05070D))) {
        // الخلفية: صورة الجبل بالشخصية
        Image(
            painter = painterResource(id = R.drawable.splash_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().alpha(a),
            contentScale = ContentScale.Crop,
        )
        // تدرّج باش النص يبان
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color(0xE605070D),
                    0.22f to Color(0x6605070D),
                    0.55f to Color(0x3305070D),
                    0.86f to Color(0xCC05070D),
                    1f to Color(0xFF05070D),
                )
            )
        )
        Column(
            Modifier.fillMaxSize().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(56.dp))
            Box(
                Modifier.size(74.dp).scale(s).clip(RoundedCornerShape(21.dp))
                    .background(Brush.linearGradient(listOf(Blue, Violet))),
                contentAlignment = Alignment.Center,
            ) {
                Text("L", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(18.dp))
            Text("Latchi AI", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Black,
                letterSpacing = 1.sp)
            Spacer(Modifier.height(6.dp))
            Text(L.tagline, color = Color(0xFFB9C6DE), fontSize = 13.sp)
            Spacer(Modifier.weight(1f))
            CircularProgressIndicator(color = Blue, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(14.dp))
            Text(L.madeIn + " 🇩🇿", color = Color(0xFF7C8AA5), fontSize = 11.sp)
            Spacer(Modifier.height(30.dp))
        }
    }
}
