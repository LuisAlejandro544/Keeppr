# VaultNotes (Motor Nativo Híbrido: Kotlin + C++ + Rust + Lua)

Aplicación móvil de gestión segura de notas y apuntes de alto rendimiento, diseñada con persistencia local robusta en Room, interfaz moderna en Jetpack Compose y un motor nativo multinúcleo en C++, Rust y Lua (C puro) compilado para distribución directa en formato APK (Uptodown y tiendas independientes).

---

## 📋 Requisitos Previos y Entorno

- **Sistema Operativo Compatible:** Android 8.0 (Oreo / API 26) o superior (incluyendo ediciones Android Go).
- **Arquitecturas NDK Soportadas:** `arm64-v8a` (64 bits), `armeabi-v7a` (32 bits / Android Go), `x86_64` (emuladores de 64 bits).
- **Android Gradle Plugin / Gradle:** 8.x+ / 9.x+.
- **Android NDK:** Versión 26.1.10909125 o superior.
- **CMake:** Versión 3.22.1+.
- **Rust Toolchain:** `rustc` y `cargo` (con targets `aarch64-linux-android`, `armv7-linux-androideabi` y `x86_64-linux-android` instalados).
- **Entorno del Usuario:** Diseñado para compilación autónoma y pruebas en teléfonos móviles y emuladores sin dependencia de Google Play Services privativos.

---

## 🚀 Instalación y Compilación Paso a Paso

### 1. Compilación del APK de Depuración
```bash
gradle :app:assembleDebug
```
El APK se genera en: `app/build/outputs/apk/debug/app-debug.apk`.

### 2. Compilación del APK de Lanzamiento (Distribución APK/Uptodown)
```bash
gradle :app:assembleRelease
```

### 3. Limpieza Total de Artefactos Nativos y Temporales
Para eliminar residuos de compilación pesados generados por C++, Cargo (`target`) y Lua:
```bash
chmod +x clean_native_artifacts.sh
./clean_native_artifacts.sh
```

### 4. Ejecución de Pruebas Unitarias
```bash
gradle :app:testDebugUnitTest
```

---

## 💡 Características Principales

1. **Persistencia Local con Room (SQLite Seguro):**
   - Almacenamiento fuera de línea garantizado sin depender de servicios en la nube privativos.
   - Búsqueda en tiempo real por texto, etiquetas y categorías.
   - Fijado de notas prioritarias y conteo automático de palabras y caracteres.

2. **Núcleo de Cómputo en Rust:**
   - Criptografía y operaciones de hashing en bajo nivel compiladas como librería estática nativa (`libvaultnotes_rust.a`).
   - Interfaz C FFI (`extern "C"`) de cero coste de abstracción.

3. **Intérprete C Oficial de Lua 5.4.6:**
   - Motor oficial de Lua compilado en C11 estático.
   - Permite la ejecución y evaluación dinámica de scripts y expresiones lógicas directamente desde el dispositivo.

4. **Puente JNI en C++17:**
   - Enlace bidireccional entre la JVM/ART de Kotlin y las librerías nativas con manejo de excepciones y validación de tipos JNI.

5. **Personalización Tipográfica (5 Estilos Offline):**
   - Selector interactivo accesible en la lista principal y en el editor de notas con previsualización en vivo.
   - 5 familias tipográficas nativas de Android: *Predeterminada (Sistema)*, *Sans-Serif Moderna*, *Serif Clásica (Editorial)*, *Monoespaciada (Código)* y *Cursiva Manuscrita*.
   - Persistencia automática de la preferencia sin requerir conectividad de red ni servicios externos.

6. **Distribución Autónoma:**
   - Preparado para tiendas de aplicaciones de terceros (Uptodown, F-Droid, APK directo) cumpliendo con políticas de privacidad e integridad del sistema.

---

## 👥 Gobernanza y Propuestas de la Comunidad (Filosofía Anti-Bloat)

Para evitar que VaultNotes se convierta en una aplicación sobrecargada con herramientas innecesarias que degraden el rendimiento:

- **Evolución guiada por la comunidad:** La hoja de ruta de nuevas funciones se define a partir de propuestas y votaciones de los propios usuarios.
- **Prevención de *Feature Bloat*:** No se implementan funciones superfluas en el núcleo de la aplicación; solo se integran aquellas características con demanda y utilidad contrastada.
- **Extensibilidad mediante Lua:** Aquellas funciones especializadas o flujos de trabajo avanzados propuestos por sectores específicos de la comunidad pueden articularse como scripts y plantillas sobre el motor Lua integrado, manteniendo el núcleo Kotlin/Rust limpio, veloz y ultraligero.
- **¿Cómo participar?:** Los usuarios pueden abrir propuestas de mejora o votar solicitudes existentes a través de los canales comunitarios del repositorio (Discussions e Issues de GitHub).

---

## 📁 Estructura General del Repositorio

```text
├── app/
│   ├── build.gradle.kts          # Pipeline de Gradle, tareas cargoBuild (arm64, armv7, x86_64) y CMake
│   └── src/
│       └── main/
│           ├── cpp/              # Código fuente C++ (JNI) y C (Lua 5.4.6 oficial)
│           ├── rust/             # Código fuente Rust (Cargo.toml, src/lib.rs)
│           ├── java/com/example/ # Código Kotlin, Compose UI, ViewModels, Room DB
│           └── res/              # Recursos visuales, temas XML y cadenas
├── .github/workflows/            # Flujos de CI de GitHub Actions (build-debug, override-commit)
├── setup_debug_keystore.sh       # Generador autónomo y limpio de debug.keystore
├── clean_native_artifacts.sh     # Script para purgar artefactos pesados (target, .cxx)
├── AI_CONTEXT.md                 # Contexto de negocio y arquitectura para IAs
├── STRUCTURE.md                  # Especificación detallada de módulos y archivos
├── ROADMAP.md                    # Hoja de ruta y próximos hitos
└── AGENTS.md                     # Directrices operativas para asistentes de código
```

---

## 🔒 Privacidad y Permisos

- No utiliza servicios privativos de Google Play.
- No utiliza permisos invasivos ni propiedades de sistema restringidas.
- Funciona 100% desconectado a internet para máxima privacidad del usuario.
