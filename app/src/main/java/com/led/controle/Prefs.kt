package com.led.controle

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Configuração persistente do LED (usada pela tela e pelo serviço em segundo plano). */
object Cfg {
    private lateinit var sp: SharedPreferences

    fun init(c: Context) {
        if (!::sp.isInitialized) sp = c.applicationContext.getSharedPreferences("led_cfg", Context.MODE_PRIVATE)
    }

    var led: LedCfg
        get() = LedCfg(
            sp.getInt("color", Color.CYAN),
            Effect.values().getOrElse(sp.getInt("effect", 0)) { Effect.FIXED },
            sp.getFloat("speed", 0.5f),
            sp.getFloat("bright", 1f)
        )
        set(v) {
            sp.edit().putInt("color", v.color).putInt("effect", v.effect.ordinal)
                .putFloat("speed", v.speed).putFloat("bright", v.brightness).apply()
        }

    var service: Boolean
        get() = sp.getBoolean("service", false)
        set(v) { sp.edit().putBoolean("service", v).apply() }

    var boot: Boolean
        get() = sp.getBoolean("boot", false)
        set(v) { sp.edit().putBoolean("boot", v).apply() }
}

/** Preferências gerais do app (observáveis pelo Compose). */
object Prefs {
    private lateinit var sp: SharedPreferences

    var deadzone by mutableFloatStateOf(0.10f); private set
    var keepOn by mutableStateOf(true); private set
    var showHints by mutableStateOf(true); private set

    /** Mapeamentos dos botões PS: nome -> keyCode. Índice 1 = Tipo 1, 2 = Tipo 2. */
    val ps1 = mutableStateMapOf<String, Int>()
    val ps2 = mutableStateMapOf<String, Int>()

    val defaultPs1 = mapOf(
        "CRO" to 96, "CIR" to 97, "SQU" to 99, "TRI" to 100,
        "L1" to 102, "R1" to 103, "L2" to 104, "R2" to 105,
        "L3" to 106, "R3" to 107, "SHARE" to 109, "OPTIONS" to 108, "PS" to 110
    )
    // Layout "genérico HID" (leitura errada do Android): botões numerados em sequência.
    // É um ponto de partida: use "Aprender" (toque longo no botão) para ajustar ao seu controle.
    val defaultPs2 = mapOf(
        "SQU" to 96, "CRO" to 97, "CIR" to 98, "TRI" to 99,
        "L1" to 100, "R1" to 101, "L2" to 102, "R2" to 103,
        "SHARE" to 104, "OPTIONS" to 105, "L3" to 106, "R3" to 107, "PS" to 108
    )

    fun init(c: Context) {
        if (::sp.isInitialized) return
        sp = c.applicationContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        deadzone = sp.getFloat("deadzone", 0.10f)
        keepOn = sp.getBoolean("keepOn", true)
        showHints = sp.getBoolean("hints", true)
        load(1, ps1, defaultPs1)
        load(2, ps2, defaultPs2)
    }

    private fun load(t: Int, target: MutableMap<String, Int>, def: Map<String, Int>) {
        target.clear(); target.putAll(def)
        sp.getString("psmap$t", null)?.split(";")?.forEach {
            val p = it.split("="); if (p.size == 2) p[1].toIntOrNull()?.let { code -> target[p[0]] = code }
        }
    }

    fun map(type: Int) = if (type == 1) ps1 else ps2

    fun learn(type: Int, name: String, code: Int) {
        map(type)[name] = code
        sp.edit().putString("psmap$type", map(type).entries.joinToString(";") { "${it.key}=${it.value}" }).apply()
    }

    fun resetMap(type: Int) {
        sp.edit().remove("psmap$type").apply()
        load(type, map(type), if (type == 1) defaultPs1 else defaultPs2)
    }

    fun setDeadzone(v: Float) { deadzone = v; sp.edit().putFloat("deadzone", v).apply() }
    fun setKeepOn(v: Boolean) { keepOn = v; sp.edit().putBoolean("keepOn", v).apply() }
    fun setShowHints(v: Boolean) { showHints = v; sp.edit().putBoolean("hints", v).apply() }
}
