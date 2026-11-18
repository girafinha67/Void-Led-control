// by: H3NRI_013/girafinha67
@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.led.controle.ui

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.led.controle.ControllerModel
import com.led.controle.GamepadHub
import com.led.controle.R
import com.led.controle.WATERMARK
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min

private data class Entry(val route: String, val title: String, val desc: String, val icon: ImageVector, val rotate: Float = 0f)

private val main = listOf(
    Entry("android", "Modo Android", "Teste botões, gatilhos, analógicos e D-pad usando os nomes de botões do próprio Android.", Icons.Filled.SportsEsports),
    Entry("ps1", "Modo PS (Tipo 1)", "Teste um controle PlayStation, com seus símbolos e os botões Share, Options e PS.", Icons.Filled.SportsEsports),
    Entry("ps2", "Modo PS (Tipo 2)", "Teste um controle PlayStation que o Android lê errado, quando o Tipo 1 acende o botão trocado.", Icons.Filled.SportsEsports),
    Entry("universal", "Modo Universal", "Teste qualquer controle, mesmo os não reconhecidos, com botões numerados e leituras ao vivo.", Icons.Filled.DragIndicator, 90f),
    Entry("vibration", "Vibração", "Teste os dois motores de vibração com força ajustável, predefinições e uma varredura para isolar cada um.", Icons.Filled.GraphicEq),
    Entry("led", "LED", "Controle a barra de luz com roda de cores, efeitos e teste de canais RGB mortos.", Icons.Filled.Lightbulb)
)

private val extras = listOf(
    Entry("diag", "Diagnóstico completo", "Assistente guiado que confere cada botão, analógico, gatilho, vibração e LED e gera um relatório.", Icons.Filled.FactCheck),
    Entry("sticks", "Analógicos", "Círculo de movimento, zona morta e medição de drift (analógico “andando” sozinho).", Icons.Filled.ControlCamera),
    Entry("triggers", "Gatilhos", "Curso, valor mínimo/máximo e se o gatilho chega a 100%.", Icons.Filled.LinearScale),
    Entry("latency", "Latência e polling", "Taxa de atualização (Hz), jitter e teste de tempo de reação.", Icons.Filled.Speed),
    Entry("stress", "Estresse de botões", "Contador por botão, cliques por segundo e teste de vários botões ao mesmo tempo.", Icons.Filled.TouchApp),
    Entry("sensors", "Sensores", "Giroscópio e acelerômetro do controle, se ele expuser ao Android.", Icons.Filled.Sensors),
    Entry("info", "Info do controle", "Nome, IDs, bateria, luzes, motores, eixos e botões suportados.", Icons.Filled.Memory)
)

private val allEntries = main + extras

private val CardW = 160.dp

@Composable
fun HomeScreen(nav: (String) -> Unit) {
    val n = allEntries.size
    val total = n * 1000                 // "infinito" na prática, sem estourar limites do Int
    val start = n * 500
    val pager = rememberPagerState(initialPage = start) { total }
    var playing by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    // Rolagem automática do carrossel (pausa pelo botão do player).
    // O toque do usuário cancela a animação: isso é normal e não pode encerrar o loop.
    LaunchedEffect(playing) {
        if (playing) while (true) {
            delay(3800)
            if (!pager.isScrollInProgress) {
                try {
                    pager.animateScrollToPage(pager.currentPage + 1)
                } catch (e: CancellationException) {
                    currentCoroutineContext().ensureActive()
                }
            }
        }
    }

    // Se chegar perto das pontas, volta silenciosamente para o meio (mesma função, sem pulo visível).
    LaunchedEffect(pager) {
        snapshotFlow { if (pager.isScrollInProgress) -1 else pager.currentPage }.collect { p ->
            if (p >= 0 && (p < n * 50 || p > total - n * 50)) pager.scrollToPage(start + p % n)
        }
    }

    val sel = GamepadHub.selected
    val model = ControllerModel.detect(sel)
    val current = pager.currentPage % n
    val screenW = LocalConfiguration.current.screenWidthDp

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Hero(model, nav)
        PlayerBar(
            index = current, total = n, playing = playing,
            onPrev = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } },
            onToggle = { playing = !playing },
            onNext = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
            modifier = Modifier.padding(horizontal = 16.dp).appear(2, 20.dp)
        )
        HorizontalPager(
            state = pager,
            modifier = Modifier.fillMaxWidth().height(196.dp).appear(3, 40.dp),
            contentPadding = PaddingValues(horizontal = ((screenW - CardW.value) / 2f).coerceAtLeast(0f).dp),
            pageSize = PageSize.Fixed(CardW),
            pageSpacing = 12.dp,
            verticalAlignment = Alignment.CenterVertically
        ) { page ->
            val idx = page % n
            val e = allEntries[idx]
            FunctionCard(
                e, idx + 1,
                Modifier.height(188.dp).graphicsLayer {
                    // lido na fase de desenho: não recompõe a cada quadro da rolagem
                    val off = abs((pager.currentPage - page) + pager.currentPageOffsetFraction)
                    val k = min(off, 1f)
                    val sc = 1f - 0.16f * k
                    scaleX = sc; scaleY = sc; alpha = 1f - 0.45f * k
                }
            ) {
                val off = abs((pager.currentPage - page) + pager.currentPageOffsetFraction)
                if (off < 0.5f) {
                    nav(e.route)
                } else {
                    scope.launch { pager.animateScrollToPage(page) }
                }
            }
        }
        StatusCircles(sel, nav)
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            WidePill("Diagnóstico completo", Modifier.weight(1f).appear(6, 24.dp)) { nav("diag") }
            WidePill("Configurações", Modifier.weight(1f).appear(7, 24.dp)) { nav("settings") }
        }
        Text(
            WATERMARK, Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 14.dp),
            textAlign = TextAlign.Center, fontFamily = Condensed, fontSize = 11.sp, letterSpacing = 1.sp,
            color = Color.White.copy(alpha = 0.35f)
        )
    }
}

// ======================= Hero (foto do controle) =======================

@Composable
private fun Hero(model: ControllerModel, nav: (String) -> Unit) {
    val frameH = 340.dp
    val badge = 76.dp
    val inf = rememberInfiniteTransition(label = "hero")
    val floatT by inf.animateFloat(-1f, 1f, infiniteRepeatable(tween(2800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "float")
    val glow by inf.animateFloat(0.18f, 0.34f, infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "glow")
    val spin by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(16000, easing = LinearEasing)), label = "spin")
    val ping by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "ping")
    val anyPressed by remember { derivedStateOf { GamepadHub.pressed.values.any { it } } }
    val pop by animateFloatAsState(if (anyPressed) 1.06f else 1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium), label = "pop")

    Box(Modifier.fillMaxWidth().height(frameH + 40.dp).padding(horizontal = 12.dp).appear(0, 28.dp)) {
        // Moldura com recorte em "U" embaixo e fundo de montanhas P&B (desenhada uma vez e guardada em cache)
        Box(
            Modifier.fillMaxWidth().height(frameH).drawWithCache {
                val w = size.width
                val h = size.height
                val side = h - 28.dp.toPx()
                val r = 30.dp.toPx()
                val cx = w / 2f
                val path = Path().apply {
                    moveTo(0f, r); quadraticBezierTo(0f, 0f, r, 0f)
                    lineTo(w - r, 0f); quadraticBezierTo(w, 0f, w, r)
                    lineTo(w, side - r); quadraticBezierTo(w, side, w - r, side)
                    lineTo(cx + 104.dp.toPx(), side)
                    cubicTo(cx + 74.dp.toPx(), side, cx + 66.dp.toPx(), h, cx + 44.dp.toPx(), h)
                    lineTo(cx - 44.dp.toPx(), h)
                    cubicTo(cx - 66.dp.toPx(), h, cx - 74.dp.toPx(), side, cx - 104.dp.toPx(), side)
                    lineTo(r, side); quadraticBezierTo(0f, side, 0f, side - r)
                    close()
                }
                val far = Path().apply {
                    moveTo(0f, side * .62f); lineTo(w * .18f, side * .40f); lineTo(w * .30f, side * .52f)
                    lineTo(w * .48f, side * .28f); lineTo(w * .64f, side * .50f); lineTo(w * .80f, side * .36f)
                    lineTo(w, side * .58f); lineTo(w, h); lineTo(0f, h); close()
                }
                val near = Path().apply {
                    moveTo(0f, side * .80f); lineTo(w * .22f, side * .62f); lineTo(w * .40f, side * .76f)
                    lineTo(w * .58f, side * .60f); lineTo(w * .78f, side * .78f); lineTo(w, side * .66f)
                    lineTo(w, h); lineTo(0f, h); close()
                }
                val sky = Brush.verticalGradient(listOf(Color(0xFFA9A9A9), Color(0xFF4C4C4C)), 0f, side)
                val shade = Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000)), side * .45f, h)
                val border = Stroke(1.5.dp.toPx())
                onDrawBehind {
                    clipPath(path) {
                        drawRect(sky)
                        drawPath(far, Color(0xFF7D7D7D))
                        drawPath(near, Color(0xFF383838))
                        drawRect(shade)
                    }
                    drawPath(path, Color.White.copy(alpha = 0.22f), style = border)
                }
            }
        )
        // Brilho atrás do controle: só o alpha da camada anima (GPU), sem redesenhar a moldura
        Box(
            Modifier.fillMaxWidth().height(frameH)
                .graphicsLayer { alpha = glow / 0.34f }
                .drawBehind {
                    drawRect(
                        Brush.radialGradient(
                            listOf(Color.White.copy(alpha = 0.34f), Color.Transparent),
                            center = Offset(size.width / 2f, (size.height - 28.dp.toPx()) * .40f),
                            radius = size.width * .48f
                        )
                    )
                }
        )

        // Pílulas do topo: marca e atalhos
        Row(
            Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                Modifier.height(34.dp).clip(CircleShape).background(Color(0x66000000)).border(1.dp, Hairline, CircleShape)
                    .padding(start = 5.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(painterResource(R.drawable.app_logo), null, Modifier.size(24.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                Spacer(Modifier.width(7.dp))
                Text("CONTROLER TESTER", fontFamily = Condensed, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 0.8.sp)
            }
            Row(
                Modifier.height(34.dp).clip(CircleShape).background(Color(0x66000000)).border(1.dp, Hairline, CircleShape)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TinyIcon(Icons.Filled.MenuBook, "Guia") { nav("guide") }
                TinyIcon(Icons.Outlined.Info, "Sobre") { nav("about") }
                TinyIcon(Icons.Filled.Settings, "Configurações") { nav("settings") }
            }
        }

        // Foto do controle conectado (troca animada, flutua e reage aos botões)
        Box(
            Modifier.align(Alignment.TopCenter).padding(top = 58.dp).fillMaxWidth(0.80f).height(176.dp)
                .graphicsLayer {
                    translationY = floatT * 7.dp.toPx()
                    rotationZ = floatT * 0.9f
                    scaleX = pop; scaleY = pop
                },
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = model,
                transitionSpec = {
                    (fadeIn(tween(520, 120)) + scaleIn(initialScale = 0.75f, animationSpec = tween(520, 120, FastOutSlowInEasing)) +
                        slideInVertically(tween(520, 120, FastOutSlowInEasing)) { it / 8 }) togetherWith
                        (fadeOut(tween(220)) + scaleOut(targetScale = 1.15f, animationSpec = tween(220)))
                },
                label = "controllerPhoto"
            ) { m -> ControllerPhoto(m, Modifier.fillMaxSize()) }
        }

        // Nome do modelo, no lugar do "ABOUT"
        Column(
            Modifier.align(Alignment.TopCenter).padding(top = 238.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedContent(
                targetState = model.label,
                transitionSpec = { (fadeIn(tween(380)) + slideInVertically(tween(380)) { it / 3 }) togetherWith fadeOut(tween(160)) },
                label = "modelLabel"
            ) { label ->
                Text(
                    label.uppercase() + ";", fontFamily = Condensed, fontWeight = FontWeight.Bold,
                    fontSize = 30.sp, lineHeight = 32.sp, maxLines = 1
                )
            }
            Swoosh(Modifier.width(130.dp))
        }

        // Selo central (logo) com anel girando e pulso
        Box(Modifier.align(Alignment.BottomCenter).size(badge), contentAlignment = Alignment.Center) {
            Canvas(
                Modifier.fillMaxSize().graphicsLayer {
                    val k = 0.82f + 0.18f * ping
                    scaleX = k; scaleY = k; alpha = 0.30f * (1f - ping)
                }
            ) { drawCircle(Color.White, size.minDimension / 2f - 1.dp.toPx(), style = Stroke(2.dp.toPx())) }
            Canvas(Modifier.fillMaxSize()) { drawCircle(Color(0xFF0A0A0A), size.minDimension / 2f - 3.dp.toPx()) }
            Canvas(Modifier.fillMaxSize().graphicsLayer { rotationZ = spin }) {
                drawCircle(
                    Color.White.copy(alpha = 0.65f), size.minDimension / 2f - 3.dp.toPx(),
                    style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f)))
                )
            }
            Image(
                painterResource(R.drawable.app_logo), "Controler Tester",
                Modifier.size(badge - 18.dp).clip(CircleShape), contentScale = ContentScale.Crop
            )
        }

        // Pílulas ao lado do selo: busca/seletor de controle e bateria
        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = (badge - 38.dp) / 2),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DevicePill(Modifier.weight(1f))
            Spacer(Modifier.width(badge + 14.dp))
            BatteryPill(Modifier.weight(1f))
        }
    }
}

@Composable
private fun TinyIcon(icon: ImageVector, desc: String, onClick: () -> Unit) {
    Box(Modifier.size(32.dp).bounceClick(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, desc, Modifier.size(19.dp), tint = Color.White)
    }
}

@Composable
private fun DevicePill(modifier: Modifier) {
    val devs = GamepadHub.devices
    val sel = GamepadHub.selected
    var open by remember { mutableStateOf(false) }
    Box(modifier.height(38.dp)) {
        Row(
            Modifier.fillMaxSize()
                .bounceClick {
                    if (devs.size > 1) {
                        open = true
                    } else {
                        GamepadHub.refresh()
                    }
                }
                .clip(CircleShape).background(Color(0xCC0A0A0A)).border(1.2.dp, Color.White.copy(alpha = 0.7f), CircleShape)
                .padding(start = 14.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                sel?.name ?: "procurar controle", Modifier.weight(1f), fontFamily = Condensed, fontWeight = FontWeight.Bold,
                fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Icon(if (devs.size > 1) Icons.Filled.SwapHoriz else Icons.Filled.Search, null, Modifier.size(18.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            devs.forEach { d -> DropdownMenuItem(text = { Text(d.name) }, onClick = { GamepadHub.select(d.id); open = false }) }
        }
    }
}

@Composable
private fun BatteryPill(modifier: Modifier) {
    val d = GamepadHub.selected
    val level by produceState(initialValue = -1f, d) {
        while (true) {
            value = runCatching {
                val b = d?.batteryState
                if (b != null && b.isPresent && !b.capacity.isNaN()) b.capacity else -1f
            }.getOrDefault(-1f)
            delay(4000)
        }
    }
    val frac by animateFloatAsState(if (level < 0f) 0f else level.coerceIn(0f, 1f), tween(700), label = "bat")
    val text = if (level < 0f) "BAT --" else "BAT ${(level * 100).toInt()}%"
    Box(
        modifier.height(38.dp).clip(CircleShape).background(Color(0xCC0A0A0A)).border(1.2.dp, Color.White.copy(alpha = 0.7f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontFamily = Condensed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Box(
            Modifier.matchParentSize().drawWithContent {
                clipRect(left = 0f, top = 0f, right = size.width * frac, bottom = size.height) {
                    this@drawWithContent.drawContent()
                }
            }.background(Color.White),
            contentAlignment = Alignment.Center
        ) { Text(text, color = InkBlack, fontFamily = Condensed, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
    }
}

// ======================= Player (indicador do carrossel) =======================

@Composable
private fun PlayerBar(
    index: Int, total: Int, playing: Boolean,
    onPrev: () -> Unit, onToggle: () -> Unit, onNext: () -> Unit, modifier: Modifier = Modifier
) {
    val frac by animateFloatAsState(if (total <= 1) 0f else index / (total - 1f), tween(450), label = "frac")
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier.fillMaxWidth().height(38.dp).clip(shape).background(Color(0x99262626)).border(1.dp, Hairline, shape)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlayerBtn(Icons.Filled.SkipPrevious, "Anterior", onPrev)
        PlayerBtn(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, if (playing) "Pausar" else "Tocar", onToggle)
        PlayerBtn(Icons.Filled.SkipNext, "Próximo", onNext)
        Text("%02d".format(index + 1), fontSize = 12.sp, fontFamily = Condensed, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp))
        Canvas(Modifier.weight(1f).height(16.dp).padding(horizontal = 10.dp)) {
            val y = size.height / 2f
            drawLine(Color.White.copy(alpha = 0.3f), Offset(0f, y), Offset(size.width, y), 2.dp.toPx())
            drawLine(Color.White, Offset(0f, y), Offset(size.width * frac, y), 2.dp.toPx())
            drawCircle(Color.White, 4.5.dp.toPx(), Offset(size.width * frac, y))
        }
        Text("-%02d".format(total - index - 1), fontSize = 12.sp, fontFamily = Condensed, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 6.dp))
    }
}

@Composable
private fun PlayerBtn(icon: ImageVector, desc: String, onClick: () -> Unit) {
    Box(Modifier.size(32.dp).bounceClick(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, desc, Modifier.size(22.dp), tint = Color.White)
    }
}

// ======================= Cards do carrossel =======================

@Composable
private fun FunctionCard(e: Entry, number: Int, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier.fillMaxWidth().bounceClick(onClick = onClick).clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFF3C3C3C), Color(0xFF101010))))
            .border(1.dp, Hairline, shape)
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawRect(
                Brush.radialGradient(
                    listOf(Color.White.copy(alpha = 0.16f), Color.Transparent),
                    center = Offset(size.width / 2f, size.height * 0.36f), radius = size.width * 0.75f
                )
            )
        }
        Text(
            "%02d".format(number), Modifier.align(Alignment.TopStart).padding(start = 12.dp, top = 10.dp),
            fontFamily = Condensed, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White.copy(alpha = 0.55f)
        )
        Box(
            Modifier.align(Alignment.TopEnd).padding(10.dp).size(26.dp).clip(RoundedCornerShape(8.dp)).background(Color.White),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Filled.NorthEast, null, Modifier.size(16.dp), tint = InkBlack) }
        Icon(
            e.icon, null, Modifier.align(Alignment.TopCenter).padding(top = 46.dp).size(70.dp).rotate(e.rotate),
            tint = Color.White.copy(alpha = 0.93f)
        )
        Column(Modifier.align(Alignment.BottomStart).padding(start = 12.dp, end = 10.dp, bottom = 10.dp)) {
            Text(
                e.title.uppercase(), fontFamily = Condensed, fontWeight = FontWeight.Bold,
                fontSize = 19.sp, lineHeight = 19.sp, maxLines = 2
            )
            if (e.title.length <= 12) Text(
                e.title.uppercase(), fontFamily = Condensed, fontWeight = FontWeight.Bold,
                fontSize = 19.sp, lineHeight = 19.sp, maxLines = 1, color = Color.White.copy(alpha = 0.25f)
            )
        }
    }
}

// ======================= Círculos de status (estilo destaques) =======================

private data class Cap(val label: String, val icon: ImageVector, val on: Boolean, val route: String)

@Composable
private fun StatusCircles(dev: android.view.InputDevice?, nav: (String) -> Unit) {
    val caps = remember(dev) {
        fun safe(block: () -> Boolean) = runCatching(block).getOrDefault(false)
        listOf(
            Cap("Conexão", Icons.Filled.SportsEsports, dev != null, "info"),
            Cap("Vibração", Icons.Filled.Vibration, dev != null && safe { dev.vibratorManager.vibratorIds.isNotEmpty() }, "vibration"),
            Cap("LED", Icons.Filled.Lightbulb, dev != null && safe { dev.lightsManager.lights.any { it.hasRgbControl() } }, "led"),
            Cap("Sensores", Icons.Filled.Sensors, dev != null && safe { dev.sensorManager.getSensorList(android.hardware.Sensor.TYPE_ALL).isNotEmpty() }, "sensors"),
            Cap("Bateria", Icons.Filled.BatteryFull, dev != null && safe { dev.batteryState.isPresent }, "info")
        )
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        caps.forEachIndexed { i, c -> CapCircle(c, i) { nav(c.route) } }
    }
}

@Composable
private fun CapCircle(c: Cap, index: Int, onClick: () -> Unit) {
    val a by animateFloatAsState(if (c.on) 1f else 0.32f, tween(500), label = "cap")
    Column(Modifier.appear(4 + index / 2, 30.dp).bounceClick(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(62.dp).clip(CircleShape).border(1.6.dp, Color.White.copy(alpha = 0.55f * a), CircleShape).padding(5.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier.fillMaxSize().clip(CircleShape).background(Brush.radialGradient(listOf(Color(0xFF3A3A3A), Color(0xFF121212)))),
                contentAlignment = Alignment.Center
            ) { Icon(c.icon, c.label, Modifier.size(24.dp).alpha(a), tint = Color.White) }
        }
        Spacer(Modifier.height(5.dp))
        Text(c.label.uppercase(), fontFamily = Condensed, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 0.8.sp, color = Color.White.copy(alpha = 0.4f + 0.5f * a))
    }
}

@Composable
private fun WidePill(text: String, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier.height(44.dp).bounceClick(onClick = onClick).clip(shape).background(Color(0xCC262626)).border(1.dp, Hairline, shape),
        contentAlignment = Alignment.Center
    ) { Text(text, fontFamily = Condensed, fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 0.3.sp) }
}
