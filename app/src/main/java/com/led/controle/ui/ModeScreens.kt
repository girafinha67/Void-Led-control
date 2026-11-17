package com.led.controle.ui

import android.view.KeyEvent
import android.view.MotionEvent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.led.controle.GamepadHub
import com.led.controle.Prefs

// ------------------------------------------------------------------ Modo Android
private val androidButtons = listOf(
    96, 97, 99, 100, 102, 103, 104, 105, 106, 107, 109, 108, 110,
    KeyEvent.KEYCODE_BUTTON_C, KeyEvent.KEYCODE_BUTTON_Z
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AndroidModeScreen(onBack: () -> Unit) {
    ToolScaffold("Modo Android", onBack) {
        SectionCard("Botões") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                androidButtons.forEach { StatusChip(GamepadHub.keyName(it), GamepadHub.key(it)) }
            }
        }
        SectionCard("D-pad") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusChip("DPAD_UP", GamepadHub.dpadUp())
                StatusChip("DPAD_DOWN", GamepadHub.dpadDown())
                StatusChip("DPAD_LEFT", GamepadHub.dpadLeft())
                StatusChip("DPAD_RIGHT", GamepadHub.dpadRight())
            }
            Hint("Funciona tanto com teclas DPAD quanto com os eixos HAT_X / HAT_Y.")
        }
        SectionCard("Analógicos") {
            val (lx, ly) = GamepadHub.leftStick()
            val (rx, ry) = GamepadHub.rightStick()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1f)) {
                    StickView(lx, ly, deadzone = Prefs.deadzone)
                    Text("Esquerdo  X %.2f  Y %.2f".format(lx, ly), fontSize = 12.sp)
                }
                Column(Modifier.weight(1f)) {
                    StickView(rx, ry, deadzone = Prefs.deadzone)
                    Text("Direito  X %.2f  Y %.2f".format(rx, ry), fontSize = 12.sp)
                }
            }
        }
        SectionCard("Gatilhos") {
            BarMeter("L2 (LTRIGGER / BRAKE)", GamepadHub.triggerStd(true))
            BarMeter("R2 (RTRIGGER / GAS)", GamepadHub.triggerStd(false))
        }
    }
}

// ------------------------------------------------------------------ Modo PS
private val PsBlue = Color(0xFF6BA8FF)
private val PsPink = Color(0xFFFF8AD0)
private val PsRed = Color(0xFFFF6B6B)
private val PsGreen = Color(0xFF4CD1A5)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PsKey(
    label: String, name: String, type: Int, learning: String?, onLearn: (String) -> Unit,
    color: Color = MaterialTheme.colorScheme.primary, size: Int = 52, wide: Boolean = false
) {
    val code = Prefs.map(type)[name] ?: 0
    val active = GamepadHub.key(code)
    val isLearning = learning == name
    val shape = if (wide) RoundedCornerShape(14.dp) else CircleShape
    Box(
        Modifier.then(if (wide) Modifier.height(size.dp).widthIn(min = 72.dp) else Modifier.size(size.dp))
            .clip(shape)
            .background(if (isLearning) WarnYellow else if (active) color else MaterialTheme.colorScheme.surfaceContainerHigh)
            .combinedClickable(onClick = {}, onLongClick = { onLearn(name) })
            .padding(horizontal = if (wide) 10.dp else 0.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label, fontSize = if (wide) 13.sp else 22.sp, fontWeight = FontWeight.Bold,
            color = if (active || isLearning) Color(0xFF0B1220) else color
        )
    }
}

@Composable
fun PsModeScreen(type: Int, onBack: () -> Unit) {
    var learning by remember { mutableStateOf<String?>(null) }
    val legacy = type == 2

    DisposableEffect(learning) {
        val l: (Int, Boolean, Long) -> Unit = { code, down, _ ->
            val n = learning
            if (n != null && down) { Prefs.learn(type, n, code); learning = null }
        }
        GamepadHub.keyListeners.add(l)
        onDispose { GamepadHub.keyListeners.remove(l) }
    }
    val onLearn: (String) -> Unit = { learning = if (learning == it) null else it }
    val l2 = if (legacy) GamepadHub.triggerLegacy(true).coerceAtLeast(GamepadHub.triggerStd(true)) else GamepadHub.triggerStd(true)
    val r2 = if (legacy) GamepadHub.triggerLegacy(false).coerceAtLeast(GamepadHub.triggerStd(false)) else GamepadHub.triggerStd(false)

    ToolScaffold(if (legacy) "Modo PS (Tipo 2)" else "Modo PS (Tipo 1)", onBack) {
        if (legacy) Hint("Layout alternativo para controles que o Android lê com os botões em outra ordem. Se algum botão acender trocado, segure o botão na tela e depois aperte o botão físico correto para reaprender.")
        else Hint("Layout padrão do Android para DualShock/DualSense. Se algo acender trocado, use o Tipo 2.")
        if (learning != null) Text("Aperte agora o botão físico para “$learning”…", color = WarnYellow, fontWeight = FontWeight.Medium)

        SectionCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f).padding(end = 8.dp)) { BarMeter("L2", l2); Spacer(Modifier.height(8.dp)); PsKey("L1", "L1", type, learning, onLearn, wide = true, size = 40) }
                Column(Modifier.weight(1f).padding(start = 8.dp), horizontalAlignment = Alignment.End) {
                    BarMeter("R2", r2); Spacer(Modifier.height(8.dp)); PsKey("R1", "R1", type, learning, onLearn, wide = true, size = 40)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                // D-pad
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    DirKey("▲", GamepadHub.dpadUp())
                    Row { DirKey("◀", GamepadHub.dpadLeft()); Spacer(Modifier.size(40.dp)); DirKey("▶", GamepadHub.dpadRight()) }
                    DirKey("▼", GamepadHub.dpadDown())
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PsKey("SHARE", "SHARE", type, learning, onLearn, wide = true, size = 34)
                    PsKey("PS", "PS", type, learning, onLearn, size = 44)
                    PsKey("OPTIONS", "OPTIONS", type, learning, onLearn, wide = true, size = 34)
                }
                // Botões de face
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PsKey("△", "TRI", type, learning, onLearn, PsGreen, 46)
                    Row(horizontalArrangement = Arrangement.spacedBy(30.dp)) {
                        PsKey("□", "SQU", type, learning, onLearn, PsPink, 46)
                        PsKey("○", "CIR", type, learning, onLearn, PsRed, 46)
                    }
                    PsKey("✕", "CRO", type, learning, onLearn, PsBlue, 46)
                }
            }
            val (lx, ly) = GamepadHub.leftStick()
            val (rx, ry) = GamepadHub.rightStick()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    StickView(lx, ly, Modifier.width(110.dp)); Spacer(Modifier.height(6.dp)); PsKey("L3", "L3", type, learning, onLearn, wide = true, size = 34)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    StickView(rx, ry, Modifier.width(110.dp)); Spacer(Modifier.height(6.dp)); PsKey("R3", "R3", type, learning, onLearn, wide = true, size = 34)
                }
            }
        }
        SectionCard("Mapeamento atual (keyCode)") {
            Hint(Prefs.map(type).entries.sortedBy { it.value }.joinToString("  ·  ") { "${it.key}=${it.value}" })
            OutlinedButton(onClick = { Prefs.resetMap(type); learning = null }) { Text("Restaurar mapeamento padrão") }
        }
    }
}

@Composable
private fun DirKey(sym: String, active: Boolean) {
    Box(
        Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
            .background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center
    ) { Text(sym, color = if (active) Color(0xFF0B1220) else MaterialTheme.colorScheme.primary) }
}

// ------------------------------------------------------------------ Modo Universal
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UniversalScreen(onBack: () -> Unit) {
    val keys = GamepadHub.seenScan.keys.sorted()
    val axisIds = GamepadHub.ranges.keys.sorted()
    ToolScaffold("Modo Universal", onBack, actions = {
        TextButton(onClick = { GamepadHub.select(GamepadHub.selectedId) }) { Text("Limpar") }
    }) {
        SectionCard("Botões detectados (${keys.size})") {
            if (keys.isEmpty()) Hint("Aperte botões no controle: cada um aparece numerado conforme é detectado.")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                keys.forEachIndexed { i, k ->
                    StatusChip("${i + 1} · ${GamepadHub.keyName(k)}", GamepadHub.key(k))
                }
            }
            val last = GamepadHub.lastKey
            if (last != 0) Hint("Último: ${GamepadHub.keyName(last)}  (keyCode $last, scanCode ${GamepadHub.seenScan[last] ?: "?"})")
        }
        SectionCard("Eixos (${axisIds.size})") {
            if (axisIds.isEmpty()) Hint("Nenhum eixo informado pelo controle ainda. Mova um analógico.")
            axisIds.forEach { a ->
                val r = GamepadHub.ranges[a]!!
                val v = GamepadHub.axis(a)
                BarMeter("${GamepadHub.axisName(a)}  [${r.first}..${r.second}]", GamepadHub.norm01(a), valueText = "%.3f".format(v))
            }
        }
    }
}
