package com.led.controle.ui

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.led.controle.GamepadHub
import com.led.controle.Prefs

// ------------------------------------------------------------------ Sensores
@Composable
fun SensorsScreen(onBack: () -> Unit) {
    val dev = GamepadHub.selected
    val sm: SensorManager? = remember(dev?.id) { dev?.sensorManager }
    val gyro = remember(sm) { sm?.getDefaultSensor(Sensor.TYPE_GYROSCOPE) }
    val accel = remember(sm) { sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }
    var g by remember { mutableStateOf(floatArrayOf(0f, 0f, 0f)) }
    var a by remember { mutableStateOf(floatArrayOf(0f, 0f, 0f)) }

    DisposableEffect(sm) {
        val l = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                if (e.sensor.type == Sensor.TYPE_GYROSCOPE) g = e.values.copyOf() else a = e.values.copyOf()
            }
            override fun onAccuracyChanged(s: Sensor?, acc: Int) {}
        }
        gyro?.let { sm?.registerListener(l, it, SensorManager.SENSOR_DELAY_GAME) }
        accel?.let { sm?.registerListener(l, it, SensorManager.SENSOR_DELAY_GAME) }
        onDispose { sm?.unregisterListener(l) }
    }

    ToolScaffold("Sensores", onBack) {
        if (dev == null) { Hint("Conecte um controle."); return@ToolScaffold }
        if (gyro == null && accel == null) {
            SectionCard("Sem sensores") { Hint("Este controle não expõe giroscópio/acelerômetro ao Android. DualShock 4 e DualSense normalmente expõem no Android 13+ via Bluetooth/USB; outros nem sempre.") }
            return@ToolScaffold
        }
        if (gyro != null) SectionCard("Giroscópio (rad/s)") {
            listOf("X", "Y", "Z").forEachIndexed { i, n -> BarMeter(n, (g[i] / 10f + 0.5f), valueText = "%.3f".format(g[i])) }
        }
        if (accel != null) SectionCard("Acelerômetro (m/s²)") {
            listOf("X", "Y", "Z").forEachIndexed { i, n -> BarMeter(n, (a[i] / 40f + 0.5f), valueText = "%.2f".format(a[i])) }
            Hint("Parado sobre a mesa, um dos eixos deve ficar perto de ±9,8.")
        }
    }
}

// ------------------------------------------------------------------ Info do controle
@Composable
fun DeviceInfoScreen(onBack: () -> Unit) {
    val dev = GamepadHub.selected
    ToolScaffold("Info do controle", onBack) {
        if (dev == null) { Hint("Conecte um controle."); return@ToolScaffold }
        SectionCard("Identificação") {
            Row2("Nome", dev.name)
            Row2("ID do dispositivo", "${dev.id}")
            Row2("Vendor ID", "0x%04X".format(dev.vendorId))
            Row2("Product ID", "0x%04X".format(dev.productId))
            Row2("Descritor", dev.descriptor.take(12) + "…")
            Row2("Microfone", if (dev.hasMicrophone()) "sim" else "não")
        }
        SectionCard("Bateria") {
            val b = runCatching { dev.batteryState }.getOrNull()
            if (b == null || !b.isPresent) Hint("O Android não informa bateria deste controle (ou é cabeado).")
            else {
                BarMeter("Carga", b.capacity, valueText = "%.0f%%".format(b.capacity * 100))
                Row2("Status", when (b.status) { 2 -> "carregando"; 3 -> "descarregando"; 4 -> "não carrega"; 5 -> "cheia"; else -> "desconhecido" })
            }
        }
        SectionCard("Recursos") {
            val lights = runCatching { dev.lightsManager.lights }.getOrDefault(emptyList())
            Row2("Luzes", "${lights.size} (${lights.count { it.hasRgbControl() }} RGB)")
            Row2("Motores de vibração", "${runCatching { dev.vibratorManager.vibratorIds.size }.getOrDefault(0)}")
            val sm = runCatching { dev.sensorManager }.getOrNull()
            Row2("Giroscópio", if (sm?.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null) "sim" else "não")
            Row2("Acelerômetro", if (sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) "sim" else "não")
        }
        SectionCard("Botões suportados") {
            val codes = intArrayOf(96, 97, 99, 100, 102, 103, 104, 105, 106, 107, 108, 109, 110, 19, 20, 21, 22)
            val has = dev.hasKeys(*codes)
            Hint(codes.indices.joinToString("  ") { (if (has[it]) "✔ " else "✘ ") + GamepadHub.keyName(codes[it]).removePrefix("BUTTON_") })
        }
        SectionCard("Eixos (${GamepadHub.ranges.size})") {
            GamepadHub.ranges.keys.sorted().forEach { id ->
                val r = dev.getMotionRange(id, android.view.InputDevice.SOURCE_JOYSTICK)
                Hint("${GamepadHub.axisName(id)}: ${r?.min}..${r?.max}  flat ${r?.flat}  fuzz ${r?.fuzz}")
            }
        }
    }
}

@Composable
private fun Row2(k: String, v: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(k, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        Text(v, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 12.dp))
    }
}

// ------------------------------------------------------------------ Diagnóstico completo
private class DiagItem(val id: String, val label: String, val auto: (() -> Boolean)?)

@Composable
fun DiagnosticScreen(onBack: () -> Unit) {
    val H = GamepadHub
    val items = remember {
        listOf(
            DiagItem("A", "Botão A / ✕") { H.key(96) }, DiagItem("B", "Botão B / ○") { H.key(97) },
            DiagItem("X", "Botão X / □") { H.key(99) }, DiagItem("Y", "Botão Y / △") { H.key(100) },
            DiagItem("L1", "L1") { H.key(102) }, DiagItem("R1", "R1") { H.key(103) },
            DiagItem("L2", "L2 (até o fundo)") { H.triggerStd(true) > 0.95f || H.triggerLegacy(true) > 0.95f },
            DiagItem("R2", "R2 (até o fundo)") { H.triggerStd(false) > 0.95f || H.triggerLegacy(false) > 0.95f },
            DiagItem("L3", "Clique do analógico esquerdo") { H.key(106) }, DiagItem("R3", "Clique do analógico direito") { H.key(107) },
            DiagItem("SEL", "Select / Share") { H.key(109) }, DiagItem("STA", "Start / Options") { H.key(108) },
            DiagItem("UP", "D-pad ↑") { H.dpadUp() }, DiagItem("DN", "D-pad ↓") { H.dpadDown() },
            DiagItem("LT", "D-pad ←") { H.dpadLeft() }, DiagItem("RT", "D-pad →") { H.dpadRight() },
            DiagItem("LXN", "Analógico esq. ←") { H.axis(MotionEvent.AXIS_X) < -0.9f },
            DiagItem("LXP", "Analógico esq. →") { H.axis(MotionEvent.AXIS_X) > 0.9f },
            DiagItem("LYN", "Analógico esq. ↑") { H.axis(MotionEvent.AXIS_Y) < -0.9f },
            DiagItem("LYP", "Analógico esq. ↓") { H.axis(MotionEvent.AXIS_Y) > 0.9f },
            DiagItem("RXN", "Analógico dir. ←") { H.axis(MotionEvent.AXIS_Z) < -0.9f },
            DiagItem("RXP", "Analógico dir. →") { H.axis(MotionEvent.AXIS_Z) > 0.9f },
            DiagItem("RYN", "Analógico dir. ↑") { H.axis(MotionEvent.AXIS_RZ) < -0.9f },
            DiagItem("RYP", "Analógico dir. ↓") { H.axis(MotionEvent.AXIS_RZ) > 0.9f },
            DiagItem("VIB", "Vibração funcionou?", null),
            DiagItem("LED", "LED acendeu/mudou de cor?", null)
        )
    }
    val done = remember { mutableStateMapOf<String, Boolean>() }
    LaunchedEffect(Unit) {
        snapshotFlow { items.filter { it.auto?.invoke() == true }.map { it.id } }.collect { ids -> ids.forEach { done[it] = true } }
    }
    val ok = items.count { done[it.id] == true }
    val manualIds = items.filter { it.auto == null }.map { it.id }

    ToolScaffold("Diagnóstico completo", onBack, actions = { TextButton(onClick = { done.clear() }) { Text("Reiniciar") } }) {
        SectionCard {
            BarMeter("Progresso", ok / items.size.toFloat(), valueText = "$ok / ${items.size}")
            Hint("Aperte cada botão e mova cada analógico até o limite. Os itens vão ficando verdes sozinhos. Vibração e LED você confirma manualmente (teste-os nas telas próprias).")
        }
        SectionCard("Itens") {
            items.forEach { it ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(it.label, color = if (done[it.id] == true) OkGreen else MaterialTheme.colorScheme.onSurface)
                    if (it.auto == null) {
                        if (done[it.id] == true) Text("✔", color = OkGreen)
                        else TextButton(onClick = { done[it.id] = true }, contentPadding = PaddingValues(0.dp)) { Text("Funcionou") }
                    } else Text(if (done[it.id] == true) "✔" else "…", color = if (done[it.id] == true) OkGreen else Color.Gray)
                }
            }
        }
        SectionCard("Relatório") {
            val failed = items.filter { done[it.id] != true }
            if (failed.isEmpty()) Text("Tudo certo: nenhum item com falha ✔", color = OkGreen, fontWeight = FontWeight.Bold)
            else {
                Text("Pendentes (${failed.size}):", fontWeight = FontWeight.Bold)
                Hint(failed.joinToString(", ") { it.label })
            }
        }
    }
}

// ------------------------------------------------------------------ Guia / Sobre / Configurações
@Composable
fun GuideScreen(onBack: () -> Unit) {
    ToolScaffold("Guia", onBack, showDevice = false) {
        SectionCard("Como usar") {
            Hint("1. Conecte o controle por USB (OTG) ou Bluetooth e aperte um botão: ele aparece no topo.\n2. Escolha um modo de teste. Se houver vários controles, toque na barra do controle para trocar.\n3. Use “Modo Android” primeiro; se um botão PlayStation acender trocado, tente PS Tipo 1 e depois Tipo 2.")
        }
        SectionCard("LED") {
            Hint("O LED só funciona se o Android expuser a luz do controle (API LightsManager, Android 12+). DualShock 4 e DualSense costumam funcionar; controles genéricos geralmente não. Em “Luzes encontradas” você vê o que o seu controle expõe.")
        }
        SectionCard("Vibração") {
            Hint("Sliders controlam o motor grave (esquerdo) e agudo (direito) separadamente. A varredura sobe a força motor por motor para achar defeitos.")
        }
        SectionCard("Aprender botões (PS)") {
            Hint("Nos modos PS, segure um botão desenhado na tela e aperte o botão físico correto: o app grava o mapeamento para aquele tipo.")
        }
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    ToolScaffold("Sobre", onBack, showDevice = false) {
        SectionCard("LED Controle 2.0") {
            Hint("Teste de controles e controle da luz LED.")
            Row2("Android", "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            Row2("Aparelho", "${Build.MANUFACTURER} ${Build.MODEL}")
            Row2("Pacote", "com.led.controle")
        }
    }
}

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    ToolScaffold("Configurações", onBack, showDevice = false) {
        SectionCard("Geral") {
            SwitchRow("Manter tela ligada", "Evita a tela apagar durante os testes.", Prefs.keepOn) { Prefs.setKeepOn(it) }
        }
        SectionCard("Zona morta dos analógicos") {
            Text("${(Prefs.deadzone * 100).toInt()}%")
            Slider(value = Prefs.deadzone, onValueChange = { Prefs.setDeadzone(it) }, valueRange = 0f..0.4f)
            Hint("Aparece como círculo tracejado nos testes de analógicos.")
        }
    }
}
