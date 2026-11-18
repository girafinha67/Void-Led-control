// by: H3NRI_013/girafinha67
package com.led.controle.ui

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.led.controle.CrashLog

/** Se o app fechou por erro na última vez, mostra o motivo para você copiar e me enviar. */
@Composable
fun CrashDialog() {
    val ctx = LocalContext.current
    var report by remember { mutableStateOf(CrashLog.read(ctx)) }
    val text = report ?: return
    AlertDialog(
        onDismissRequest = { CrashLog.clear(ctx); report = null },
        title = { Text("O app fechou na última vez") },
        text = {
            Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                Text(text.take(4000), fontSize = 11.sp, lineHeight = 14.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                runCatching {
                    val cm = ctx.getSystemService(ClipboardManager::class.java)
                    cm.setPrimaryClip(ClipData.newPlainText("erro", text))
                }
            }) { Text("Copiar") }
        },
        dismissButton = {
            TextButton(onClick = { CrashLog.clear(ctx); report = null }) { Text("Fechar") }
        }
    )
}
