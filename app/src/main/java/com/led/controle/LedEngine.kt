package com.led.controle

import android.content.Context
import android.graphics.Color
import android.hardware.input.InputManager
import android.hardware.lights.Light
import android.hardware.lights.LightState
import android.hardware.lights.LightsManager
import android.hardware.lights.LightsRequest
import android.view.InputDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

enum class Effect(val label: String) {
    FIXED("Cor fixa"),
    BREATHING("Respirar"),
    RAINBOW("Arco-íris"),
    STROBE("Estroboscópio"),
    POLICE("Polícia"),
    RGB_TEST("Teste R-G-B")
}

data class LedCfg(val color: Int, val effect: Effect, val speed: Float, val brightness: Float)

fun scaleColor(c: Int, k: Float): Int {
    val f = k.coerceIn(0f, 1f)
    return Color.rgb((Color.red(c) * f).toInt(), (Color.green(c) * f).toInt(), (Color.blue(c) * f).toInt())
}

/** Primeiro controle que expõe LED RGB ao Android. */
fun firstRgbDevice(ctx: Context): InputDevice? =
    InputDevice.getDeviceIds().mapNotNull { InputDevice.getDevice(it) }
        .filter { GamepadHub.isGamepad(it) }
        .firstOrNull { d -> runCatching { d.lightsManager.lights.any { it.hasRgbControl() } }.getOrDefault(false) }

/** Abre uma sessão de luzes em um controle e executa efeitos nele. */
class LedRunner(private val scope: CoroutineScope) {
    private var session: LightsManager.LightsSession? = null
    private var job: Job? = null
    var lights: List<Light> = emptyList(); private set

    val hasRgb get() = lights.any { it.hasRgbControl() }
    val hasPlayerLights get() = lights.any { it.type == Light.LIGHT_TYPE_PLAYER_ID }

    fun attach(dev: InputDevice?) {
        stopJob(); closeSession(); lights = emptyList()
        if (dev == null) return
        runCatching {
            val lm = dev.lightsManager
            lights = lm.lights
            session = lm.openSession()
        }
    }

    fun apply(c: LedCfg) {
        stopJob()
        if (session == null) return
        job = scope.launch { runEffect(c) }
    }

    fun setPlayerId(id: Int) {
        val s = session ?: return
        val req = LightsRequest.Builder()
        var any = false
        lights.filter { it.type == Light.LIGHT_TYPE_PLAYER_ID }.forEach {
            req.addLight(it, LightState.Builder().setPlayerId(id).build()); any = true
        }
        if (any) runCatching { s.requestLights(req.build()) }
    }

    fun stop() { stopJob(); closeSession() }

    private fun stopJob() { job?.cancel(); job = null }
    private fun closeSession() { runCatching { session?.close() }; session = null }

    private fun push(color: Int) {
        val s = session ?: return
        val req = LightsRequest.Builder()
        var any = false
        for (l in lights) if (l.hasRgbControl()) {
            req.addLight(l, LightState.Builder().setColor(color).build()); any = true
        }
        if (any) runCatching { s.requestLights(req.build()) }
    }

    private suspend fun runEffect(c: LedCfg) {
        val b = c.brightness
        val spd = 0.3f + c.speed * 2f
        when (c.effect) {
            Effect.FIXED -> push(scaleColor(c.color, b))
            Effect.BREATHING -> {
                var t = 0f
                while (true) {
                    push(scaleColor(c.color, b * (sin(t) + 1f) / 2f))
                    t += 0.12f * spd; delay(33)
                }
            }
            Effect.RAINBOW -> {
                var h = 0f
                while (true) {
                    push(scaleColor(Color.HSVToColor(floatArrayOf(h, 1f, 1f)), b))
                    h = (h + 3f * spd) % 360f; delay(33)
                }
            }
            Effect.STROBE -> {
                val p = (400f / spd).toLong().coerceAtLeast(40L)
                while (true) { push(scaleColor(c.color, b)); delay(p); push(Color.BLACK); delay(p) }
            }
            Effect.POLICE -> while (true) {
                repeat(3) { push(scaleColor(Color.RED, b)); delay(80); push(Color.BLACK); delay(60) }
                repeat(3) { push(scaleColor(Color.BLUE, b)); delay(80); push(Color.BLACK); delay(60) }
            }
            Effect.RGB_TEST -> while (true) {
                for (cc in intArrayOf(Color.RED, Color.GREEN, Color.BLUE, Color.WHITE, Color.BLACK)) {
                    push(scaleColor(cc, b)); delay((1500f / spd).toLong().coerceAtLeast(300L))
                }
            }
        }
    }
}
