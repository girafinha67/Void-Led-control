# Controler Tester 6.0

Projeto Android (Kotlin + Jetpack Compose, Material 3). minSdk 31 (Android 12+).
Antigo "LED Controle" — agora com identidade preto e branco, foto do controle conectado
(DualSense, DualShock 4, Xbox ou genérico), carrossel infinito de testes e animações em todas as telas.

## Como gerar o APK
1. Abra a pasta no Android Studio (Koala ou mais novo) e aguarde o Gradle sincronizar.
2. Build > Build APK(s). O arquivo sai em app/build/outputs/apk/debug/app-debug.apk
   (ou, pelo terminal com o Gradle instalado: gradle assembleDebug).
3. Ou envie para o GitHub: a Action "Build APK" gera o app-debug.apk como artefato.

## Estrutura
- GamepadHub.kt        entrada: botões e eixos de qualquer controle
- ControllerModel.kt   detecta o modelo do controle (foto da tela inicial) e a flag PHOTO_GRAYSCALE
- LedEngine.kt         controle do LED (LightsManager) e efeitos
- LedService.kt        serviço que mantém o LED com o app fechado + BootReceiver
- Prefs.kt             configurações e mapeamento dos botões PS
- ui/Theme.kt          paleta P&B e fonte condensada
- ui/Components.kt     fundo granulado, cabeçalho, cards e animações (appear, bounceClick)
- ui/HomeScreen.kt     tela inicial (hero com foto, player, carrossel, círculos de status)
- res/drawable-nodpi/  fotos dos controles (ctrl_*.webp) e logo (app_logo, ic_launcher_art)

by: H3NRI_013/girafinha67
