# VaultNotes (Motor Nativo Híbrido: Kotlin + C++ + Rust + Lua)

Aplicación móvil de gestión segura de notas y apuntes de alto rendimiento, diseñada con persistencia local robusta en Room, interfaz moderna en Jetpack Compose y un motor nativo multinúcleo en C++, Rust y Lua (C puro) compilado para distribución directa en formato APK (Uptodown y tiendas independientes).

---

## 📋 Requisitos Previos y Entorno

- **Sistema Operativo Compatible:** Android 8.0 (Oreo / API 26) o superior.
- **Arquitecturas NDK Soportadas:** `arm64-v8a`, `x86_64`.
- **Android Gradle Plugin / Gradle:** 8.x+ / 9.x+.
- **Android NDK:** Versión 26.1.10909125 o superior.
- **CMake:** Versión 3.22.1+.
- **Rust Toolchain:** `rustc` y `cargo` (con targets `aarch64-linux-android` y `x86_64-linux-android` instalados).
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

5. **Distribución Autónoma:**
   - Preparado para tiendas de aplicaciones de terceros (Uptodown, F-Droid, APK directo) cumpliendo con políticas de privacidad e integridad del sistema.

---

## 📁 Estructura General del Repositorio

```text
├── app/
│   ├── build.gradle.kts          # Pipeline de Gradle, tareas cargoBuild y enlace CMake
│   └── src/
│       └── main/
│           ├── cpp/              # Código fuente C++ (JNI) y C (Lua 5.4.6 oficial)
│           ├── rust/             # Código fuente Rust (Cargo.toml, src/lib.rs)
│           ├── java/com/example/ # Código Kotlin, Compose UI, ViewModels, Room DB
│           └── res/              # Recursos visuales, temas XML y cadenas
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
