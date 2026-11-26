# Habitum Aqua — Pendientes

App antes llamada StudyGrid (el paquete `com.pesa.studygrid` no cambia).

Lista de trabajo de la auditoría del 2026-11-25. Se resuelven de uno en uno; marca `[x]` al terminar y probar en el teléfono.

## Hay que arreglar

- [x] **1. Los datos se suben a la nube por Auto Backup**
  - Dónde: [app/src/main/AndroidManifest.xml](app/src/main/AndroidManifest.xml) (`android:allowBackup="true"`)
  - Problema: con el respaldo de Google activado, Android copia la base de datos a Google Drive. Va contra el requisito de "sin nube" y contradice el README.
  - Arreglo: `android:allowBackup="false"`. El respaldo JSON manual no cambia.

## Conviene arreglar

- [x] **2. Las burbujas redibujan todo 120 veces por segundo**
  - Dónde: [Glass.kt](app/src/main/java/com/pesa/studygrid/ui/components/Glass.kt) → `AeroBackground`
  - Problema: a 120 Hz se repinta la foto, las burbujas y el desenfoque con shader de cada tarjeta, aunque el movimiento por cuadro es menor a 1 px. Gasta batería.
  - Arreglo: actualizar `time` solo cada ~33 ms (30 fps).

- [x] **3. Se instala la versión debug (lenta, 23 MB)**
  - Dónde: [app/build.gradle.kts](app/build.gradle.kts)
  - Problema: Compose en debug corre más lento y el APK pesa más.
  - Arreglo: build `release` con R8 (`isMinifyEnabled` + `isShrinkResources`) y una llave de firma local fuera de git (`.gitignore`). Medir tamaño y fluidez antes y después.

- [x] **4. Texto blanco ilegible sobre coral y azul cielo**
  - Dónde: [Glass.kt](app/src/main/java/com/pesa/studygrid/ui/components/Glass.kt) → `GelButton`
  - Problema: blanco sobre `#FF8A65` tiene contraste 2.3:1 y sobre `#4FA3FF` 2.6:1; el mínimo es 4.5:1.
  - Arreglo: elegir siempre el color de texto (claro u oscuro) que dé más contraste con el fondo.

- [x] **5. "Guardar" sin título no hace nada**
  - Dónde: [GoalEditorDialog.kt](app/src/main/java/com/pesa/studygrid/ui/components/GoalEditorDialog.kt) → botón Guardar
  - Problema: con el título vacío, el toque se ignora sin avisar.
  - Arreglo: `enabled = title.isNotBlank()` en el `GelButton`.

- [x] **6. Los cuadritos (14dp) son muy chicos para el dedo**
  - Dónde: [ContributionGraph.kt](app/src/main/java/com/pesa/studygrid/ui/components/ContributionGraph.kt), [LogEditorDialog.kt](app/src/main/java/com/pesa/studygrid/ui/components/LogEditorDialog.kt)
  - Problema: es fácil tocar el día de al lado y registrar la fecha equivocada.
  - Arreglo: flechas ‹ › en el diálogo del día para moverse al día anterior o siguiente (sin pasar de hoy).

- [x] **7. TalkBack no anuncia qué nivel o color está elegido**
  - Dónde: [LogEditorDialog.kt](app/src/main/java/com/pesa/studygrid/ui/components/LogEditorDialog.kt) (chips 0–4), [GoalEditorDialog.kt](app/src/main/java/com/pesa/studygrid/ui/components/GoalEditorDialog.kt) (círculos de color)
  - Arreglo: cambiar `clickable` por `selectable(selected = …, role = Role.RadioButton)`.
  - Hecho: ambos usan `selectable(selected, role = Role.RadioButton)` con `contentDescription` ("Nivel 2, Medio", "Color aqua"). Falta probarlo con TalkBack en el teléfono.

## Estaría bien

- [x] **8. "Hoy" no se distingue en el mapa de calor**
  - Dónde: [ContributionGraph.kt](app/src/main/java/com/pesa/studygrid/ui/components/ContributionGraph.kt)
  - Arreglo: borde brillante en la celda de hoy.

- [x] **9. La pantalla vacía no tiene el marco cromado**
  - Dónde: [MainScreen.kt](app/src/main/java/com/pesa/studygrid/ui/screens/MainScreen.kt) → `EmptyState`
  - Arreglo: usar el mismo `chromeBezel` que las tarjetas.
  - Hecho: `chromeBezel` se reemplazó por `GlassFrame.kt`; `EmptyState` ya usa `glass().glassRim(24.dp)` como las tarjetas.

- [x] **10. Colores del tema sin uso**
  - Dónde: [Theme.kt](app/src/main/java/com/pesa/studygrid/ui/theme/Theme.kt) → `borderTop`, `borderBottom`
  - Arreglo: borrarlos (6 líneas).

- [x] **11. Parámetros que nadie cambia**
  - Dónde: [Glass.kt](app/src/main/java/com/pesa/studygrid/ui/components/Glass.kt) → `GelButton(shape)`, `glass(tint, blur, bevel, depth)`
  - Arreglo: convertirlos en constantes (~8 líneas menos).

## Pendientes de diseño

- [x] **12. Marco cromado más grueso**: bisel de ~12dp en dos tonos de metal, tornillos y esquinas recortadas en ángulo, al estilo del skin de referencia.
- [x] **13. El botón + tapa el "+1 Hoy" de la última tarjeta**: más espacio al final de la lista.
- [x] **14. Fondo de día con tus tonos verde y azul**: necesita una foto de Unsplash o Pexels (río turquesa, agua con piedras o canicas). Las fotos del moodboard no tienen licencia.

## Funciones nuevas

- [x] **15. Deshacer**: aviso "Deshacer" de unos segundos después de cambiar un día o borrar una meta.
- [x] **16. Meta semanal**: objetivo de N días por semana, con barra de progreso en la tarjeta.
- [x] **17. Widget en la pantalla de inicio**: mapa de calor y "+1 Hoy" (usa Jetpack Glance, dependencia nueva).
- [x] **18. Recordatorio diario local**: notificación a la hora que elijas, sin internet (requiere permiso de notificaciones).
- [x] **19. Reordenar metas**: arrastrar para cambiar el orden (requiere guardar el orden en la base de datos, con migración de Room).

## Encontrado al probar 15 y 16

- [x] **20. El nivel 1 casi no se ve en modo día**: un cuadrito con nivel 1 (aqua al 30%) se confunde con uno vacío sobre el panel claro. Subir la opacidad mínima o usar un tono más oscuro en día.
- [x] **21. El aviso de "Deshacer" no usa el estilo de la app**: sale con el estilo por defecto de Material (gris oscuro con botón morado). Darle el marco cromado y los colores del tema.

## Ideas siguientes

- [x] **22. Estadísticas por mes**: días activos y nivel promedio por mes, con una gráfica pequeña (pantalla o sección nueva).
- [x] **23. Elegir la meta de cada widget**: hoy muestra la primera de la lista; agregar una pantalla de configuración al colocar el widget.
- [x] **24. Exportar el mapa de calor como imagen** para compartirlo (renderizar la tarjeta a bitmap y abrir el menú de compartir).
- [x] **25. Ver notas en el mapa**: mantener presionado un día con nota muestra el texto sin abrir el editor.
- [x] **26. Logros**: insignias al llegar a rachas de 7, 30 y 100 días.

## Antes de publicar

- [ ] **27. Llave de firma propia**: la release está firmada con la llave local de pruebas (`signingConfig = debug`). Crear un keystore propio, guardarlo fuera de git y respaldarlo; si se pierde, no se puede actualizar la app.

## Próximo (ver ROADMAP.md)

Detalle, archivos y criterios de aceptación de cada tarea en [ROADMAP.md](ROADMAP.md). Se hacen en orden.

### Fase 0 — Base: Ajustes, motor de temas, accesibilidad
- [ ] **0.1** Pantalla de Ajustes y navegación mínima (M)
- [ ] **0.2** Calendario de temas, lógica pura + tests (S)
- [ ] **0.3** `ThemeSpec` y `StudyGridTheme` con tema (M)
- [ ] **0.4** Fondo y partículas por tema (M)
- [ ] **0.5** Reducir movimiento y reducir transparencia (M)
- [ ] **0.6** Selector de tema en Ajustes (S)

### Fase 1 — Temas de temporada
- [ ] **1.1** Jolly / Navidad, 1–30 dic (M)
- [ ] **1.2** Año Nuevo, 31 dic – 1 ene (M)
- [ ] **1.3** Dark Aero, manual (S)
- [ ] **1.4** Aurora, 2 ene – feb (L)
- [ ] **1.5** Gen X Soft Club y DORFic, manuales (S)
- [ ] **1.6** Eco, mar – abr (M)
- [ ] **1.7** Vectorgarden, may (M)
- [ ] **1.8** Aqua, jun – ago (L)
- [ ] **1.9** Halloween, 25 – 31 oct (M)
- [ ] **1.10** Día de Muertos, 1 – 2 nov (M)
- [ ] **1.11** El widget sigue el tema (S)

### Fase 2 — Rachas flexibles y celebración
- [ ] **2.1** Rachas con frecuencia, lógica pura (M)
- [ ] **2.2** Comodines de racha, lógica pura (S)
- [ ] **2.3** Migración Room v4 + editor de frecuencia (M)
- [ ] **2.4** Mostrar comodines y días no programados (S)
- [ ] **2.5** Celebración "pop" al dar +1 (M)
- [ ] **2.6** Sonidos sintetizados, apagados por defecto (M)

### Fase 3 — Acuario MVP
- [ ] **3.1** Economía, lógica pura + tests (M)
- [ ] **3.2** Room v5: tablas del juego (M)
- [ ] **3.3** Premios desde el registro de hábitos (M)
- [ ] **3.4** Respaldo JSON v2 (M)
- [ ] **3.5** Arte de los peces (M)
- [ ] **3.6** Pantalla del acuario (L)
- [ ] **3.7** Cuidado: alimentar y gotas (M)

### Fase 4 — Tienda y cosméticos
- [ ] **4.1** Catálogo y compra (S)
- [ ] **4.2** Diálogo de tienda (M)
- [ ] **4.3** Temas desbloqueables (S)
- [ ] **4.4** Decoraciones del acuario (M)
- [ ] **4.5** Paquetes de sonido (S)
- [ ] **4.6** Íconos alternativos de la app (M)

### Fase 5 — Utilidades
- [ ] **5.1** Migración Room v6: archivar, recordatorio por meta, minutos (M)
- [ ] **5.2** Archivar metas (S)
- [ ] **5.3** Recordatorio por meta (M)
- [ ] **5.4** Vista anual (M)
- [ ] **5.5** Temporizador de enfoque, lógica pura (S)
- [ ] **5.6** Burbuja de enfoque, UI + notificación (L)
- [ ] **5.7** Bloqueo con biometría (M)

## Para seguir en otra computadora

1. Clonar: `git clone https://github.com/Pesa12378/StudyGrid.git`
2. Instalar **JDK 17** y el **Android SDK** con `platforms;android-35` (no hace falta Android Studio).
3. Crear `local.properties` con `sdk.dir=<ruta al SDK>` (no se sube a git).
4. `./gradlew testDebugUnitTest assembleRelease` → `app/build/outputs/apk/release/app-release.apk`.
5. Datos de prueba: exportar un respaldo desde la app y correr
   `python tools/demo_fill.py respaldo.json historial.json 2026-07-01 <hoy>`; luego importar `historial.json`.
