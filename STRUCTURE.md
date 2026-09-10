# Estructura del Proyecto (STRUCTURE.md)

Este documento detalla el árbol de directorios, la organización de módulos y la responsabilidad de cada componente en la arquitectura de Keeppr (anteriormente VaultNotes).

---

## 🌳 Árbol de Directorios

```text
.
├── app/
│   ├── build.gradle.kts                   # Configuración del módulo: SDKs, dependencias, tareas de Cargo y CMake
│   ├── proguard-rules.pro                 # Reglas de ofuscación y conservación de símbolos JNI
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml        # Manifiesto de Android con configuración de componentes y permisos
│       │   ├── cpp/                       # Capa Nativa en C / C++
│       │   │   ├── CMakeLists.txt         # Guión de CMake para compilar Lua estático y libvaultnotes_native.so
│       │   │   ├── native-bridge.cpp      # Implementación JNI en C++17 conectando Kotlin, Rust y Lua
│       │   │   └── lua/                   # Código fuente C oficial de Lua 5.4.6 (sin dependencias externas)
│       │   │       ├── lua.h, luaconf.h, lualib.h, lauxlib.h
│       │   │       ├── lapi.c, lbaselib.c, lcode.c, lcorolib.c... (32 archivos fuente en C)
│       │   ├── rust/                      # Capa Nativa en Rust
│       │   │   ├── Cargo.toml             # Manifiesto de Rust con configuración de crate staticlib
│       │   │   └── src/
│       │   │       └── lib.rs             # Funciones nativas de Rust expuestas vía extern "C"
│       │   ├── java/com/example/          # Código fuente en Kotlin (Capa de Aplicación Android)
│       │   │   ├── MainActivity.kt        # Actividad principal y punto de entrada de la UI
│       │   │   ├── NotesApplication.kt    # Clase Application que inicializa la base de datos Room
│       │   │   ├── data/                  # Capa de Persistencia Local (Room)
│       │   │   │   ├── database/
│       │   │   │   │   └── AppDatabase.kt # Base de datos Room v3 (migraciones MIGRATION_1_2 y MIGRATION_2_3 para isEncrypted)
│       │   │   │   ├── dao/
│       │   │   │   │   └── NoteDao.kt     # Operaciones CRUD y consultas reactivas con Flow
│       │   │   │   ├── model/
│       │   │   │   │   └── Note.kt        # Entidad Note (id, título, contenido, etiquetas, fecha, fijado, fontTheme, isEncrypted)
│       │   │   │   └── repository/
│       │   │   │       └── NoteRepository.kt # Abstracción del acceso a datos
│       │   │   ├── native/
│       │   │   │   └── NativeEngine.kt    # Fachada Kotlin para invocar métodos JNI (Rust, C++, Lua, Criptografía AES/PBKDF2)
│       │   │   ├── updater/               # Sistema de Actualizaciones Desacoplado
│       │   │   │   └── AppUpdateManager.kt # Gestor reactivo de comprobación, descarga directa de APKs e instalación in-app
│       │   │   ├── debug/                 # Módulo de Monitoreo y Diagnóstico en Vivo
│       │   │   │   ├── PerformanceMonitor.kt # Monitor en tiempo real de FPS (Choreographer), RAM (JVM y Heap Nativo) e hilos
│       │   │   │   └── InAppLogCollector.kt  # Colector en tiempo real de logs del proceso (Logcat) con filtros
│       │   │   ├── ui/                    # Capa de Presentación (Jetpack Compose)
│       │   │   │   ├── components/
│       │   │   │   │   ├── EmojiPickerDialog.kt    # Selector de iconos/emojis para notas
│       │   │   │   │   ├── MarkdownToolbar.kt      # Barra de herramientas Markdown móvil (con botones 'Aa Fuente' y '🪄 Lua')
│       │   │   │   │   ├── FontSelectionDialog.kt  # Selector de tipografías con alcance dual (toda la nota vs selección)
│       │   │   │   │   ├── LuaScriptDialog.kt      # Diálogo interactivo para ejecutar scripts/plantillas Lua (Plantilla Diaria y Script Personalizado)
│       │   │   │   │   ├── SettingsDialog.kt       # Diálogo modal de Ajustes: Modo Claro/Oscuro/Sistema, Material You, Acentos y Fuentes
│       │   │   │   │   └── EncryptionDialogs.kt    # Diálogos modales: EncryptNoteDialog, DecryptNoteDialog, RemoveEncryptionDialog
│       │   │   │   ├── debug/                 # Componentes Visuales de Depuración y Rendimiento
│       │   │   │   │   ├── PerformanceFloatingHud.kt # HUD / Overlay flotante arrastrable sobre toda la app
│       │   │   │   │   └── DebugDashboardDialog.kt   # Diálogo / Dashboard completo con pestañas de Rendimiento, Hilos, Logs y Sistema
│       │   │   │   ├── markdown/
│       │   │   │   │   ├── MarkdownParser.kt       # Parser offline de bloques y checkboxes estilo Notion
│       │   │   │   │   └── MarkdownPreview.kt      # Renderizado interactivo con fuentes base y etiquetas [font:id]
│       │   │   │   ├── navigation/
│       │   │   │   │   └── AppNavigation.kt # Grafo de navegación y rutas de pantalla
│       │   │   │   ├── screens/
│       │   │   │   │   ├── NotesListScreen.kt  # Pantalla principal: lista (tarjetas con fuente y eliminación con confirmación), búsqueda, ajustes y motor
│       │   │   │   │   └── NoteEditorScreen.kt # Editor con soporte de tipografía por nota, etiquetas de fragmento y eliminación con confirmación
│       │   │   │   ├── theme/
│       │   │   │   │   ├── Color.kt, Theme.kt, Type.kt # Paleta M3 y definiciones de AppFontTheme (5 familias de fuentes)
│       │   │   │   │   └── ThemePreferences.kt # Modelos para AppThemeMode (Sistema/Claro/Oscuro) y AppAccentPalette (paletas de acento)
│       │   │   │   └── viewmodel/
│       │   │   │       └── NotesViewModel.kt   # ViewModel para gestión reactiva de notas, tipografía y preferencias de tema visual
│       │   └── res/                       # Recursos Android
│       │       ├── values/                # strings.xml, colors.xml, themes.xml
│       │       ├── mipmap-*/              # Iconos adaptativos de la aplicación
│       │       └── drawable/              # Vectores gráficos, fondos e ic_keeppr_logo.webp
│       └── test/                          # Pruebas Unitarias en JVM
│           └── java/com/example/
│               └── ExampleRobolectricTest.kt # Pruebas funcionales de componentes
├── gradle/
│   └── libs.versions.toml                 # Catálogo de versiones de dependencias (Compose, Room, KSP, AndroidX)
├── build.gradle.kts                       # Configuración raíz de Gradle
├── settings.gradle.kts                    # Registro de módulos y repositorios Maven
├── .github/workflows/                     # Flujos automatizados CI/CD (build-debug, build-release, process-changelog-beta, override-commit)
├── updater_config.lua                     # Script oficial de Lua 5.4 con URLs, endpoints y filtrado de pre-releases beta
├── DISCORD_CONFIG.md                      # Estructura de roles y configuración de la comunidad de Discord
├── Changelog-beta.md                      # Registro de capacidades y novedades de la versión Beta v0.1.0-b
├── commit_message.txt                     # Mensaje de confirmación sincronizado en español
├── setup_debug_keystore.sh                # Generador forzado y limpio de debug.keystore sin dependencias
├── clean_native_artifacts.sh              # Script Bash para purgar artefactos de compilación (target, .cxx, etc.)
├── convert_jpg_to_webp.sh                 # Script Bash utilitario para convertir imágenes JPG a WebP a máxima compresión
├── README.md                              # Documentación general y guía de arranque
├── ROADMAP.md                             # Hoja de ruta técnica
├── STRUCTURE.md                           # Este archivo: desglose estructural del proyecto
├── AI_CONTEXT.md                          # Contexto técnico y operativo para agentes de IA
└── AGENTS.md                              # Reglas y restricciones estrictas para asistentes automáticos
```

---

## 🧩 Responsabilidades de los Módulos

### 1. Capa Nativa (`app/src/main/cpp` y `app/src/main/rust`)
- **`CMakeLists.txt`**: Orquesta la compilación cruzada para las arquitecturas `arm64-v8a` (64 bits), `armeabi-v7a` (32 bits / Android Go) y `x86_64` (emuladores). Compila el código oficial de Lua 5.4.6 como librería estática en C11 (`liblua_static.a`), integra las funciones C/Rust e incluye el runtime de Rust precompilado por la tarea `cargoBuild` de Gradle.
- **`native-bridge.cpp`**: Punto de contacto JNI (`Java_com_example_native_NativeEngine_*`). Inicializa y destruye estados de Lua (`lua_State`), evalúa scripts de usuario capturando salidas con `lua_pcall`, inyecta variables globales de contexto (`content` y `title` vía `executeLuaWithContext`), enlaza métodos criptográficos (`encryptNoteInRust`, `decryptNoteInRust`, `isEncryptedPayloadInRust`) e invoca las funciones exportadas por Rust.
- **`lib.rs` (Rust) & `rust_shim.c`**: Funciones seguras de cálculo, hashing y criptografía expuestas con firmas ABI C (`#[no_mangle] pub extern "C"`). Incluye:
  * Motor criptográfico offline: cifrado simétrico AES-256-CBC con padding PKCS#7, derivación de clave PBKDF2-HMAC-SHA256 (10,000 rondas), código MAC HMAC-SHA256 y serialización `VAULT_ENC_V1$<salt>$<iv>$<ciphertext>$<mac>`.
  * Motor acelerador de procesamiento de texto: cálculo de métricas de lectura y snippets de Markdown en una pasada nativa (`rust_process_note_summary`).
  * Motor de búsqueda insensible a mayúsculas y acentos (`rust_match_note`).

### 2. Capa de Datos (`app/src/main/java/com/example/data`)
- **Room Database (`AppDatabase.kt`)**: Base de datos SQLite v3 con migraciones incrementales seguras: `MIGRATION_1_2` (soporte de tipografía por nota) y `MIGRATION_2_3` (añade columna `isEncrypted` a tabla `notes`).
- **DAO & Entidades**: Diseñadas para operaciones transaccionales rápidas mediante Kotlin Coroutines (`suspend`) y observación en tiempo real con `Flow`. La entidad `Note` integra el indicador `isEncrypted`.
- **`VaultPackageHelper`**: Motor de serialización, empaquetado y desempaquetado de notas. Procesa la exportación e importación dual (Markdown plano `.md` y paquetes comprimidos `.zip` que integran `note.md`, metadatos en `vault_meta.json` y firma de autenticidad criptográfica `signature.vault` verificada mediante SHA-256 nativo).

### 3. Capa de Presentación (`app/src/main/java/com/example/ui`)
- **Jetpack Compose + Material 3**: UI declarativa, soporte completo para modo oscuro, claro y seguimiento del sistema, integración dinámica con Material You en Android 12+, paletas de acento personalizadas (Obsidian, Esmeralda, Ámbar, Azul Zafiro, Rosa Neón), animaciones fluidas y accesibilidad táctil con áreas mínimas de 48dp.
- **Sistema de Protección Criptográfica en UI (`EncryptionDialogs.kt`)**:
  * `EncryptNoteDialog`: Diálogo modal de protección con validación de contraseña y confirmación.
  * `DecryptNoteDialog`: Diálogo de desbloqueo modal interactivo con gestión de errores al ingresar contraseñas incorrectas.
  * `RemoveEncryptionDialog`: Diálogo de confirmación para retirar el cifrado y volver a guardar la nota en texto claro.
- **Sistema Tipográfico Granular**: 5 familias de fuentes nativas seleccionables de forma individual por nota o por fragmento de texto (`[font:id]...[/font]`) y configuración de tipografía global predeterminada desde el diálogo de ajustes, con persistencia en base de datos Room y SharedPreferences.
- **`NotesViewModel`**: Maneja el estado de la UI (`StateFlow`) desacoplado del ciclo de vida de la actividad, persistiendo en tiempo real temas visuales, modo de vista y consultas. Administra la sesión efímera en memoria de la contraseña activa (`activeNoteSessionPassword`), realiza cifrado/descifrado transparente al persistir o desbloquear notas, y excluye notas protegidas de las búsquedas de texto.

### 4. Capa de Diagnóstico y Depuración en Tiempo Real (`app/src/main/java/com/example/debug` y `ui/debug`)
- **Aislamiento Condicional y Exclusividad Canary (`v0.1.0-dev`):** Esta suite de depuración opera **exclusivamente en la variante Debug / Canary**. En el APK Release Beta (`v0.1.0-b`), todos los monitores de fondo, el HUD flotante, el panel de control y los accesos visuales se desactivan y suprimen por completo mediante `BuildConfig.DEBUG`, eliminando cualquier sobrecarga de rendimiento o interfaz de desarrollo para los usuarios finales.
- **`PerformanceMonitor`**: Muestrea FPS mediante `Choreographer`, calcula memoria JVM libre/usada y rastrea el uso de memoria nativa C++/Rust mediante `Debug.getNativeHeapAllocatedSize()` (activo solo en debug).
- **`InAppLogCollector`**: Captura en tiempo real la salida de Logcat del proceso para auditar eventos sin conectar el teléfono a un PC (activo solo en debug).
- **`PerformanceFloatingHud`**: Overlay arrastrable con respuesta háptica para visualización continua de FPS, memoria e hilos sobre cualquier vista de la app (exclusivo debug).
- **`DebugDashboardDialog`**: Panel modal con 4 pestañas interactivas: métricas de rendimiento y GC manual, visor e inspector de hilos activos, consola de logs en vivo y estado del hardware y enlaces nativos (exclusivo debug).
- **LeakCanary (v2.14)**: Detección automatizada de fugas de memoria configurada en Gradle mediante `debugImplementation`, quedando 100% fuera del empaquetado de Release Beta.
- **Pureza de Dependencias y Nomenclatura:** El proyecto está completamente libre de dependencias de Google Play Services y rastreadores de Firebase, garantizando que el APK sea distribuible en Uptodown y tiendas libres con absoluta soberanía y privacidad. En Release Beta se descartan emuladores de PC (`x86_64` y `x86`), generando el archivo oficial `Keeppr-v0.1.0-b-Release.apk` optimizado para procesadores móviles (`arm64-v8a` y `armeabi-v7a`).
- **Optimización Agresiva con R8 Full Mode y ProGuard (`app/proguard-rules.pro`, `gradle.properties`):** Minificación activada con reducción de recursos (`isShrinkResources = true`), modo completo `android.enableR8.fullMode=true`, poda de librerías no utilizadas (Retrofit, Moshi), eliminación de más de 75 idiomas no soportados en `resources.arsc` (`resourceConfigurations = ["es", "en"]`), exclusión de metadatos `META-INF/*.kotlin_module`, eliminación de comprobaciones repetitivas de Kotlin (`Intrinsics`) y retiro de reglas sobreprotectoras de Compose, logrando un APK ultra-compacto y reduciendo la huella de código compilado de Android ART (`base.odex`). Además, con `android:extractNativeLibs="false"` y `useLegacyPackaging = false`, Android no descomprime librerías `.so` en disco, ahorrando entre 3 y 5 MB en el almacenamiento interno del usuario al instalarse en 32 y 64 bits. Preserva 100% intactos los enlaces JNI nativos de Rust/C++ (`com.example.native.NativeEngine`) y las entidades de Room.
- **Experiencia Inicial Limpia (`AppDatabase.kt`):** Se erradicaron notas de prueba pre-hechas, inicializando la base de datos local con una única nota oficial de bienvenida que explica detalladamente las funciones disponibles y el uso de Keeppr.

### 5. Capa de Actualizaciones Desacopladas (`app/src/main/java/com/example/updater` y `updater_config.lua`)
- **`updater_config.lua`**: Script oficial en Lua 5.4 desacoplado del binario Kotlin. Define repositorios, endpoints de la API de GitHub Releases (`LuisAlejandro544/Keeppr`) y la función nativa `filter_beta_release(tag_name, is_prerelease)` para aceptar estrictamente pre-releases beta con sufijo `-b`.
- **`AppUpdateManager.kt`**: Orquesta la consulta de actualizaciones ejecutando el script Lua a través de JNI (`NativeEngine.evalLua`). Permite la actualización dinámica del script desde GitHub Raw sin recompilar el APK.
- **Descarga Directa e Instalación In-App**: Descarga el archivo APK en `cacheDir/updates/` reportando métricas de progreso (MB y porcentaje) e interactúa directamente con el instalador de paquetes de Android vía `FileProvider` y `REQUEST_INSTALL_PACKAGES`, eliminando la necesidad de recurrir al navegador web.

---

## 🛡️ Criterio de Extensión y Control de Código (Filosofía Comunitaria)

Para evitar inflar la base de código con dependencias o características superfluas:
1. **Validación de funcionalidades:** Ninguna función nueva se integra al código base a menos que haya sido propuesta y validada por la comunidad como una necesidad real y transversal.
2. **Modularidad aislada:** Componentes visuales como selectores de diálogo o herramientas de edición se construyen en `ui/components/` como piezas independientes sin dependencias circulares.
3. **Canal de scripting:** Funciones ultra-específicas de formato o manipulación de datos se delegan al motor Lua embebido sin necesidad de modificar el código nativo ni inflar el tamaño de la aplicación.
