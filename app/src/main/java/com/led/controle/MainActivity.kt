// by: H3NRI_013/girafinha67
package com.led.controle

import android.hardware.input.InputManager
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalView
import com.led.controle.ui.*

class MainActivity : ComponentActivity() {
    private lateinit var im: InputManager

    private val listener = object : InputManager.InputDeviceListener {
        override fun onInputDeviceAdded(deviceId: Int) = GamepadHub.refresh()
        override fun onInputDeviceRemoved(deviceId: Int) = GamepadHub.refresh()
        override fun onInputDeviceChanged(deviceId: Int) = GamepadHub.refresh(force = true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CrashLog.install(this)
        enableEdgeToEdge()
        Cfg.init(this)
        Prefs.init(this)
        im = getSystemService(InputManager::class.java)
        im.registerInputDeviceListener(listener, null)
        GamepadHub.refresh()
        setContent { LedTheme { AppBackground { AppRoot() } } }
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

    CrashDialog()

    AnimatedContent(
        targetState = route,
        transitionSpec = {
            if (targetState == "home") {
                (fadeIn(tween(380, 120)) + slideInHorizontally(tween(420, easing = FastOutSlowInEasing)) { -it / 5 } +
                    scaleIn(initialScale = 0.94f, animationSpec = tween(420))) togetherWith
                    (fadeOut(tween(240)) + slideOutHorizontally(tween(420, easing = FastOutSlowInEasing)) { it / 3 } +
                        scaleOut(targetScale = 1.04f, animationSpec = tween(300)))
            } else {
                (fadeIn(tween(380, 120)) + slideInHorizontally(tween(420, easing = FastOutSlowInEasing)) { it / 3 } +
                    scaleIn(initialScale = 0.94f, animationSpec = tween(420))) togetherWith
                    (fadeOut(tween(240)) + slideOutHorizontally(tween(420, easing = FastOutSlowInEasing)) { -it / 5 } +
                        scaleOut(targetScale = 0.96f, animationSpec = tween(300)))
            }
        },
        label = "screens"
    ) { r ->
        when (r) {
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
            else -> Unit
        }
    }
}
