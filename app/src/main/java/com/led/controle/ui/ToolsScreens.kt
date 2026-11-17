package com.led.controle.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.led.controle.GamepadHub
import com.led.controle.Prefs
import kotlinx.coroutines.delay
import kotlin.math.hypot
import kotlin.math.sqrt

// ------------------------------------------------------------------ Analógicos
@Composable
fun SticksScreen(onBack: () -> Unit) {
    val trailL = remember { mutableStateListOf<Offset>() }
    val trailR = remember { mutableStateListOf<Offset>() }
    var maxL by remember { mutableFloatStateOf(0f) }
    var maxR by remember { mutableFloatStateOf(0f) }
    var driftL by remember { mutableStateOf<Float?>(null) }
    var driftR by remember { mutableStateOf<Float?>(null) }
    var measuring by remember { mutableIntStateOf(0) } // segundos restantes
    var driftRun by remember { mutableIntStateOf(0) }

    val (lx, ly) = GamepadHub.leftStick()
    val (rx, ry) = GamepadHub.rightStick()

    LaunchedEffect(lx, ly, rx, ry) {
        if (hypot(lx, ly) > 0.05f) { trailL.add(Offset(lx, ly)); if (trailL.size > 400) trailL.removeAt(0) }
        if (hypot(rx, ry) > 0.05f) { trailR.add(Offset(rx, ry)); if (trailR.size > 400) trailR.removeAt(0) }
        maxL = maxOf(maxL, hypot(lx, ly)); maxR = maxOf(maxR, hypot(rx, ry))
    }

    fun verdict(d: Float?) = when {
        d == null -> "—"
        d < 0.04f -> "OK (sem drift)"
        d < 0.10f -> "Drift leve"
        else -> "Drift forte"
    }
    fun verdictColor(d: Float?) = when {
        d == null -> Color.Gray; d < 0.04f -> OkGreen; d < 0.10f -> WarnYellow; else -> BadRed
    }

    // Medição de drift: 5 s com os analógicos soltos.
    LaunchedEffect(driftRun) {
        if (driftRun > 0) {
            var mL = 0f; var mR = 0f
            for (i in 5 downTo 1) {
                measuring = i
                repeat(10) {
                    val (a, b) = GamepadHub.leftStick(); val (c, d) = GamepadHub.rightStick()
                    mL = maxOf(mL, hypot(a, b)); mR = maxOf(mR, hypot(c, d)); delay(100)
                }
            }
            driftL = mL; driftR = mR; measuring = 0
        }
    }

    ToolScaffold("Analógicos", onBack, actions = {
        TextButton(onClick = { trailL.clear(); trailR.clear(); maxL = 0f; maxR = 0f }) { Text("Limpar") }
    }) {
        SectionCard("Círculo de movimento") {
            Hint("Gire cada analógico em todo o contorno. O rastro deve formar um círculo uniforme; o círculo tracejado é a zona morta (${(Prefs.deadzone * 100).toInt()}%).")
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1f)) {
                    StickView(lx, ly, trace = trailL, deadzone = Prefs.deadzone)
                    Text("Esq: %.2f, %.2f".format(lx, ly), fontSize = 12.sp)
                    Text("Alcance máx: %.0f%%".format(maxL * 100), fontSize = 12.sp)
                }
                Column(Modifier.weight(1f)) {
                    StickView(rx, ry, trace = trailR, deadzone = Prefs.deadzone)
                    Text("Dir: %.2f, %.2f".format(rx, ry), fontSize = 12.sp)
                    Text("Alcance máx: %.0f%%".format(maxR * 100), fontSize = 12.sp)
                }
            }
        }
        SectionCard("Teste de drift") {
            Hint("Solte os dois analógicos e toque em iniciar. O app mede o maior desvio do centro durante 5 segundos.")
            Button(onClick = { driftL = null; driftR = null; measuring = 5; driftRun++ }, enabled = measuring == 0, modifier = Modifier.fillMaxWidth()) {
                Text(if (measuring == 0) "Iniciar (5 s)" else "Medindo… $measuring")
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Esquerdo"); Text(verdict(driftL), color = verdictColor(driftL), fontWeight = FontWeight.Bold)
                    driftL?.let { Text("desvio %.3f".format(it), fontSize = 12.sp) }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Direito"); Text(verdict(driftR), color = verdictColor(driftR), fontWeight = FontWeight.Bold)
                    driftR?.let { Text("desvio %.3f".format(it), fontSize = 12.sp) }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ Gatilhos
@Composable
fun TriggersScreen(onBack: () -> Unit) {
    var minL by remember { mutableFloatStateOf(1f) }; var maxL by remember { mutableFloatStateOf(0f) }
    var minR by remember { mutableFloatStateOf(1f) }; var maxR by remember { mutableFloatStateOf(0f) }
    val l = GamepadHub.triggerStd(true).coerceAtLeast(GamepadHub.triggerLegacy(true))
    val r = GamepadHub.triggerStd(false).coerceAtLeast(GamepadHub.triggerLegacy(false))
    LaunchedEffect(l, r) { minL = minOf(minL, l); maxL = maxOf(maxL, l); minR = minOf(minR, r); maxR = maxOf(maxR, r) }

    ToolScaffold("Gatilhos", onBack, actions = {
        TextButton(onClick = { minL = 1f; maxL = 0f; minR = 1f; maxR = 0f }) { Text("Zerar") }
    }) {
        @Composable
        fun block(name: String, v: Float, mn: Float, mx: Float) = SectionCard(name) {
            BarMeter("Valor atual", v, valueText = "%.0f%%".format(v * 100))
            Text("Mínimo visto: %.0f%%   ·   Máximo visto: %.0f%%".format(if (mn > mx) 0f else mn * 100, mx * 100), fontSize = 13.sp)
            val ok = mx >= 0.98f
            Text(
                if (mx == 0f) "Aperte o gatilho até o fundo" else if (ok) "Chega a 100% ✔" else "Não chegou a 100% (curso incompleto?)",
                color = if (mx == 0f) Color.Gray else if (ok) OkGreen else WarnYellow, fontWeight = FontWeight.Medium
            )
        }
        block("L2", l, minL, maxL)
        block("R2", r, minR, maxR)
        Hint("Pressione devagar e solte devagar: o valor deve subir e descer de forma suave, sem saltos.")
    }
}

// ------------------------------------------------------------------ Latência e polling
@Composable
fun LatencyScreen(onBack: () -> Unit) {
    var hz by remember { mutableIntStateOf(0) }
    var jitter by remember { mutableFloatStateOf(0f) }
    var avgGap by remember { mutableFloatStateOf(0f) }

    // Atualiza a cada segundo.
    LaunchedEffect(Unit) {
        var last = GamepadHub.samples
        while (true) {
            delay(1000)
            val now = GamepadHub.samples
            hz = (now - last).toInt(); last = now
            val n = minOf(GamepadHub.gapIndex, GamepadHub.gaps.size)
            if (n > 5) {
                val xs = GamepadHub.gaps.take(n)
                val mean = xs.average().toFloat()
                avgGap = mean
                jitter = sqrt(xs.map { (it - mean) * (it - mean) }.average()).toFloat()
            }
        }
    }

    // Teste de reação
    var phase by remember { mutableIntStateOf(0) } // 0 parado, 1 aguardando, 2 pronto
    var t0 by remember { mutableLongStateOf(0L) }
    val results = remember { mutableStateListOf<Int>() }
    var early by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val l: (Int, Boolean, Long) -> Unit = { _, down, t ->
            if (down) when (phase) {
                1 -> { early = true; phase = 0 }
                2 -> { results.add(((t - t0) / 1_000_000L).toInt()); phase = 0; early = false }
            }
        }
        GamepadHub.keyListeners.add(l)
        onDispose { GamepadHub.keyListeners.remove(l) }
    }
    LaunchedEffect(phase) {
        if (phase == 1) { delay((1500..4000).random().toLong()); if (phase == 1) { t0 = System.nanoTime(); phase = 2 } }
    }

    ToolScaffold("Latência e polling", onBack, actions = {
        TextButton(onClick = { GamepadHub.clearCounters(); results.clear() }) { Text("Zerar") }
    }) {
        SectionCard("Taxa de atualização") {
            Text("$hz Hz", fontSize = 40.sp, fontWeight = FontWeight.Bold)
            Hint("Mexa os analógicos continuamente durante a leitura. Em Bluetooth, valores típicos ficam entre 60 e 250 Hz; USB pode passar de 500 Hz. O Android pode agrupar eventos, então use como estimativa.")
            Text("Intervalo médio entre pacotes: %.2f ms".format(avgGap))
            Text("Jitter (desvio): %.2f ms".format(jitter), color = if (jitter < 2f) OkGreen else if (jitter < 6f) WarnYellow else BadRed)
        }
        SectionCard("Tempo de reação") {
            Box(
                Modifier.fillMaxWidth().height(120.dp).background(
                    when (phase) { 2 -> Color(0xFF2E7D32); 1 -> Color(0xFF8E2A2A); else -> MaterialTheme.colorScheme.surfaceContainerHigh },
                    RoundedCornerShape(20.dp)
                ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    when (phase) { 2 -> "AGORA! Aperte um botão"; 1 -> "Espere ficar verde…"; else ->
                        if (early) "Muito cedo! Tente de novo" else if (results.isNotEmpty()) "${results.last()} ms" else "Toque em iniciar" },
                    fontSize = 20.sp, fontWeight = FontWeight.Bold
                )
            }
            Button(onClick = { early = false; phase = 1 }, enabled = phase == 0, modifier = Modifier.fillMaxWidth()) { Text("Iniciar rodada") }
            if (results.isNotEmpty()) {
                Text("Média: ${results.average().toInt()} ms · Melhor: ${results.min()} ms · Rodadas: ${results.size}")
                Hint("Inclui seu tempo de reação humano (≈200 ms). Compare entre USB e Bluetooth com a mesma pessoa para ver a diferença de latência.")
            }
        }
    }
}

// ------------------------------------------------------------------ Estresse
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StressScreen(onBack: () -> Unit) {
    var cps by remember { mutableIntStateOf(0) }
    var peak by remember { mutableIntStateOf(0) }
    var challenge by remember { mutableIntStateOf(0) }  // segundos restantes (0 = parado)
    var challengeTotal by remember { mutableIntStateOf(-1) }
    var challengeRun by remember { mutableIntStateOf(0) }
    val total = GamepadHub.pressCount.values.sum()

    LaunchedEffect(Unit) {
        var last = GamepadHub.pressCount.values.sum()
        while (true) {
            delay(1000)
            val now = GamepadHub.pressCount.values.sum()
            cps = now - last; last = now; peak = maxOf(peak, cps)
        }
    }
    LaunchedEffect(challengeRun) {
        if (challengeRun > 0) {
            val start = GamepadHub.pressCount.values.sum()
            for (i in 10 downTo 1) { challenge = i; delay(1000) }
            challengeTotal = GamepadHub.pressCount.values.sum() - start; challenge = 0
        }
    }

    ToolScaffold("Estresse de botões", onBack, actions = {
        TextButton(onClick = { GamepadHub.clearCounters(); peak = 0; challengeTotal = -1 }) { Text("Zerar") }
    }) {
        SectionCard("Contagem") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column { Text("Total", fontSize = 12.sp); Text("$total", fontSize = 28.sp, fontWeight = FontWeight.Bold) }
                Column { Text("Cliques/s", fontSize = 12.sp); Text("$cps", fontSize = 28.sp, fontWeight = FontWeight.Bold) }
                Column { Text("Pico", fontSize = 12.sp); Text("$peak", fontSize = 28.sp, fontWeight = FontWeight.Bold) }
                Column { Text("Simultâneos", fontSize = 12.sp); Text("${GamepadHub.maxSimultaneous}", fontSize = 28.sp, fontWeight = FontWeight.Bold) }
            }
            Hint("“Simultâneos” é o maior número de botões segurados ao mesmo tempo: útil para detectar ghosting (botões que somem quando muitos são apertados).")
        }
        SectionCard("Desafio de 10 segundos") {
            Button(onClick = { challengeTotal = -1; challenge = 10; challengeRun++ }, enabled = challenge == 0, modifier = Modifier.fillMaxWidth()) {
                Text(if (challenge == 0) "Iniciar: aperte o máximo possível" else "Restam $challenge s…")
            }
            if (challengeTotal >= 0) Text("Resultado: $challengeTotal apertos (${"%.1f".format(challengeTotal / 10f)}/s)", fontWeight = FontWeight.Bold)
        }
        SectionCard("Por botão") {
            val list = GamepadHub.pressCount.entries.sortedByDescending { it.value }
            if (list.isEmpty()) Hint("Aperte botões para ver o contador de cada um.")
            list.forEach { (k, n) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(GamepadHub.keyName(k) + if (GamepadHub.key(k)) "  ●" else "")
                    Text("$n", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
