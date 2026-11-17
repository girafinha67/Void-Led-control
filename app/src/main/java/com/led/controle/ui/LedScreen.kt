package com.led.controle.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color as AColor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.led.controle.*
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LedScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val dev = GamepadHub.selected
    val runner = remember { LedRunner(scope) }
    var cfg by remember { mutableStateOf(Cfg.led) }
    var service by remember { mutableStateOf(Cfg.service) }
    var boot by remember { mutableStateOf(Cfg.boot) }
    var player by remember { mutableIntStateOf(0) }
    var hsv by remember {
        mutableStateOf(FloatArray(3).also { AColor.colorToHSV(Cfg.led.color, it); if (it[1] == 0f) it[1] = 0f })
    }

    // Quando o serviço está ativo, ele é o dono da sessão de LED; senão, a tela abre a sua.
    DisposableEffect(dev?.id, service) {
        if (!service) runner.attach(dev)
        onDispose { if (!service) runner.stop() }
    }
    fun apply(c: LedCfg) {
        cfg = c; Cfg.led = c
        if (service) LedService.start(ctx) else runner.apply(c)
    }
    LaunchedEffect(dev?.id, service) { if (!service && dev != null) runner.apply(cfg) }

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        Cfg.service = true; service = true; LedService.start(ctx)
    }

    ToolScaffold("LED", onBack) {
        if (dev == null) { Hint("Conecte um controle para controlar a luz."); return@ToolScaffold }
        val lights = runner.lights.ifEmpty { runCatching { dev.lightsManager.lights }.getOrDefault(emptyList()) }
        val rgb = lights.any { it.hasRgbControl() }
        if (!rgb) {
            SectionCard("LED RGB indisponível") {
                Hint("O Android não expõe luz RGB para este controle (${lights.size} luz(es) encontrada(s)). " +
                    "No Android 13+ o DualShock 4 e o DualSense normalmente expõem; controles genéricos geralmente não.")
            }
        }

        SectionCard("Cor") {
            ColorWheel(hsv) { h, s ->
                hsv = floatArrayOf(h, s, 1f)
                apply(cfg.copy(color = AColor.HSVToColor(floatArrayOf(h, s, 1f)), effect = if (cfg.effect == Effect.RAINBOW || cfg.effect == Effect.RGB_TEST) Effect.FIXED else cfg.effect))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(36.dp).clip(CircleShape).background(Color(cfg.color)))
                Text("#%06X".format(cfg.color and 0xFFFFFF), fontSize = 16.sp)
            }
            val swatches = listOf(
                "Vermelho" to AColor.RED, "Laranja" to 0xFFFF8800.toInt(), "Amarelo" to AColor.YELLOW,
                "Verde" to AColor.GREEN, "Ciano" to AColor.CYAN, "Azul" to AColor.BLUE,
                "Roxo" to 0xFF9C27B0.toInt(), "Rosa" to AColor.MAGENTA, "Branco" to AColor.WHITE
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                swatches.forEach { (n, c) ->
                    Box(
                        Modifier.clip(RoundedCornerShape(12.dp)).background(Color(c)).clickable {
                            AColor.colorToHSV(c, hsv); apply(cfg.copy(color = c, effect = if (cfg.effect == Effect.RAINBOW || cfg.effect == Effect.RGB_TEST) Effect.FIXED else cfg.effect))
                        }.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) { Text(n, color = Color.Black, fontSize = 12.sp) }
                }
                OutlinedButton(onClick = { apply(cfg.copy(color = AColor.BLACK, effect = Effect.FIXED)) }) { Text("Apagar") }
            }
            Text("Brilho: ${(cfg.brightness * 100).toInt()}%")
            Slider(value = cfg.brightness, onValueChange = { apply(cfg.copy(brightness = it)) }, valueRange = 0.02f..1f)
        }

        SectionCard("Efeitos") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Effect.values().forEach { e ->
                    FilterChip(selected = cfg.effect == e, onClick = { apply(cfg.copy(effect = e)) }, label = { Text(e.label) })
                }
            }
            if (cfg.effect != Effect.FIXED) {
                Text("Velocidade: ${(cfg.speed * 100).toInt()}%")
                Slider(value = cfg.speed, onValueChange = { apply(cfg.copy(speed = it)) })
            }
            if (cfg.effect == Effect.RGB_TEST) Hint("Mostra vermelho, verde, azul, branco e apagado em sequência. Se algum canal não acende ou a cor sai errada, o LED tem um canal queimado.")
        }

        if (lights.any { it.type == android.hardware.lights.Light.LIGHT_TYPE_PLAYER_ID }) {
            SectionCard("LEDs de jogador") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (0..4).forEach { p ->
                        FilterChip(selected = player == p, onClick = { player = p; runner.setPlayerId(p) },
                            label = { Text(if (p == 0) "Nenhum" else "Jogador $p") })
                    }
                }
            }
        }

        SectionCard("Segundo plano") {
            SwitchRow("Manter LED com o app fechado", "Usa um serviço com notificação. Toque em “Parar” na notificação para devolver o LED ao normal.", service) { on ->
                if (on) {
                    if (ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                        Cfg.service = true; service = true; LedService.start(ctx)
                    } else notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else { service = false; LedService.stop(ctx) }
            }
            SwitchRow("Iniciar com o sistema", "Aplica a cor ao ligar o celular (precisa do serviço ativo).", boot) { boot = it; Cfg.boot = it }
        }

        SectionCard("Luzes encontradas (${lights.size})") {
            lights.forEach {
                Hint("• ${it.name} — tipo ${it.type}${if (it.hasRgbControl()) " · RGB" else ""}${if (it.hasBrightnessControl()) " · brilho" else ""}")
            }
        }
    }
}

@Composable
private fun ColorWheel(hsv: FloatArray, onPick: (hue: Float, sat: Float) -> Unit) {
    val sweep = remember {
        Brush.sweepGradient(List(7) { Color(AColor.HSVToColor(floatArrayOf(it * 60f, 1f, 1f))) })
    }
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Canvas(
            Modifier.size(240.dp)
                .pointerInput(Unit) {
                    detectTapGestures { pos -> pick(pos, size.width.toFloat(), onPick) }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ -> pick(change.position, size.width.toFloat(), onPick) }
                }
        ) {
            val r = size.minDimension / 2f
            drawCircle(sweep, r)
            drawCircle(Brush.radialGradient(listOf(Color.White, Color.Transparent), center = center, radius = r), r)
            val ang = Math.toRadians(hsv[0].toDouble())
            val d = hsv[1] * r
            val p = Offset(center.x + (kotlin.math.cos(ang) * d).toFloat(), center.y + (kotlin.math.sin(ang) * d).toFloat())
            drawCircle(Color.Black, 12.dp.toPx(), p)
            drawCircle(Color.White, 9.dp.toPx(), p)
        }
    }
}

private fun pick(pos: Offset, width: Float, onPick: (Float, Float) -> Unit) {
    val c = width / 2f
    val dx = pos.x - c; val dy = pos.y - c
    var deg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
    if (deg < 0) deg += 360f
    onPick(deg, min(hypot(dx, dy) / c, 1f))
}
