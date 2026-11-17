package com.led.controle.ui

import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.led.controle.GamepadHub
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun amp(v: Float) = (v * 255f).toInt().coerceIn(1, 255)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VibrationScreen(onBack: () -> Unit) {
    val dev = GamepadHub.selected
    val vm: VibratorManager? = remember(dev?.id) { dev?.vibratorManager }
    val ids = remember(vm) { vm?.vibratorIds ?: IntArray(0) }
    var left by remember { mutableFloatStateOf(0.6f) }   // motor grave (esquerdo)
    var right by remember { mutableFloatStateOf(0.6f) }  // motor agudo (direito)
    var running by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    var job by remember { mutableStateOf<Job?>(null) }

    fun stopAll() { job?.cancel(); job = null; running = false; runCatching { vm?.cancel() } }

    /** Aciona os motores em paralelo (esquerdo = id[0], direito = id[1], ou único motor). */
    fun fire(l: Float, r: Float, ms: Long) {
        val m = vm ?: return
        runCatching {
            if (ids.size >= 2) {
                val b = CombinedVibration.startParallel()
                if (l > 0f) b.addVibrator(ids[0], VibrationEffect.createOneShot(ms, amp(l)))
                if (r > 0f) b.addVibrator(ids[1], VibrationEffect.createOneShot(ms, amp(r)))
                if (l <= 0f && r <= 0f) m.cancel() else m.vibrate(b.combine())
            } else if (ids.size == 1) {
                val v = maxOf(l, r)
                if (v <= 0f) m.cancel() else m.vibrate(CombinedVibration.createParallel(VibrationEffect.createOneShot(ms, amp(v))))
            }
        }
    }

    DisposableEffect(vm) { onDispose { runCatching { vm?.cancel() } } }
    LaunchedEffect(left, right, running) { if (running) fire(left, right, 60_000) }

    ToolScaffold("Vibração", onBack) {
        if (dev == null) { Hint("Conecte um controle para testar a vibração."); return@ToolScaffold }
        if (ids.isEmpty()) {
            SectionCard("Sem motores") { Hint("O Android não expõe motores de vibração para este controle. Alguns controles só vibram em apps/jogos com driver próprio.") }
            return@ToolScaffold
        }
        SectionCard("Motores detectados: ${ids.size}") {
            Text("Motor esquerdo (grave): ${(left * 100).toInt()}%")
            Slider(value = left, onValueChange = { left = it }, valueRange = 0f..1f)
            Text("Motor direito (agudo): ${(right * 100).toInt()}%")
            Slider(value = right, onValueChange = { right = it }, valueRange = 0f..1f, enabled = ids.size >= 2)
            Button(onClick = { if (running) stopAll() else running = true }, Modifier.fillMaxWidth()) {
                Text(if (running) "Parar" else "Vibrar continuamente")
            }
        }
        SectionCard("Predefinições") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { stopAll(); fire(0.3f, 0.3f, 400) }) { Text("Fraca") }
                FilledTonalButton(onClick = { stopAll(); fire(0.6f, 0.6f, 400) }) { Text("Média") }
                FilledTonalButton(onClick = { stopAll(); fire(1f, 1f, 400) }) { Text("Forte") }
                FilledTonalButton(onClick = {
                    stopAll(); job = scope.launch { repeat(4) { fire(0.8f, 0.8f, 120); delay(260) } }
                }) { Text("Pulsos") }
                FilledTonalButton(onClick = {
                    stopAll(); job = scope.launch {
                        repeat(3) { fire(1f, 0.4f, 90); delay(160); fire(0.6f, 0.2f, 120); delay(520) }
                    }
                }) { Text("Batimento") }
            }
        }
        SectionCard("Varredura (isola cada motor)") {
            Hint("Roda: só esquerdo → só direito → ambos, subindo a força. Serve para achar um motor fraco ou morto.")
            Button(onClick = {
                stopAll(); running = false
                job = scope.launch {
                    status = "Motor esquerdo"; for (i in 1..10) { fire(i / 10f, 0f, 300); delay(300) }
                    status = "Motor direito"; for (i in 1..10) { fire(0f, i / 10f, 300); delay(300) }
                    status = "Ambos"; for (i in 1..10) { fire(i / 10f, i / 10f, 300); delay(300) }
                    status = "Concluído"; runCatching { vm?.cancel() }
                }
            }, Modifier.fillMaxWidth()) { Text("Iniciar varredura") }
            if (status.isNotEmpty()) Text("Etapa: $status")
            OutlinedButton(onClick = { stopAll(); status = "" }, Modifier.fillMaxWidth()) { Text("Parar tudo") }
        }
    }
}
