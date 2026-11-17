package com.led.controle.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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

@Composable
fun HomeScreen(nav: (String) -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("LED Controle", fontSize = 28.sp, modifier = Modifier.weight(1f))
            IconButton(onClick = { nav("guide") }) { Icon(Icons.Filled.MenuBook, "Guia") }
            IconButton(onClick = { nav("about") }) { Icon(Icons.Outlined.Info, "Sobre") }
            IconButton(onClick = { nav("settings") }) { Icon(Icons.Filled.Settings, "Configurações") }
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { DeviceBar() }
            items(main) { EntryCard(it) { nav(it.route) } }
            item {
                Text("Mais ferramentas", Modifier.padding(start = 8.dp, top = 8.dp), fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
            }
            items(extras) { EntryCard(it) { nav(it.route) } }
        }
    }
}

@Composable
private fun EntryCard(e: Entry, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(Modifier.padding(vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(0.32f), contentAlignment = Alignment.Center) {
                Icon(e.icon, null, Modifier.size(52.dp).rotate(e.rotate), tint = MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.weight(0.68f).padding(end = 18.dp)) {
                Text(e.title, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(e.desc, fontSize = 17.sp, lineHeight = 24.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
