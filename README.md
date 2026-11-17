# LED Controle 2.0

Projeto Android (Kotlin + Jetpack Compose, Material 3). minSdk 31 (Android 12+).

## Como gerar o APK
1. Abra a pasta no Android Studio (Koala ou mais novo) e aguarde o Gradle sincronizar.
2. Build > Build APK(s). O arquivo sai em app/build/outputs/apk/debug/app-debug.apk
   (ou, pelo terminal com o Gradle instalado: gradle assembleDebug).

## Estrutura
- GamepadHub.kt   entrada: botões e eixos de qualquer controle
- LedEngine.kt    controle do LED (LightsManager) e efeitos
- LedService.kt   serviço que mantém o LED com o app fechado + BootReceiver
- Prefs.kt        configurações e mapeamento dos botões PS
- ui/             telas (Home, modos, vibração, LED, ferramentas, info)
