// by: H3NRI_013/girafinha67
package com.led.controle.ui

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.led.controle.ControllerModel
import com.led.controle.GamepadHub
import com.led.controle.PHOTO_GRAYSCALE
import com.led.controle.R
import kotlinx.coroutines.delay

// ======================= Fundo (grafite + granulado) =======================

private val noiseBitmap: ImageBitmap by lazy {
    val size = 160
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val rnd = java.util.Random(13)
    val px = IntArray(size * size) {
        val v = rnd.nextInt(256)
        val a = rnd.nextInt(28)
        (a shl 24) or (v shl 16) or (v shl 8) or v
    }
    bmp.setPixels(px, 0, size, 0, 0, size, size)
    bmp.asImageBitmap()
}

@Composable
fun AppBackground(content: @Composable () -> Unit) {
    val grain = remember { ShaderBrush(ImageShader(noiseBitmap, TileMode.Repeated, TileMode.Repeated)) }
    Box(Modifier.fillMaxSize()) {
        // Fundo em camada própria: não é redesenhado quando os elementos da tela animam.
        Box(
            Modifier.fillMaxSize().graphicsLayer { }
                .background(Brush.verticalGradient(listOf(Color(0xFF1C1C1C), Color(0xFF0B0B0B), Color(0xFF050505))))
                .drawBehind { drawRect(grain) }
        )
        content()
    }
}

// ======================= Animações reutilizáveis =======================

class AppearCounter { var n = 0 }

val LocalAppear = compositionLocalOf<AppearCounter?> { null }

/** Entrada suave: sobe e aparece, com atraso escalonado por índice. */
@Composable
fun Modifier.appear(index: Int = 0, offsetY: Dp = 36.dp): Modifier {
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(minOf(index, 9) * 70L)
        p.animateTo(1f, tween(480, easing = FastOutSlowInEasing))
    }
    return this.graphicsLayer {
        alpha = p.value
        translationY = (1f - p.value) * offsetY.toPx()
    }
}

/** Clique com "mola": encolhe ao tocar e volta com um leve quique. */
fun Modifier.bounceClick(enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val s by animateFloatAsState(
        if (pressed) 0.94f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "press"
    )
    this
        .graphicsLayer { scaleX = s; scaleY = s }
        .clickable(interactionSource = src, indication = null, enabled = enabled, onClick = onClick)
}

// ======================= Foto do controle =======================

@Composable
fun ControllerPhoto(model: ControllerModel, modifier: Modifier = Modifier) {
    val filter = remember {
        if (PHOTO_GRAYSCALE) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) else null
    }
    val res = model.photo
    if (res != null) {
        Image(painterResource(res), model.label, modifier, contentScale = ContentScale.Fit, colorFilter = filter)
    } else {
        Image(
            painterResource(R.drawable.app_logo), "Controler Tester",
            modifier.clip(RoundedCornerShape(22.dp)).alpha(0.6f), contentScale = ContentScale.Fit
        )
    }
}

// ======================= Cabeçalho das telas =======================

/** Sublinhado curvo, como o traço sob o "ABOUT" da referência. */
@Composable
fun Swoosh(modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { delay(180); progress.animateTo(1f, tween(650, easing = FastOutSlowInEasing)) }
    Canvas(modifier.height(10.dp)) {
        val w = size.width * progress.value
        val path = Path().apply {
            moveTo(0f, size.height * 0.6f)
            quadraticBezierTo(w * 0.5f, size.height * 1.15f, w, size.height * 0.25f)
        }
        drawPath(path, Color.White, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
fun ScreenHeader(title: String, onBack: () -> Unit, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 4.dp).appear(0, 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(42.dp).clip(CircleShape).background(Color(0x22FFFFFF)).border(1.dp, Hairline, CircleShape)
                .bounceClick(onClick = onBack),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", Modifier.size(22.dp), tint = Color.White) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title.uppercase() + ";", fontFamily = Condensed, fontWeight = FontWeight.Bold,
                fontSize = 27.sp, lineHeight = 28.sp, maxLines = 2, overflow = TextOverflow.Ellipsis
            )
            Swoosh(Modifier.fillMaxWidth(0.5f))
        }
        actions()
    }
}

/** Tela padrão de ferramenta: cabeçalho com voltar + conteúdo rolável com entrada animada. */
@Composable
fun ToolScaffold(
    title: String,
    onBack: () -> Unit,
    showDevice: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        ScreenHeader(title, onBack, actions)
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CompositionLocalProvider(LocalAppear provides remember { AppearCounter() }) {
                if (showDevice) DeviceBar()
                content()
            }
            Spacer(Modifier.navigationBarsPadding().height(24.dp))
        }
    }
}

@Composable
fun SectionCard(title: String? = null, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val counter = LocalAppear.current
    val idx = remember { counter?.let { val i = it.n; it.n = i + 1; i } ?: 0 }
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier.fillMaxWidth().appear(idx).clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFF1F1F1F), Color(0xFF121212))))
            .border(1.dp, Hairline, shape).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (title != null) Text(
            title.uppercase(), fontFamily = Condensed, fontWeight = FontWeight.Bold,
            fontSize = 19.sp, letterSpacing = 0.5.sp
        )
        content()
    }
}

@Composable
fun Hint(text: String) = Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, lineHeight = 20.sp)

/** Seletor do controle ativo, com a foto do modelo conectado. */
@Composable
fun DeviceBar() {
    val devs = GamepadHub.devices
    val sel = GamepadHub.selected
    val model = ControllerModel.detect(sel)
    var open by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(22.dp)
    val counter = LocalAppear.current
    val idx = remember { counter?.let { val i = it.n; it.n = i + 1; i } ?: 0 }
    val pulse = rememberInfiniteTransition(label = "dot")
    val dotAlpha by pulse.animateFloat(
        0.35f, 1f, infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse), label = "dotA"
    )
    Box(Modifier.fillMaxWidth().appear(idx)) {
        Row(
            Modifier.fillMaxWidth().clip(shape)
                .background(Brush.verticalGradient(listOf(Color(0xFF1F1F1F), Color(0xFF121212))))
                .border(1.dp, Hairline, shape)
                .clickable(enabled = devs.size > 1) { open = true }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.width(68.dp).height(46.dp), contentAlignment = Alignment.Center) {
                AnimatedContent(
                    targetState = model,
                    transitionSpec = { (fadeIn(tween(420)) + scaleIn(initialScale = 0.7f, animationSpec = tween(420))) togetherWith fadeOut(tween(180)) },
                    label = "miniPhoto"
                ) { m -> ControllerPhoto(m, Modifier.fillMaxSize()) }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    sel?.name ?: "Nenhum controle conectado", fontFamily = Condensed, fontWeight = FontWeight.Bold,
                    fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (sel != null) "Pressione um botão para testar" + if (devs.size > 1) " · toque para trocar" else ""
                    else "Conecte por USB/Bluetooth e mexa em um botão",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp
                )
            }
            if (devs.size > 1) Icon(Icons.Filled.SwapHoriz, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier.size(9.dp).graphicsLayer { alpha = if (sel != null) dotAlpha else 1f }.clip(CircleShape)
                    .background(if (sel != null) Color.White else Color(0xFF444444))
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            devs.forEach { d ->
                DropdownMenuItem(text = { Text(d.name) }, onClick = { GamepadHub.select(d.id); open = false })
            }
        }
    }
}

@Composable
fun StatusChip(label: String, active: Boolean, modifier: Modifier = Modifier, activeColor: Color = MaterialTheme.colorScheme.primary) {
    val bg by animateColorAsStateCompat(if (active) activeColor else MaterialTheme.colorScheme.surfaceContainerHigh)
    val fg = if (active) InkBlack else MaterialTheme.colorScheme.onSurfaceVariant
    val scale by animateFloatAsState(if (active) 1.04f else 1f, spring(Spring.DampingRatioMediumBouncy), label = "chip")
    Box(
        modifier.graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(12.dp)).background(bg)
            .border(1.dp, if (active) Color.Transparent else Hairline, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) { Text(label, color = fg, fontSize = 13.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center) }
}

@Composable
private fun animateColorAsStateCompat(target: Color): State<Color> =
    androidx.compose.animation.animateColorAsState(target, tween(180), label = "chipColor")

@Composable
fun BarMeter(label: String, value: Float, modifier: Modifier = Modifier, valueText: String = "%.2f".format(value)) {
    val anim by animateFloatAsState(value.coerceIn(0f, 1f), tween(90), label = "bar")
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(valueText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { anim },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }
}

/** Analógico: círculo, mira, zona morta tracejada, rastro opcional e ponto atual. */
@Composable
fun StickView(x: Float, y: Float, modifier: Modifier = Modifier, trace: List<Offset> = emptyList(), deadzone: Float = 0f) {
    val primary = MaterialTheme.colorScheme.primary
    val line = MaterialTheme.colorScheme.outline
    Canvas(modifier.aspectRatio(1f)) {
        val r = size.minDimension / 2f
        val c = center
        drawCircle(line, r - 2f, c, style = Stroke(2.dp.toPx()))
        drawLine(line.copy(alpha = .35f), Offset(c.x - r, c.y), Offset(c.x + r, c.y))
        drawLine(line.copy(alpha = .35f), Offset(c.x, c.y - r), Offset(c.x, c.y + r))
        if (deadzone > 0f) drawCircle(
            line.copy(alpha = .7f), r * deadzone, c,
            style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)))
        )
        trace.forEach { drawCircle(primary.copy(alpha = .35f), 2.dp.toPx(), Offset(c.x + it.x * r, c.y + it.y * r)) }
        drawCircle(primary.copy(alpha = .25f), 16.dp.toPx(), Offset(c.x + x * r, c.y + y * r))
        drawCircle(primary, 9.dp.toPx(), Offset(c.x + x * r, c.y + y * r))
    }
}

@Composable
fun SwitchRow(title: String, subtitle: String? = null, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            if (subtitle != null) Hint(subtitle)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
