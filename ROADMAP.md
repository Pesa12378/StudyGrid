# Habitum Aqua — Roadmap

App: **Habitum Aqua** (antes StudyGrid; el paquete `com.pesa.studygrid`, la base `studygrid.db` y los nombres de clases como `StudyGridTheme`/`StudyGridWidget` no cambian).

Plan de implementación del 2026-11-26. Cada tarea está pensada para **una sola corrida de un agente** (1–3 archivos de cambio enfocado). Se hacen en orden; al terminar una, marcar su casilla en [TODO.md](TODO.md) → "Próximo".

Tamaños: **S** = < 1 h, un archivo · **M** = 2–3 archivos · **L** = 3 archivos con lógica nueva o UI grande.

Convenciones para el agente:
- Lee antes la tarea completa, la sección **Reglas** y los archivos que menciona.
- Toda lógica pura (fechas, rachas, economía, síntesis de audio) va en funciones **sin imports de Android/Compose**, con tests JVM en `app/src/test/...` (JUnit 4, como `StatsAndBackupTest.kt`).
- Verificación mínima por tarea: `./gradlew testDebugUnitTest assembleRelease` en verde + probar en el teléfono lo que diga "Aceptación".
- Si la tarea toca Room: instalar la release **encima** de la versión anterior con datos reales y confirmar que no se pierde nada (no hay `androidTest`; la prueba de migración es manual).

---

## Reglas

1. **100% offline.** Nunca agregar el permiso `INTERNET` ni librerías que lo pidan. Nada de analítica, fuentes descargables ni assets remotos.
2. **Arte original dibujado en código.** Fondos, partículas, peces, decoraciones, íconos y sonidos se hacen con `Canvas`/`Path`/gradientes/AGSL o síntesis de audio. Prohibido copiar fan art, stickers, wallpapers, sprites, sonidos o nombres de juegos comerciales (incluido el juego de "jardín zen" que inspira el acuario). Las únicas imágenes de terceros siguen siendo las ya acreditadas en `THIRD_PARTY_NOTICES.md`.
3. **Tipografía y nombres:** la fuente Frutiger es comercial → no se incluye. "Frutiger" (marca de Monotype) solo se usa para describir la estética en documentos; **nunca** en nombres visibles de temas, la app ni la tienda (los temas se llaman "Aero", "Aurora", "Eco"…). Se usa la sans del sistema (`FontFamily.Default`). Si algún día se quiere una redondeada, solo una con licencia OFL y documentada en `THIRD_PARTY_NOTICES.md`.
4. **Sin dependencias pesadas.** Nada de navigation-compose, Lottie, motores de juego ni librerías de audio. Excepción evaluada: ninguna; el bloqueo biométrico usa el `BiometricPrompt` del framework (API 30+, y `minSdk` es 33), así que **no** hace falta `androidx.biometric`.
5. **Accesibilidad:** objetivos táctiles ≥ 48dp, todo control con `contentDescription`/`Role`, selección con `selectable(role = Role.RadioButton)`, contraste de texto ≥ 4.5:1 (usar `readableTextOn` de `Glass.kt`). Animaciones y transparencia obedecen **Reducir movimiento** y **Reducir transparencia** (Fase 0).
6. **Cero pérdida de datos.** Cada cambio de esquema = migración Room explícita (`MIGRATION_n_m` en `AppDatabase.kt`), nunca `fallbackToDestructiveMigration`. El respaldo JSON incluye **todo** dato nuevo, y los respaldos viejos se siguen pudiendo importar.
7. **Batería:** animaciones a ≤ 30 fps (como `BUBBLE_FRAME_SECONDS`), se pausan fuera de pantalla, y los shaders de pantalla completa se dibujan estáticos con Reducir movimiento.
8. **El estudio va primero.** El juego nunca bloquea, regaña ni castiga; se puede ocultar por completo en Ajustes.

---

## Arquitectura

### Motor de temas

Hoy: `StudyGridTheme(dark)` en `ui/theme/Theme.kt` elige `DarkAero`/`LightAero` (`AeroColors`) y los publica en `LocalAero`; `AeroBackground` en `Glass.kt` pinta fotos + `scrim` + 18 burbujas (`soapBubble`) a 30 fps.

Propuesta (sin tocar a los consumidores de `LocalAero`):

```
ui/theme/Seasons.kt     // PURO: enum ThemeId, seasonalTheme(date), resolveTheme(pref, date)  ← tests
ui/theme/Themes.kt      // ThemeSpec por ThemeId: paletas light/dark (AeroColors), accent, Particle, Backdrop
ui/theme/Theme.kt       // StudyGridTheme(dark, spec) → LocalAero = spec.colors(dark), LocalThemeSpec = spec
ui/components/Glass.kt  // AeroBackground(spec) = backdrop(spec) + drawRect(scrim) + ParticleField(spec.particle)
ui/components/Particles.kt // enum Particle + DrawScope.drawParticle(kind, …) (snow, leaves, petals, orbs…)
ui/theme/Backdrops.kt   // fondos pintados y shaders AGSL (aurora, cáusticas, niebla) por tema
```

```kotlin
enum class ThemeId { AERO, DARK_AERO, AURORA, ECO, VECTORGARDEN, AQUA, HALLOWEEN, MUERTOS, JOLLY, NEW_YEAR, GENX, DORFIC }
enum class Particle { BUBBLES, SNOW, LEAVES, PETALS, BUTTERFLIES, FISH, ORBS, FIREWORKS, STARS, NONE }
sealed interface Backdrop {
    data object Photos : Backdrop                                   // el slideshow actual (solo AERO)
    class Painted(val draw: DrawScope.(time: Float, dark: Boolean) -> Unit) : Backdrop
    class Shader(val agsl: String, val fallback: Painted) : Backdrop // fallback = cuadro estático/reduce motion
}
class ThemeSpec(val id: ThemeId, val label: String, val light: AeroColors, val dark: AeroColors,
                val particle: Particle, val backdrop: Backdrop, val seasonal: Boolean)
```

- **Calendario (México), primera coincidencia gana** — `seasonalTheme(date: LocalDate): ThemeId`:

  | Prioridad | Tema | Fechas |
  |---|---|---|
  | 1 | NEW_YEAR | 31 dic – 1 ene |
  | 2 | MUERTOS | 1 – 2 nov |
  | 3 | HALLOWEEN | 25 – 31 oct |
  | 4 | JOLLY | 1 – 30 dic |
  | 5 | AURORA | 2 ene – fin de feb (29 en bisiesto) |
  | 6 | ECO | 1 mar – 30 abr (incluye 22 abr, Día de la Tierra) |
  | 7 | VECTORGARDEN | 1 – 31 may (10 may, Día de las Madres) |
  | 8 | AQUA | 1 jun – 31 ago |
  | — | AERO | resto del año |

  Solo-manuales: `DARK_AERO`, `GENX`, `DORFIC`.
- **Preferencia** `theme` en `SharedPreferences("settings")`: `"auto"` (default) o un `ThemeId.name`. `resolveTheme(pref, date)` devuelve el tema. El botón sol/luna sigue eligiendo la paleta clara/oscura **del tema activo** (todo tema define ambas).
- **Mapa de calor:** la rampa por meta (`levelColor(level, goalColor, dark)`) **no cambia**; los temas solo cambian paneles, scrim, acento y fondo.
- **"Hoy":** se recalcula al volver a la app (`ON_RESUME`) para que el tema cambie a medianoche sin reiniciar.

### Juego del acuario

```
game/Economy.kt     // PURO: reglas de monedas, topes, ánimo de peces, gotas, llaves de ledger  ← tests
game/Catalog.kt     // PURO: catálogo estático (id estable, tipo, precio, nombre). Los ids NUNCA se renombran.
game/FishArt.kt     // dibujo de especies en DrawScope (parámetros por especie)
data/GameEntities.kt, data/GameDao.kt, data/GameRepository.kt  // Room v5
ui/screens/AquariumScreen.kt, ui/components/ShopDialog.kt
```

- **Moneda: "perlas".** Saldo = `SUM(amount)` del **ledger** (`wallet_ledger`); nunca se guarda un saldo suelto. Cada movimiento tiene una `key` **UNIQUE** → otorgar dos veces lo mismo es imposible (`INSERT OR IGNORE`). Deshacer/editar no devuelve ni re-otorga: la llave ya existe.
- **Se gana (solo por actividad real de HOY):**

  | Evento | Llave | Premio | Tope |
  |---|---|---|---|
  | Un día de una meta pasa de 0 → activo, y es **hoy** | `day:{goalId}:{epochDay}` | 5 perlas | 3 metas/día (15/día) |
  | Meta semanal cumplida en la semana actual | `week:{goalId}:{lunesEpochDay}` | 20 perlas | 3 metas/semana |
  | Insignia de racha 7 / 30 / 100 | `badge:{goalId}:{n}` | pez (tier 1/2/3) + 25/75/200 | una vez por meta y umbral, para siempre |
  | Meta nueva llega a 3 días activos | `newgoal:{goalId}` | 1 pez tier 1 | máx. 1 cada 7 días |
  | Primera vez que abres el acuario | `starter` | 1 pez "Gota" | una vez |
  | Gotas de los peces (tocar para recoger) | `drop:{epochMillis}` | 1 perla c/u | 1 gota/pez cada 6 h, máx. 2 pendientes por pez, 20/día |

  Rellenar días pasados, el temporizador de enfoque o crear muchas metas **no** da perlas extra.
- **Reloj:** las gotas se calculan con `pendingDrops(fish, lastCollectAt, now)`; si `now < lastCollectAt` (reloj atrasado) se reinicia sin premio; el tope de pendientes limita adelantar el reloj. Es un juego de una persona: no hay más anti-trampa que los topes.
- **Cuidado sin culpa:** alimentar = 1 vez al día, gratis, **disponible cuando ya registraste algo hoy** ("la comida se gana estudiando"). Ánimo: `HAPPY` (≤ 1 día sin comer), `OK` (2–3), `SLEEPY` (≥ 4: colores desaturados, nadan lento, gotas a la mitad). **Nunca mueren ni se van.**
- **Precios** (balance estimado: 2 metas diarias + 6 peces ≈ 150–200 perlas/semana):

  | Cosa | Precio |
  |---|---|
  | Decoración | 60 – 150 |
  | Especie de pez | 100 – 250 |
  | Paquete de sonidos | 200 |
  | Ícono de la app | 300 |
  | Tema de temporada fuera de fecha | 400 |

- **Cómo se conecta:** tema desbloqueado → aparece en el selector de Ajustes (`unlock "theme:JOLLY"`); paquete de sonido → `sound_pack` en prefs, lo usa `Sfx`; ícono → `activity-alias`; decoraciones y peces → se dibujan en el acuario.
- **Navegación mínima:** `enum class Screen { MAIN, SETTINGS, AQUARIUM }` en `MainActivity` con `rememberSaveable` + `BackHandler`. Sin navigation-compose.

---

## Opinión de diseño (acuario)

Es una buena idea **si** se mantiene secundario: un premio visual que te recuerda que estudiaste, no una segunda app. Riesgos y cómo los cubre el plan:

- **Crecimiento sin fin (scope creep):** el MVP (Fase 3) es solo peces + perlas + alimentar; la tienda llega después y su catálogo es una lista estática en código. Nada de minijuegos, crías, mercados ni misiones. Si la Fase 3 no motiva en 2–3 semanas de uso, la Fase 4 se recorta.
- **Distracción:** el acuario vive detrás de un botón, no en la pantalla principal; no hay notificaciones del juego; el ánimo "dormido" no regaña; y en Ajustes se puede ocultar todo (los premios se siguen anotando en silencio para no perder nada si lo vuelves a activar).
- **Economía rota o "farmeable":** solo la actividad de **hoy** paga, con topes por día/semana, llaves únicas en el ledger y gotas con tope offline. El ledger es auditable (se puede listar cada perla) y viaja en el respaldo.
- **Trampa a uno mismo:** si las perlas dependieran del nivel 4 o de minutos, darías "+1" de más. Por eso solo cuenta que el día se vuelva activo, no la intensidad.
- **Arte/legal:** todo original y dibujado en código (Regla 2); nombres de especies propios.
- **Costo de mantenimiento:** el juego no toca las tablas de metas/registros (solo las lee); borrarlo algún día = quitar su pantalla y dejar sus tablas.

---

## Por qué este orden

- **Fase 0** primero porque todo tema y todo efecto necesitan la pantalla de Ajustes, el motor de temas y los interruptores de accesibilidad. **Reducir movimiento/transparencia se adelantan** desde la Fase 5: cada tema nuevo con partículas y shaders debe respetarlos desde el día uno.
- **Fase 1** empieza con Navidad y Año Nuevo (hoy es 26 nov; Jolly entra el 1 dic) y deja el resto por calendario.
- **Fase 2** (rachas flexibles y celebración) mejora el hábito antes del juego; además el juego usa las rachas y los sonidos.
- **Fases 3–4**: acuario y tienda, con la economía como módulo puro primero.
- **Fase 5**: utilidades independientes; se pueden intercalar si urgen (por ejemplo el bloqueo).

Versiones de Room: **v4** (Fase 2), **v5** (Fase 3), **v6** (Fase 5). Respaldo JSON: sigue siendo `version: 1` con campos opcionales hasta la Fase 3, donde pasa a **`version: 2`**.

---

## Fase 0 — Base: Ajustes, motor de temas, accesibilidad

### 0.1 Pantalla de Ajustes y navegación mínima — M
- **Meta:** una pantalla de Ajustes de pantalla completa, abierta desde el menú ⋮.
- **Archivos:** `MainActivity.kt` (`enum class Screen`, `rememberSaveable`, `BackHandler`), nuevo `ui/screens/SettingsScreen.kt`, `MainScreen.kt` (entrada "Ajustes" en el `DropdownMenu`). Nuevo `data/Settings.kt`: objeto con las llaves de `SharedPreferences("settings")` (`dark`, `reminder_on`, `reminder_minutes` ya existen) y un `AppSettings` con propiedades `mutableStateOf` que escriben a prefs.
- **Contenido inicial:** secciones vacías "Apariencia", "Accesibilidad", "Sonido", "Juego", "Privacidad", y mover "Recordatorio diario" (sigue abriendo `ReminderDialog`). Exportar/Importar se quedan en ⋮.
- **Datos:** ninguno.
- **Aceptación:** Atrás regresa a la lista; el modo oscuro sigue funcionando igual; títulos de sección con `heading()`; fondo `AeroBackground` también en Ajustes.

### 0.2 Calendario de temas (lógica pura) — S
- **Meta:** `ui/theme/Seasons.kt` con `enum ThemeId`, `seasonalTheme(date)`, `resolveTheme(pref: String, date: LocalDate): ThemeId` (pref inválida → `seasonalTheme`).
- **Archivos:** `ui/theme/Seasons.kt`, `app/src/test/.../ui/theme/SeasonsTest.kt`.
- **Aceptación (tests):** cada frontera de la tabla (24/25 oct, 31 oct/1 nov, 2/3 nov, 30/31 dic, 1/2 ene, 28/29 feb en 2028, 30 abr/1 may, 31 ago/1 sep); `"auto"` y `"JOLLY"` y `"basura"`.

### 0.3 ThemeSpec y StudyGridTheme con tema — M
- **Meta:** `ui/theme/Themes.kt` con `ThemeSpec`, `Particle`, `Backdrop` y `specOf(id)`; por ahora todos los ids devuelven el spec AERO (las paletas actuales `DarkAero`/`LightAero`, `Particle.BUBBLES`, `Backdrop.Photos`). `StudyGridTheme(dark, spec)` publica `LocalThemeSpec`; el `ColorScheme` de Material deriva `primary` del `accent` del spec.
- **Archivos:** `ui/theme/Themes.kt`, `ui/theme/Theme.kt`, `MainActivity.kt` (lee `resolveTheme` y lo recalcula en `ON_RESUME`).
- **Aceptación:** la app se ve **idéntica** a hoy; sin cambios en consumidores de `LocalAero`.

### 0.4 Fondo y partículas por tema — M
- **Meta:** separar `AeroBackground` en: backdrop (fotos | pintado | shader) + `scrim` + `ParticleField(kind)`. Mover `Bubble`/`soapBubble` a `ui/components/Particles.kt` con `DrawScope.drawParticle(kind, …)`; por ahora solo `BUBBLES` y `NONE`. Movimiento genérico por tipo: sube (burbujas), cae (nieve, hojas, pétalos), deriva (orbes, mariposas), estático (estrellas).
- **Archivos:** `ui/components/Glass.kt`, nuevo `ui/components/Particles.kt`.
- **Aceptación:** misma apariencia y 30 fps; el slideshow solo se carga cuando `backdrop is Photos` (otros temas no decodifican JPGs).

### 0.5 Reducir movimiento y reducir transparencia — M
- **Meta:** `LocalA11y(reduceMotion, reduceTransparency)`. `reduceMotion` default = sistema (`ANIMATOR_DURATION_SCALE == 0`, la lógica que hoy está en `AeroBackground`), con interruptor propio en Ajustes. `reduceTransparency`: `Modifier.glass()` dibuja relleno opaco `dialogBg` sin shader ni blur; `scrim` sube a ~0.85; partículas a 50% de opacidad.
- **Archivos:** `data/Settings.kt`, `ui/components/Glass.kt`, `ui/screens/SettingsScreen.kt`.
- **Aceptación:** con ambos activos: fondo quieto, texto sobre paneles opacos legible (contraste ≥ 4.5:1), TalkBack lee los dos switches con estado.

### 0.6 Selector de tema en Ajustes — S
- **Meta:** sección "Apariencia": "Automático (por temporada)" + lista de temas con radio (`selectable(role = RadioButton)`) y mini-vista de su paleta. Por ahora: Automático, Aero, más los solo-manuales a medida que existan. En builds `debug`, además "Vista previa de fecha" (DatePicker) para probar temporadas.
- **Archivos:** `ui/screens/SettingsScreen.kt`, `data/Settings.kt`.
- **Aceptación:** elegir tema cambia la app al instante y se conserva al reiniciar.

---

## Fase 1 — Temas de temporada

Cada tema = 1 tarea: paletas light/dark (`AeroColors` completas, ver hex de referencia), backdrop, partícula, registro en `specOf`. Aceptación común: contraste ≥ 4.5:1 en textos y `muted`; vidrio legible en ambos modos; con Reducir movimiento todo estático; 30 fps; probado con la vista previa de fecha.

### 1.1 Jolly (Navidad, 1–30 dic) — M
- Paleta `#C8102E #0F7B4A #A8DFF5 #F5C542`. Backdrop pintado: cielo invernal (gradiente vertical), colinas nevadas con `Path`. Partícula `SNOW`: copos tipo bokeh (círculos con gradiente radial suave) cayendo con vaivén; 3–4 esferas navideñas de vidrio colgando arriba (círculo + brillo especular, como `soapBubble`).
- **Archivos:** `Themes.kt`, `Backdrops.kt` (nuevo), `Particles.kt`.

### 1.2 Año Nuevo (31 dic – 1 ene) — M
- Paleta aurora (ver 1.4) + dorado `#F5C542`. Backdrop: cielo nocturno con aurora (reutiliza 1.4 si ya existe; si no, gradiente animado simple). Partícula `FIREWORKS`: explosiones de "vidrio" (anillo de 12 gotas brillantes que se expanden y se desvanecen), máx. 2 a la vez.
- **Archivos:** `Themes.kt`, `Backdrops.kt`, `Particles.kt`.

### 1.3 Dark Aero (manual) — S
- Paleta `#0D1117 #1A2332 #00E5FF #FF3D7F`: negro brillante, borde cian neón en `glassRim` (usar `accent` en vez del blanco cuando el tema lo pide). Partícula `BUBBLES` con tinte cian. Variante clara = `LightAero` actual.
- **Archivos:** `Themes.kt`, `GlassFrame.kt` (color de borde desde el spec).

### 1.4 Aurora (2 ene – feb) — L
- Paleta `#0B1F3A #0E3B43 #2EC4B6 #7CF5C8 #8E6CFF #E0B3FF`. Backdrop `Shader`: aurora AGSL (bandas de ruido senoidal sobre azul noche; uniform `time`, `resolution`) con `fallback` pintado. Partícula `STARS` (puntos que parpadean). Variante clara: "amanecer polar" (lila/menta pálidos).
- **Archivos:** `Backdrops.kt`, `Themes.kt`, `Particles.kt`.
- **Aceptación extra:** el shader se pinta a resolución completa ≤ 30 fps sin tirones en el teléfono; si tira cuadros, dibujarlo en un `GraphicsLayer` a media resolución.

### 1.5 Gen X Soft Club y DORFic (manuales) — S
- Gen X: `#5E6B5A #C8B49A #8A7A66 #9E9E9E #E8DDD0`, fondo de gradiente plano apagado, `Particle.NONE`, brillo del vidrio reducido. DORFic ("semana de exámenes"): `#FF7A1A #1F6FB2 #2B2B2B #F4F4F4`, fondo blanco con rejilla tenue, `NONE`, alto contraste.
- **Archivos:** `Themes.kt`, `Backdrops.kt`.

### 1.6 Eco (mar – abr) — M
- `#43A047 #8BC34A #00897B #FFD54F`. Backdrop: cielo + pasto con gradientes (o el slideshow de fotos de pradera ya incluido: `bg_meadow`, `bg_hills`). Partícula `LEAVES`: hoja = `Path` de dos curvas con nervadura, giran al caer.

### 1.7 Vectorgarden (may) — M
- `#FF6F91 #FFC75F #F9F871 #8FD3C1`. Fondo plano con flores vectoriales (círculos + pétalos con `rotate`) en las esquinas. Partículas `PETALS` + `BUTTERFLIES` (alas = 2 elipses que "aletean" escalando X).

### 1.8 Aqua (jun – ago) — L
- `#0288D1 #039BE5 #00BCD4 #80DEEA`. Backdrop `Shader`: cáusticas submarinas AGSL. Partícula `FISH` (siluetas simples) + burbujas. **El shader de cáusticas se reutiliza en el acuario (3.6)**: dejarlo como `const val CAUSTICS_AGSL` público en `Backdrops.kt`.

### 1.9 Halloween (25 – 31 oct) — M
- `#FF7518 #2E1A47 #7BD389 #FFB347`. Fondo: niebla morada (gradientes radiales que se desplazan lento). Partículas `ORBS` (orbes-fantasma translúcidos) + 2–3 calabazas de vidrio estáticas (esfera achatada con gajos y brillo).

### 1.10 Día de Muertos (1 – 2 nov) — M
- `#FF9F1C #E36414` + magenta `#D81B60` / morado `#6A1B9A`. Fondo: atardecer cempasúchil (gradiente). Partícula `PETALS` en naranja. Arriba, guirnalda de papel picado **de vidrio** con diseños geométricos propios (rombos, flores de 6 pétalos, recortes con `Path.op`), meciéndose.

### 1.11 El widget sigue el tema — S
- **Meta:** `WidgetTheme` en `StudyGridWidget.kt` toma `scrim`, `title`, `muted` del tema resuelto (`resolveTheme` + prefs) en vez de `Day`/`Night` fijos; las fotos y los drawables de panel se quedan (el widget no puede usar shaders).
- **Archivos:** `widget/StudyGridWidget.kt`, `MainActivity.kt` (al cambiar tema, `StudyGridWidget().updateAll`, como ya hace con `dark`).
- **Aceptación:** cambiar de tema actualiza el widget; textos legibles en todos los temas.

---

## Fase 2 — Rachas flexibles y celebración

### 2.1 Rachas con frecuencia (lógica pura) — M
- **Meta:** `computeStats(active, today, rule: StreakRule = Daily)`; `sealed interface StreakRule { Daily; Weekdays(mask: Int); PerWeek(n: Int) }`.
  - `Weekdays`: un día no programado faltante **no rompe** ni suma.
  - `PerWeek(n)`: un día faltante no rompe si su semana (lun–dom) llegó a `n` días activos; en la semana actual, mientras todavía se pueda llegar (`días restantes ≥ n − hechos`).
  - La racha cuenta **días activos** (las insignias 7/30/100 siguen siendo días).
- **Archivos:** `ui/StudyViewModel.kt` (o mover a `ui/Streaks.kt` si crece), `StatsAndBackupTest.kt` (+ tests nuevos).
- **Aceptación:** los tests existentes pasan sin cambios (default `Daily`); tests de L-M-V con fines de semana vacíos, de 3×semana con semana actual a medias, y de semana pasada fallida.

### 2.2 Comodines de racha (lógica pura) — S
- **Meta:** se **calculan**, no se guardan: recorriendo el historial, cada 7 días activos seguidos ganan 1 comodín (tope 2); un día programado faltante consume 1 y la racha sigue. `GoalStats` agrega `freezes: Int` (disponibles) y `frozen: Set<LocalDate>` (días salvados). Al ser derivado, deshacer/editar/importar nunca lo desincroniza.
- **Archivos:** mismo archivo que 2.1 + tests.
- **Aceptación (tests):** 7 activos + 1 falta + 3 activos = racha 10, `frozen` = ese día; dos faltas seguidas con 1 comodín rompe; tope 2.

### 2.3 Migración v4 + editor de frecuencia — M
- **Datos:** `GoalEntity` + `streakRule INTEGER NOT NULL DEFAULT 0` (0 diario, 1 días, 2 por semana) y `scheduleDays INTEGER NOT NULL DEFAULT 127` (bit 0 = lunes … bit 6 = domingo). `MIGRATION_3_4`, `version = 4`. Por semana reutiliza `weeklyTarget` como `n`.
- **Respaldo:** campos opcionales en `BackupJson` (default 0/127; validar `scheduleDays in 1..127`), `version` sigue en 1, igual que se hizo con `weeklyTarget`.
- **UI:** `GoalEditorDialog.kt`: "Frecuencia" = Todos los días / Días específicos (chips L M M J V S D, 48dp, `selectable`) / N por semana (usa el selector semanal existente).
- **Archivos:** `Entities.kt`, `AppDatabase.kt`, `StudyRepository.kt` (BackupJson), `GoalEditorDialog.kt`, `StudyViewModel.kt` (pasar la regla a `computeStats`), test de round-trip.
- **Aceptación:** instalar encima con datos → metas iguales; respaldo viejo importa; respaldo nuevo conserva la frecuencia.

### 2.4 Mostrar comodines y días no programados — S
- **Meta:** en `ContributionGraph.kt`, días en `frozen` con borde de "hielo" (línea punteada cian) y días no programados con relleno más tenue; en `GoalCard.kt` un contador "❄ 1" (GelIcon nuevo `Snowflake` en `GelIcon.kt`) con `contentDescription = "1 comodín de racha"`. Leyenda corta en `StatsDialog`.
- **Archivos:** `ContributionGraph.kt`, `GoalCard.kt`, `GelIcon.kt`.

### 2.5 Celebración "pop" al dar +1 — M
- **Meta:** al tocar "+1 Hoy" (y al guardar un día que sube de nivel), una burbuja de vidrio sale del botón y revienta en 6–8 gotitas (≈ 450 ms). Al llegar a una insignia, versión grande centrada. Con Reducir movimiento: solo un destello de 150 ms sin desplazamiento.
- **Archivos:** nuevo `ui/components/Celebration.kt` (overlay con `Animatable`), `GoalCard.kt` (disparo junto al `performHapticFeedback` existente), `MainScreen.kt` (overlay de insignia ante el mensaje de logro).
- **Aceptación:** no bloquea toques; no se acumula si se toca rápido; no se anuncia en TalkBack (decorativo; el snackbar de logro ya informa).

### 2.6 Sonidos sintetizados (apagados por defecto) — M
- **Meta:** `sound/Synth.kt` PURO: `render(tone: ToneSpec, sampleRate = 44_100): ShortArray` (osciladores seno/triángulo + armónicos, envolvente ADSR, glide de tono) — campanitas estilo 2000s hechas aquí. `sound/Sfx.kt`: `AudioTrack` `MODE_STATIC`, `USAGE_ASSISTANCE_SONIFICATION`, buffers en caché; eventos `POP`, `LEVEL_UP`, `BADGE`, `COIN`. `SoundPack(id, tones: Map<Event, List<ToneSpec>>)`; pack inicial "Cristal".
- **Ajustes:** "Sonidos" (off por defecto) en Sonido. Respeta modo silencio (`AudioManager.ringerMode != NORMAL` → no suena).
- **Archivos:** `sound/Synth.kt`, `sound/Sfx.kt`, `SettingsScreen.kt` (+ `app/src/test/.../sound/SynthTest.kt`).
- **Aceptación (tests):** longitud = duración × sampleRate; pico ≤ `Short.MAX_VALUE`; primer y último sample ≈ 0 (sin "clic").

---

## Fase 3 — Acuario MVP

### 3.1 Economía (lógica pura) — M
- **Meta:** `game/Economy.kt` con todas las reglas de la tabla de Arquitectura: llaves (`dayKey`, `weekKey`, `badgeKey`, `newGoalKey`), `dailyActivationReward(alreadyToday: Int): Int`, `pendingDrops(fishLastCollect, now, mood)`, `dropsCollectable(pending, collectedToday)`, `mood(lastFedDay, today)`, `canFeed(lastFedDay, today, anyActiveToday)`, `pickSpecies(key, tier, owned)` (aleatorio determinista con `Random(key.hashCode())`).
- **Archivos:** `game/Economy.kt`, `app/src/test/.../game/EconomyTest.kt`.
- **Aceptación (tests):** tope 15/día; tope de pendientes; reloj hacia atrás = 0 y reinicia; dormido = mitad; `pickSpecies` estable para la misma llave y prefiere especies que no tienes.

### 3.2 Room v5: tablas del juego — M
- **Datos (`MIGRATION_4_5`, `version = 5`):**
  - `wallet_ledger(id INTEGER PK AUTOINCREMENT, key TEXT NOT NULL UNIQUE, amount INTEGER NOT NULL, reason TEXT NOT NULL, epochDay INTEGER NOT NULL, createdAt INTEGER NOT NULL)`
  - `fish(id INTEGER PK AUTOINCREMENT, species TEXT NOT NULL, name TEXT NOT NULL, sourceKey TEXT NOT NULL UNIQUE, acquiredAt INTEGER NOT NULL, lastCollectAt INTEGER NOT NULL)`
  - `unlocks(itemId TEXT PK, acquiredAt INTEGER NOT NULL, slot INTEGER)` — `slot` = posición de una decoración en el acuario (null = guardada).
  - `aquarium_state(id INTEGER PK CHECK(id = 1), lastFedDay INTEGER NOT NULL DEFAULT -1)`
  - Sin claves foráneas a `goals`: el historial del juego sobrevive a borrar metas.
- **Archivos:** `data/GameEntities.kt`, `data/GameDao.kt` (`@Insert(onConflict = IGNORE)` para ledger y peces; `balance(): Flow<Int>` = `SELECT COALESCE(SUM(amount),0)`; `@Transaction buy(item, price)` que inserta `-price` con llave `buy:{itemId}` solo si `balance ≥ price`), `AppDatabase.kt`.
- **Aceptación:** instalar encima de v4 con datos → todo intacto, tablas vacías.

### 3.3 Premios desde el registro de hábitos — M
- **Meta:** `data/GameRepository.kt` con `onDayChanged(goalId, date, before, after, statsBefore, statsAfter)`; llamado desde `StudyViewModel.bumpToday` y `saveLog` (junto a `announceBadge`). Solo si `date == hoy`. Mensaje "+5 perlas" vía `UiMessage` (y sonido `COIN` si está activo) **solo** si el juego está visible.
- **Ajuste:** "Mostrar acuario y perlas" (default on). Apagado: se otorga igual en silencio, se oculta el botón y los mensajes.
- **Archivos:** `data/GameRepository.kt`, `ui/StudyViewModel.kt`, `data/Settings.kt`.
- **Aceptación:** +1 tres veces el mismo día = 5 perlas una vez; deshacer y volver a dar +1 = nada nuevo; editar un día pasado = nada; 4ª meta del día = nada.

### 3.4 Respaldo v2 — M
- **Meta:** `BackupJson` escribe `version: 2` con `"game": { "ledger": [...], "fish": [...], "unlocks": [...], "aquarium": { "lastFedDay": n } }` y `"settings": { "theme": "...", "soundPack": "..." }`. `decode` acepta 1 y 2. **Importar v1 reemplaza metas/registros y deja el juego intacto**; v2 reemplaza todo en una transacción (`replaceAll` extendido o `GameDao.replaceAll`).
- **Validación:** llaves únicas, `amount` en ±10 000, saldo final ≥ 0, `species`/`itemId` conocidos en `Catalog` (desconocidos se descartan con aviso, no rompen la importación).
- **Archivos:** `data/StudyRepository.kt` (BackupJson), `data/GameDao.kt`, `StatsAndBackupTest.kt`.
- **Aceptación (tests):** round-trip v2; importar el JSON v1 del README sigue funcionando; ledger con llave duplicada → rechazo sin cambios. Actualizar la sección "Formato del respaldo" del `README.md`.

### 3.5 Arte de los peces — M
- **Meta:** `game/FishArt.kt`: `DrawScope.drawFish(species, center, size, facingLeft, phase, mood)`; cuerpo (elipse/gota/rombo), cola animada por `phase`, aleta, ojo, brillo especular de vidrio, desaturado si `SLEEPY`. 6 especies propias iniciales en `Catalog.kt`: **Gota** (inicial), **Burbujín**, **Cometa**, **Menta**, **Cristal** (translúcido), **Coral Luna** (tier 3). Nombres y diseños originales.
- **Archivos:** `game/FishArt.kt`, `game/Catalog.kt`.
- **Aceptación:** vista previa (`@Preview` o hoja de prueba en debug) con las 6 especies legibles a 32dp y 96dp.

### 3.6 Pantalla del acuario — L
- **Meta:** `ui/screens/AquariumScreen.kt` (`Screen.AQUARIUM`), abierto con un botón de pez en la barra superior (`GelGlyph.Fish` nuevo en `GelIcon.kt`, `contentDescription = "Acuario"`), oculto si el juego está apagado.
- **Dibujo:** pecera de vidrio (marco `glassRim`), agua con `CAUSTICS_AGSL` de 1.8 (o gradiente si aún no existe / Reducir movimiento), arena, burbujas. Peces con trayectorias senoidales deterministas por id, ≤ 30 fps, máx. 12 visibles (el resto "descansando" en una lista).
- **UI:** chip de saldo ("🫧 120 perlas"), botones `BubbleButton` "Alimentar" y "Tienda" (Tienda deshabilitada hasta Fase 4), lista accesible de peces (nombre, especie, ánimo) para TalkBack. Primera visita: otorga `starter`.
- **Archivos:** `ui/screens/AquariumScreen.kt`, `MainActivity.kt`, `MainScreen.kt`, `GelIcon.kt` (si cabe; si no, separar el glyph a la 3.7).

### 3.7 Cuidado: alimentar y gotas — M
- **Meta:** "Alimentar": si `canFeed` → hojuelas que caen y los peces suben; actualiza `lastFedDay`. Si no hay registro hoy: texto amable "Registra algo hoy para conseguir comida". Gotas: perlas brillantes que aparecen junto a los peces según `pendingDrops`; tocar una (≥ 48dp de área) cobra todas las pendientes en una sola entrada de ledger y reinicia `lastCollectAt`. Renombrar pez (tocar → diálogo).
- **Archivos:** `ui/screens/AquariumScreen.kt`, `data/GameRepository.kt`.
- **Aceptación:** sin comer 4 días → peces dormidos (nunca desaparecen); tope diario de gotas respetado; con TalkBack existe un botón "Recoger N perlas".

---

## Fase 4 — Tienda y cosméticos

### 4.1 Catálogo y compra — S
- **Meta:** completar `game/Catalog.kt` (`CatalogItem(id, kind: FISH|DECOR|THEME|ICON|SOUND, price, label)`) con los precios de Arquitectura y `GameRepository.buy(id)` usando `GameDao.buy`. Comprar un pez crea la fila `fish` con `sourceKey = "buy:{id}"`.
- **Aceptación (tests):** sin saldo → falla sin escribir; comprar dos veces lo mismo → la segunda no cobra (llave única).

### 4.2 Diálogo de tienda — M
- **Meta:** `ui/components/ShopDialog.kt` sobre `GlassDialog`: pestañas por tipo (`GlassTab`), tarjetas con vista previa dibujada (pez con `drawFish`, tema con su paleta, ícono, decoración), precio, estado (comprado / te faltan N). Confirmación con `ConfirmDialog` existente. Habilitar "Tienda" en el acuario.
- **Archivos:** `ShopDialog.kt`, `AquariumScreen.kt`.

### 4.3 Temas desbloqueables — S
- **Meta:** regla de disponibilidad pura `availableThemes(today, unlocked)`: siempre Aero, Dark Aero, Gen X y DORFic (DORFic es funcional, no se cobra); un tema de temporada está disponible si está en temporada **o** tiene `unlock "theme:{ID}"`. El selector de 0.6 usa esta lista. En "Automático" la temporada siempre aplica gratis.
- **Archivos:** `ui/theme/Seasons.kt` (+ tests), `SettingsScreen.kt`.

### 4.4 Decoraciones del acuario — M
- **Meta:** 6 decoraciones dibujadas en código (castillo de vidrio, alga, roca lisa, cofre-burbuja, coral, faro chiquito). En el acuario, modo "Acomodar": tocar una ranura (6 ranuras fijas en la arena) asigna/quita una decoración (`unlocks.slot`).
- **Archivos:** `game/DecorArt.kt`, `AquariumScreen.kt`, `game/Catalog.kt`.

### 4.5 Paquetes de sonido — S
- **Meta:** 3 paquetes extra como `SoundPack` en `sound/Synth.kt` o `Catalog.kt` ("Burbuja", "Chime Y2K", "Retro 8-bit" con onda cuadrada), comprables; selector en Ajustes → Sonido con botón "Probar".
- **Archivos:** `sound/Sfx.kt` (packs), `SettingsScreen.kt`.

### 4.6 Íconos alternativos de la app — M
- **Meta:** quitar el `LAUNCHER` de `MainActivity` y agregar `<activity-alias>` `.IconDefault` (habilitado) + `.IconAurora`, `.IconSunset`, `.IconMono` (deshabilitados), cada uno con su `android:icon` adaptativo en vector (`res/drawable/ic_launcher_fg_*.xml`, dibujos propios). Cambiar = `setComponentEnabledSetting(..., DONT_KILL_APP)` habilitando uno y deshabilitando los demás.
- **Advertencias (mostrar en la UI):** el launcher puede tardar unos segundos; en algunos launchers se borra el acceso directo de la pantalla de inicio y hay que volver a ponerlo; el widget no se ve afectado. Probar que `adb install -r` posterior no deje dos íconos.
- **Archivos:** `AndroidManifest.xml`, drawables nuevos, `SettingsScreen.kt`.

---

## Fase 5 — Utilidades

### 5.1 Migración v6 (archivar, recordatorio por meta, minutos) — M
- **Datos (`MIGRATION_5_6`, `version = 6`):** `goals.archivedAt INTEGER` (epochDay, null = activa), `goals.reminderMinutes INTEGER` (null = sin recordatorio), `daily_logs.focusMinutes INTEGER NOT NULL DEFAULT 0`.
- **Respaldo:** campos opcionales en v2 (defaults null/0).
- **Archivos:** `Entities.kt`, `AppDatabase.kt`, `StudyRepository.kt` + test de round-trip.

### 5.2 Archivar metas — S
- **Meta:** en `GoalEditorDialog`, "Archivar" (sin confirmación, con Deshacer). La lista oculta archivadas; sección plegable "Archivadas (N)" al final para restaurar. Archivadas no cuentan en el recordatorio, el widget ni el tope de perlas.
- **Archivos:** `StudyDao.kt`, `MainScreen.kt`, `GoalEditorDialog.kt`.

### 5.3 Recordatorio por meta — M
- **Meta:** en el editor de meta, "Recordarme a las HH:MM". `Reminder.schedule` programa una alarma por meta (`requestCode = goalId`, misma ventana inexacta) y notifica solo si esa meta no tiene registro hoy. El global se queda.
- **Archivos:** `reminder/Reminder.kt`, `GoalEditorDialog.kt`, `ReminderTest.kt`.

### 5.4 Vista anual — M
- **Meta:** en `StatsDialog`, pestaña "Año": 53 semanas × 7 en celdas pequeñas no interactivas, con flechas de año; reutiliza `levelColor`. Accesible como resumen ("2026: 214 días activos").
- **Archivos:** `ui/components/YearGraph.kt` (nuevo), `StatsDialog.kt`.

### 5.5 Temporizador de enfoque (lógica pura) — S
- **Meta:** `focus/Focus.kt`: `levelForMinutes(m)` (0 → 0, 1–24 → 1, 25–49 → 2, 50–89 → 3, ≥ 90 → 4), `elapsedMinutes(startMillis, nowMillis, pausedMillis)`, y "el nivel solo sube": `max(actual, levelForMinutes(total))`.
- **Archivos:** `focus/Focus.kt` + `FocusTest.kt`.

### 5.6 Burbuja de enfoque (UI + notificación) — L
- **Meta:** desde `GoalCard` (botón "Enfoque"), un diálogo con una burbuja de vidrio que se llena (25/50 min o personalizado). Estado en prefs (`goalId`, `startAt`, `duration`) → sobrevive a cerrar la app. Notificación continua con cuenta regresiva del sistema (`setUsesChronometer(true)` + `setChronometerCountDown(true)`), botón "Terminar". Al final, alarma → notificación "Burbuja lista" y se suman `focusMinutes` a hoy y sube el nivel.
- **Decisión:** **sin servicio en primer plano.** El tiempo se calcula con marcas de reloj, así que no necesita correr; evita el tipo `specialUse` de Android 14 y el permiso extra. Alarma exacta si `canScheduleExactAlarms()`, si no ventana de 1 min. Si en pruebas el aviso final llega tarde, cambiar a FGS `specialUse`.
- **Archivos:** `focus/FocusController.kt`, `ui/components/FocusDialog.kt`, `AndroidManifest.xml` (receiver).
- **Aceptación:** cerrar la app y volver muestra el tiempo correcto; terminar antes registra lo hecho; no da perlas extra (solo la activación normal del día).

### 5.7 Bloqueo con biometría — M
- **Meta:** Ajustes → Privacidad → "Bloquear con huella o PIN". `android.hardware.biometrics.BiometricPrompt` del framework con `setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)` (sin dependencia nueva). Pide al abrir y al volver tras > 30 s en segundo plano; mientras, la app muestra solo el fondo. Opción "Ocultar en recientes" (`FLAG_SECURE`).
- **Advertencia en la UI:** el widget sigue visible en la pantalla de inicio.
- **Archivos:** `MainActivity.kt`, `AndroidManifest.xml` (`USE_BIOMETRIC`), `SettingsScreen.kt`.
- **Aceptación:** sin huella registrada → usa PIN; cancelar no da acceso; nunca se pierde el acceso a los datos (si el dispositivo no tiene bloqueo, el switch se deshabilita con explicación).

---

## Fuera de alcance (por ahora)

Temas extra que no son de temporada, 16 de septiembre, misiones/logros del juego, crías de peces, sincronización, tablets/landscape. Se reconsideran cuando la Fase 4 esté en uso.
