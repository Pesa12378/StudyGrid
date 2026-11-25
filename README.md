# StudyGrid

App de Android para seguir metas de estudio diarias con un **gráfico de contribuciones estilo GitHub**. Funciona 100% offline, sin cuentas, sin anuncios y sin telemetría.

La interfaz mezcla **Frutiger Aero** (cielo, pasto, burbujas de jabón, botones "gel") con **Liquid Glass**: paneles de vidrio que desenfocan y refractan lo que tienen detrás mediante un shader AGSL.

## Características

- **Metas**: crear, editar y eliminar, con título, descripción y uno de 5 colores. Para borrar hay que escribir el nombre de la meta, así no se pierde el progreso por accidente.
- **Heatmap de 26 semanas** (lunes a domingo) con meses en español. Abre en la semana actual y los días futuros no se pueden tocar.
- **"+1 Hoy"**: sube la intensidad de hoy (0 → 4) con vibración. Se detiene en 4.
- **Registro de cualquier día**: tocas un cuadrito, eliges el nivel (Nada, Bajo, Medio, Alto, Intenso) y escribes una nota.
- **Métricas por meta**: racha actual (cuenta hasta hoy, o hasta ayer si hoy aún no registras), mejor racha histórica y total de días activos.
- **Respaldo local**: exporta e importa un `.json` con el selector de archivos de Android. Importar reemplaza todo, pero primero valida el archivo y lo aplica en una transacción: si el archivo está mal, no cambia nada.
- **Modo oscuro y claro** (Aero de noche y de día), con un botón en la barra superior.
- Las burbujas animadas se detienen si el sistema tiene activado "Quitar animaciones".

## Privacidad

La app **no tiene permiso de `INTERNET`**. Todo se guarda en una base de datos SQLite local (Room), y los datos solo salen del teléfono si tú exportas un respaldo.

## Stack

| | |
|---|---|
| Lenguaje | Kotlin 2.1 |
| UI | Jetpack Compose + Material 3 |
| Persistencia | Room 2.6 (KSP) |
| Fechas | `java.time` |
| Efectos | `RuntimeShader` (AGSL) + `RenderEffect`, sin librerías externas |
| JSON | `org.json` incluido en Android |
| SDK | `minSdk 33` (Android 13), `targetSdk`/`compileSdk 35` |

## Compilar

Requisitos: **JDK 17** y el **Android SDK** con `platforms;android-35` instalado. No hace falta Android Studio.

1. Crea `local.properties` en la raíz con la ruta del SDK:
   ```properties
   sdk.dir=C:/ruta/al/Android/sdk
   ```
2. Compila el APK de debug y corre los tests:
   ```bash
   ./gradlew assembleDebug testDebugUnitTest
   ```
3. El APK queda en `app/build/outputs/apk/debug/app-debug.apk`.

## Instalar

Necesitas **Android 13 o superior**.

- Con el teléfono conectado y la depuración USB activada:
  ```bash
  adb install -r app/build/outputs/apk/debug/app-debug.apk
  ```
- O copia el APK al teléfono, ábrelo y permite "instalar apps de origen desconocido".

## Formato del respaldo

```json
{
  "app": "StudyGrid",
  "version": 1,
  "goals": [
    { "id": 1, "title": "Roadmap Data Engineer", "description": "SQL + Kimball", "color": -13903362 }
  ],
  "logs": [
    { "goalId": 1, "date": "2026-11-25", "intensity": 3, "note": "Capítulo 3 Kimball + Window Functions" }
  ]
}
```

`color` es un entero ARGB; `intensity` va de 0 a 4.

## Estructura

```
app/src/main/java/com/pesa/studygrid/
├── MainActivity.kt            # Tema claro/oscuro (preferencia guardada)
├── data/                      # Room: entidades, DAO, base de datos, repositorio y JSON
└── ui/
    ├── StudyViewModel.kt      # Estado (StateFlow) y cálculo de rachas
    ├── theme/Theme.kt         # Paletas Aero oscura y clara
    ├── components/
    │   ├── Glass.kt           # Shader Liquid Glass, fondo con burbujas, botón gel, diálogos
    │   ├── ContributionGraph.kt
    │   ├── GoalCard.kt
    │   ├── GoalEditorDialog.kt
    │   └── LogEditorDialog.kt
    └── screens/MainScreen.kt
```

## Créditos

- Emojis 3D: [Microsoft Fluent Emoji](https://github.com/microsoft/fluentui-emoji), licencia MIT.
- Foto de fondo: Chris Barbalis en [Unsplash](https://unsplash.com/photos/-nYBR0LFTvQ), licencia de Unsplash.

Los detalles están en [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
