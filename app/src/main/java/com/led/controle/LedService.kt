package com.led.controle

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.input.InputManager
import android.os.IBinder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/** Mantém a cor/efeito do LED ativo mesmo com o app fechado. */
class LedService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var runner: LedRunner
    private lateinit var im: InputManager

    private val listener = object : InputManager.InputDeviceListener {
        override fun onInputDeviceAdded(deviceId: Int) = rebind()
        override fun onInputDeviceRemoved(deviceId: Int) = rebind()
        override fun onInputDeviceChanged(deviceId: Int) {}
    }

    override fun onCreate() {
        super.onCreate()
        Cfg.init(this)
        runner = LedRunner(scope)
        im = getSystemService(InputManager::class.java)
        im.registerInputDeviceListener(listener, null)
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "LED Controle", NotificationManager.IMPORTANCE_LOW))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            Cfg.service = false
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(1, notification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        rebind()
        return START_STICKY
    }

    private fun rebind() {
        val dev = firstRgbDevice(this)
        runner.attach(dev)
        if (dev != null) runner.apply(Cfg.led)
    }

    private fun notification(): Notification {
        val stop = PendingIntent.getService(
            this, 0, Intent(this, LedService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_led)
            .setContentTitle("LED Controle ativo")
            .setContentText("Toque em Parar para devolver o LED ao normal")
            .setOngoing(true)
            .addAction(Notification.Action.Builder(null, "Parar", stop).build())
            .build()
    }

    override fun onDestroy() {
        runner.stop()
        runCatching { im.unregisterInputDeviceListener(listener) }
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL = "led"
        const val ACTION_STOP = "com.led.controle.STOP"

        fun start(ctx: Context) =
            ContextCompat.startForegroundService(ctx, Intent(ctx, LedService::class.java))

        fun stop(ctx: Context) {
            Cfg.service = false
            ctx.stopService(Intent(ctx, LedService::class.java))
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        Cfg.init(context)
        if (Cfg.service && Cfg.boot) LedService.start(context)
    }
}
