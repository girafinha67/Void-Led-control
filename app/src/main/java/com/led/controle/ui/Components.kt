package com.led.controle.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.led.controle.GamepadHub

/** Tela padrão de ferramenta: barra superior com voltar + conteúdo rolável. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolScaffold(
    title: String,
    onBack: () -> Unit,
    showDevice: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Medium) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") }
                },
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (showDevice) DeviceBar()
            content()
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun SectionCard(title: String? = null, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (title != null) Text(title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
fun Hint(text: String) = Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, lineHeight = 20.sp)

/** Seletor do controle ativo. */
@Composable
fun DeviceBar() {
    val devs = GamepadHub.devices
    val sel = GamepadHub.selected
    var open by remember { mutableStateOf(false) }
    Card(
        Modifier.fillMaxWidth().clickable(enabled = devs.size > 1) { open = true },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.SportsEsports, null, tint = if (sel != null) OkGreen else MaterialTheme.colorScheme.outline)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(sel?.name ?: "Nenhum controle conectado", fontWeight = FontWeight.Medium, maxLines = 1)
                Text(
                    if (sel != null) "Pressione um botão para testar" + if (devs.size > 1) " · toque para trocar" else ""
                    else "Conecte por USB/Bluetooth e mexa em um botão",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp
                )
            }
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
    val bg = if (active) activeColor else MaterialTheme.colorScheme.surfaceContainerHigh
    val fg = if (active) Color(0xFF0B1220) else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier.clip(RoundedCornerShape(12.dp)).background(bg).padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) { Text(label, color = fg, fontSize = 13.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center) }
}

@Composable
fun BarMeter(label: String, value: Float, modifier: Modifier = Modifier, valueText: String = "%.2f".format(value)) {
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(valueText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { value.coerceIn(0f, 1f) },
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
