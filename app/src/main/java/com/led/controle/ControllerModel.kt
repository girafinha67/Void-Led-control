package com.led.controle

import android.view.InputDevice

/** Troque para false se quiser as fotos dos controles coloridas (como no original). */
const val PHOTO_GRAYSCALE = true

/** Assinatura do projeto, exibida no app. */
const val WATERMARK = "by: H3NRI_013/girafinha67"

/** Modelo de controle reconhecido, usado para mostrar a foto certa na tela inicial. */
enum class ControllerModel(val label: String, val photo: Int?) {
    NONE("Nenhum controle", null),
    DUALSENSE("DualSense", R.drawable.ctrl_dualsense),
    DUALSHOCK4("DualShock 4", R.drawable.ctrl_ds4),
    XBOX("Xbox", R.drawable.ctrl_xbox),
    GENERIC("Controle", R.drawable.ctrl_generic);

    companion object {
        private const val SONY = 0x054C
        private const val MICROSOFT = 0x045E

        fun detect(d: InputDevice?): ControllerModel {
            if (d == null) return NONE
            val name = d.name.lowercase()
            val vendor = d.vendorId
            val product = d.productId
            return when {
                "dualsense" in name || (vendor == SONY && (product == 0x0CE6 || product == 0x0DF2)) -> DUALSENSE
                "dualshock" in name || "ps4" in name || (vendor == SONY) ||
                    "wireless controller" in name -> DUALSHOCK4
                vendor == MICROSOFT || "xbox" in name || "x-box" in name || "microsoft" in name -> XBOX
                else -> GENERIC
            }
        }
    }
}
