package com.led.controle

import android.hardware.input.InputManager
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalView
import com.led.controle.ui.*

class MainActivity : ComponentActivity() {
    private lateinit var im: InputManager

    private val listener = object : InputManager.InputDeviceListener {
        override fun onInputDeviceAdded(deviceId: Int) = GamepadHub.refresh()
        override fun onInputDeviceRemoved(deviceId: Int) = GamepadHub.refresh()
        override fun onInputDeviceChanged(deviceId: Int) = GamepadHub.refresh()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Cfg.init(this)
        Prefs.init(this)
        im = getSystemService(InputManager::class.java)
        im.registerInputDeviceListener(listener, null)
        GamepadHub.refresh()
        setContent { LedTheme { AppRoot() } }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean =
        if (GamepadHub.onKey(event)) true else super.dispatchKeyEvent(event)

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean =
        if (GamepadHub.onMotion(event)) true else super.dispatchGenericMotionEvent(event)

    override fun onDestroy() {
        runCatching { im.unregisterInputDeviceListener(listener) }
        super.onDestroy()
    }
}

@Composable
fun AppRoot() {
    var route by rememberSaveable { mutableStateOf("home") }
    val back = { route = "home" }
    val view = LocalView.current
    LaunchedEffect(Prefs.keepOn) { view.keepScreenOn = Prefs.keepOn }
    BackHandler(enabled = route != "home") { back() }

    when (route) {
        "home" -> HomeScreen { route = it }
        "android" -> AndroidModeScreen(back)
        "ps1" -> PsModeScreen(1, back)
        "ps2" -> PsModeScreen(2, back)
        "universal" -> UniversalScreen(back)
        "vibration" -> VibrationScreen(back)
        "led" -> LedScreen(back)
        "sticks" -> SticksScreen(back)
        "triggers" -> TriggersScreen(back)
        "latency" -> LatencyScreen(back)
        "stress" -> StressScreen(back)
        "sensors" -> SensorsScreen(back)
        "info" -> DeviceInfoScreen(back)
        "diag" -> DiagnosticScreen(back)
        "guide" -> GuideScreen(back)
        "about" -> AboutScreen(back)
        "settings" -> SettingsScreen(back)
    }
}
