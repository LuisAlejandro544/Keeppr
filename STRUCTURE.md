# Estructura del Proyecto (STRUCTURE.md)

Este documento detalla el árbol de directorios, la organización de módulos y la responsabilidad de cada componente en la arquitectura de VaultNotes.

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
│       │   │   │   │   └── AppDatabase.kt # Base de datos Room v2 (migración MIGRATION_1_2 para fontTheme) y DAOs
│       │   │   │   ├── dao/
│       │   │   │   │   └── NoteDao.kt     # Operaciones CRUD y consultas reactivas con Flow
│       │   │   │   ├── model/
│       │   │   │   │   └── Note.kt        # Entidad Note (id, título, contenido, etiquetas, fecha, fijado, fontTheme)
│       │   │   │   └── repository/
│       │   │   │       └── NoteRepository.kt # Abstracción del acceso a datos
│       │   │   ├── native/
│       │   │   │   └── NativeEngine.kt    # Fachada Kotlin para invocar métodos JNI (Rust, C++, Lua)
│       │   │   ├── ui/                    # Capa de Presentación (Jetpack Compose)
│       │   │   │   ├── components/
│       │   │   │   │   ├── EmojiPickerDialog.kt    # Selector de iconos/emojis para notas
│       │   │   │   │   ├── MarkdownToolbar.kt      # Barra de herramientas Markdown móvil (con botón 'Aa Fuente')
│       │   │   │   │   └── FontSelectionDialog.kt  # Selector de tipografías con alcance dual (toda la nota vs selección)
│       │   │   │   ├── markdown/
│       │   │   │   │   ├── MarkdownParser.kt       # Parser offline de bloques y checkboxes estilo Notion
│       │   │   │   │   └── MarkdownPreview.kt      # Renderizado interactivo con fuentes base y etiquetas [font:id]
│       │   │   │   ├── navigation/
│       │   │   │   │   └── AppNavigation.kt # Grafo de navegación y rutas de pantalla
│       │   │   │   ├── screens/
│       │   │   │   │   ├── NotesListScreen.kt  # Pantalla principal: lista (tarjetas con fuente por nota), búsqueda y motor
│       │   │   │   │   └── NoteEditorScreen.kt # Editor con soporte de tipografía por nota y etiquetas de fragmento
│       │   │   │   ├── theme/
│       │   │   │   │   ├── Color.kt, Theme.kt, Type.kt # Paleta M3 y definiciones de AppFontTheme (5 familias de fuentes)
│       │   │   │   └── viewmodel/
│       │   │   │       └── NotesViewModel.kt   # ViewModel para gestión reactiva de notas y tipografía por nota
│       │   └── res/                       # Recursos Android
│       │       ├── values/                # strings.xml, colors.xml, themes.xml
│       │       ├── mipmap-*/              # Iconos adaptativos de la aplicación
│       │       └── drawable/              # Vectores gráficos y fondos
│       └── test/                          # Pruebas Unitarias en JVM
│           └── java/com/example/
│               └── ExampleRobolectricTest.kt # Pruebas funcionales de componentes
├── gradle/
│   └── libs.versions.toml                 # Catálogo de versiones de dependencias (Compose, Room, KSP, AndroidX)
├── build.gradle.kts                       # Configuración raíz de Gradle
├── settings.gradle.kts                    # Registro de módulos y repositorios Maven
├── .github/workflows/                     # Flujos automatizados CI/CD (build-debug, override-commit)
├── setup_debug_keystore.sh                # Generador forzado y limpio de debug.keystore sin dependencias
├── clean_native_artifacts.sh              # Script Bash para purgar artefactos de compilación (target, .cxx, etc.)
├── README.md                              # Documentación general y guía de arranque
├── ROADMAP.md                             # Hoja de ruta técnica
├── STRUCTURE.md                           # Este archivo: desglose estructural del proyecto
├── AI_CONTEXT.md                          # Contexto técnico y operativo para agentes de IA
└── AGENTS.md                              # Reglas y restricciones estrictas para asistentes automáticos
```

---

## 🧩 Responsabilidades de los Módulos

### 1. Capa Nativa (`app/src/main/cpp` y `app/src/main/rust`)
- **`CMakeLists.txt`**: Orquesta la compilación cruzada para las arquitecturas `arm64-v8a` (64 bits), `armeabi-v7a` (32 bits / Android Go) y `x86_64` (emuladores). Compila el código oficial de Lua 5.4.6 como librería estática en C11 (`liblua_static.a`) y enlaza el runtime de Rust precompilado por la tarea `cargoBuild` de Gradle.
- **`native-bridge.cpp`**: Punto de contacto JNI (`Java_com_example_native_NativeEngine_*`). Inicializa y destruye estados de Lua (`lua_State`), evalúa scripts de usuario capturando salidas con `lua_pcall`, e invoca las funciones exportadas por Rust.
- **`lib.rs` (Rust)**: Funciones seguras de cálculo, hashing y criptografía expuestas con firmas ABI C (`#[no_mangle] pub extern "C"`). Incluye el motor acelerador de procesamiento de texto: cálculo de métricas de lectura y snippets de Markdown en una pasada nativa (`rust_process_note_summary`), y motor de búsqueda insensible a mayúsculas (`rust_match_note`).

### 2. Capa de Datos (`app/src/main/java/com/example/data`)
- **Room Database**: Utiliza SQLite embebido sin dependencias en la nube.
- **DAO & Entidades**: Diseñadas para operaciones transaccionales rápidas mediante Kotlin Coroutines (`suspend`) y observación en tiempo real con `Flow`.

### 3. Capa de Presentación (`app/src/main/java/com/example/ui`)
- **Jetpack Compose + Material 3**: UI declarativa, soporte para modo oscuro/claro, animaciones fluidas y accesibilidad táctil con áreas mínimas de 48dp.
- **Sistema Tipográfico Granular**: 5 familias de fuentes nativas seleccionables de forma individual por nota o por fragmento de texto (`[font:id]...[/font]`) sin alterar la aplicación globalmente, con persistencia en base de datos Room.
- **`NotesViewModel`**: Maneja el estado de la UI (`StateFlow`) desacoplado del ciclo de vida de la actividad.

---

## 🛡️ Criterio de Extensión y Control de Código (Filosofía Comunitaria)

Para evitar inflar la base de código con dependencias o características superfluas:
1. **Validación de funcionalidades:** Ninguna función nueva se integra al código base a menos que haya sido propuesta y validada por la comunidad como una necesidad real y transversal.
2. **Modularidad aislada:** Componentes visuales como selectores de diálogo o herramientas de edición se construyen en `ui/components/` como piezas independientes sin dependencias circulares.
3. **Canal de scripting:** Funciones ultra-específicas de formato o manipulación de datos se delegan al motor Lua embebido sin necesidad de modificar el código nativo ni inflar el tamaño de la aplicación.
