package com.led.controle

import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Central de entrada: recebe todos os eventos de controle (botões e eixos) do Activity
 * e expõe o estado como estado observável do Compose.
 */
object GamepadHub {
    var devices by mutableStateOf(listOf<InputDevice>()); private set
    var selectedId by mutableIntStateOf(-1); private set

    val pressed = mutableStateMapOf<Int, Boolean>()
    val pressCount = mutableStateMapOf<Int, Int>()
    val seenScan = mutableStateMapOf<Int, Int>()
    val axes = mutableStateMapOf<Int, Float>()
    val ranges = mutableStateMapOf<Int, Pair<Float, Float>>()

    var lastKey by mutableIntStateOf(0); private set
    var maxSimultaneous by mutableIntStateOf(0); private set
    var samples by mutableLongStateOf(0L); private set   // amostras de eixo recebidas (inclui histórico)
    var batches by mutableLongStateOf(0L); private set   // eventos de movimento recebidos

    /** Intervalos (ms) entre eventos de movimento consecutivos – usado para jitter/polling. */
    val gaps = FloatArray(256)
    var gapIndex = 0; private set
    private var lastBatchNs = 0L

    /** Ouvintes de teclas (keyCode, isDown, nanoTime) – usado em reação, aprendizado e diagnóstico. */
    val keyListeners = mutableListOf<(Int, Boolean, Long) -> Unit>()

    val selected: InputDevice? get() = devices.firstOrNull { it.id == selectedId }

    fun isGamepad(d: InputDevice): Boolean {
        if (d.isVirtual) return false
        val s = d.sources
        return (s and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
            (s and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
    }

    fun refresh() {
        val list = InputDevice.getDeviceIds().mapNotNull { InputDevice.getDevice(it) }.filter { isGamepad(it) }
        devices = list
        if (list.none { it.id == selectedId }) select(list.firstOrNull()?.id ?: -1)
    }

    fun select(id: Int) {
        selectedId = id
        pressed.clear(); axes.clear(); ranges.clear(); pressCount.clear(); seenScan.clear()
        maxSimultaneous = 0; samples = 0; batches = 0; gapIndex = 0; lastBatchNs = 0
        selected?.motionRanges?.forEach { r ->
            if ((r.source and InputDevice.SOURCE_JOYSTICK) != 0) ranges[r.axis] = r.min to r.max
        }
    }

    fun clearCounters() {
        pressCount.clear(); maxSimultaneous = 0; samples = 0; batches = 0; gapIndex = 0; lastBatchNs = 0
    }

    private fun adopt(deviceId: Int): Boolean {
        if (deviceId != selectedId) {
            if (devices.none { it.id == deviceId }) refresh()
            if (selectedId == -1 && devices.any { it.id == deviceId }) select(deviceId)
        }
        return deviceId == selectedId
    }

    fun onKey(e: KeyEvent): Boolean {
        val s = e.source
        val pad = (s and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
            (s and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK ||
            ((s and InputDevice.SOURCE_DPAD) == InputDevice.SOURCE_DPAD && (e.device?.let { isGamepad(it) } == true))
        if (!pad || e.keyCode == KeyEvent.KEYCODE_BACK) return false
        if (!adopt(e.deviceId)) return false
        val down = e.action == KeyEvent.ACTION_DOWN
        if (down && e.repeatCount > 0) return true
        pressed[e.keyCode] = down
        seenScan[e.keyCode] = e.scanCode
        if (down) {
            pressCount[e.keyCode] = (pressCount[e.keyCode] ?: 0) + 1
            lastKey = e.keyCode
            maxSimultaneous = maxOf(maxSimultaneous, pressed.count { it.value })
        }
        val t = System.nanoTime()
        keyListeners.toList().forEach { it(e.keyCode, down, t) }
        return true
    }

    fun onMotion(e: MotionEvent): Boolean {
        if ((e.source and InputDevice.SOURCE_JOYSTICK) != InputDevice.SOURCE_JOYSTICK || e.action != MotionEvent.ACTION_MOVE) return false
        if (!adopt(e.deviceId)) return false
        val dev = e.device ?: return false
        for (r in dev.motionRanges) {
            if ((r.source and InputDevice.SOURCE_JOYSTICK) == 0) continue
            ranges[r.axis] = r.min to r.max
            axes[r.axis] = e.getAxisValue(r.axis)
        }
        val now = System.nanoTime()
        if (lastBatchNs != 0L) {
            gaps[gapIndex % gaps.size] = (now - lastBatchNs) / 1_000_000f
            gapIndex++
        }
        lastBatchNs = now
        samples += (e.historySize + 1)
        batches++
        return true
    }

    // ---------- Helpers de leitura ----------
    fun key(code: Int) = pressed[code] == true
    fun axis(a: Int) = axes[a] ?: 0f

    /** Valor do eixo normalizado para 0..1 usando o range real do controle. */
    fun norm01(a: Int): Float {
        val r = ranges[a] ?: return 0f
        val v = axes[a] ?: r.first
        val span = r.second - r.first
        return if (span <= 0f) 0f else ((v - r.first) / span).coerceIn(0f, 1f)
    }

    fun dpadLeft() = key(KeyEvent.KEYCODE_DPAD_LEFT) || axis(MotionEvent.AXIS_HAT_X) < -0.5f
    fun dpadRight() = key(KeyEvent.KEYCODE_DPAD_RIGHT) || axis(MotionEvent.AXIS_HAT_X) > 0.5f
    fun dpadUp() = key(KeyEvent.KEYCODE_DPAD_UP) || axis(MotionEvent.AXIS_HAT_Y) < -0.5f
    fun dpadDown() = key(KeyEvent.KEYCODE_DPAD_DOWN) || axis(MotionEvent.AXIS_HAT_Y) > 0.5f

    /** Gatilho padrão do Android (LTRIGGER/RTRIGGER, BRAKE/GAS ou botão L2/R2). */
    fun triggerStd(left: Boolean): Float = maxOf(
        norm01(if (left) MotionEvent.AXIS_LTRIGGER else MotionEvent.AXIS_RTRIGGER),
        norm01(if (left) MotionEvent.AXIS_BRAKE else MotionEvent.AXIS_GAS),
        if (key(if (left) KeyEvent.KEYCODE_BUTTON_L2 else KeyEvent.KEYCODE_BUTTON_R2)) 1f else 0f
    )

    /** Gatilho do layout legado (RX/RY). */
    fun triggerLegacy(left: Boolean): Float =
        norm01(if (left) MotionEvent.AXIS_RX else MotionEvent.AXIS_RY)

    fun leftStick() = axis(MotionEvent.AXIS_X) to axis(MotionEvent.AXIS_Y)
    fun rightStick() = axis(MotionEvent.AXIS_Z) to axis(MotionEvent.AXIS_RZ)

    fun keyName(code: Int) = KeyEvent.keyCodeToString(code).removePrefix("KEYCODE_")
    fun axisName(a: Int) = MotionEvent.axisToString(a).removePrefix("AXIS_")
}
