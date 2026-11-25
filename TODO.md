# StudyGrid — Pendientes

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

- [ ] **7. TalkBack no anuncia qué nivel o color está elegido**
  - Dónde: [LogEditorDialog.kt](app/src/main/java/com/pesa/studygrid/ui/components/LogEditorDialog.kt) (chips 0–4), [GoalEditorDialog.kt](app/src/main/java/com/pesa/studygrid/ui/components/GoalEditorDialog.kt) (círculos de color)
  - Arreglo: cambiar `clickable` por `selectable(selected = …, role = Role.RadioButton)`.

## Estaría bien

- [x] **8. "Hoy" no se distingue en el mapa de calor**
  - Dónde: [ContributionGraph.kt](app/src/main/java/com/pesa/studygrid/ui/components/ContributionGraph.kt)
  - Arreglo: borde brillante en la celda de hoy.

- [ ] **9. La pantalla vacía no tiene el marco cromado**
  - Dónde: [MainScreen.kt](app/src/main/java/com/pesa/studygrid/ui/screens/MainScreen.kt) → `EmptyState`
  - Arreglo: usar el mismo `chromeBezel` que las tarjetas.

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
