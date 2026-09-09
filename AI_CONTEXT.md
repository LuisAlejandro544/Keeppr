# Contexto de Inteligencia Artificial (AI_CONTEXT.md)

Este documento provee el contexto técnico, limitaciones del entorno y directrices de diseño para cualquier modelo de lenguaje o agente de IA que interactúe con el repositorio de Keeppr (anteriormente VaultNotes).

---

## 🎯 Perfil del Proyecto y del Usuario

- **Propósito:** Keeppr es una aplicación de notas y bóveda local de alta velocidad escrita en Kotlin y Jetpack Compose, potenciada con un backend nativo multinúcleo en C++, Rust y Lua.
- **Versión Oficial Actual:** `v0.1.0-b` (Beta) orientada a pruebas comunitarias y distribución en APK independiente.
- **Identidad e Identificador de Paquete:** Su nombre público es **Keeppr** y su `applicationId` es `com.keeppr.notes`, garantizando almacenamiento limpio y profesional en `Android/data/com.keeppr.notes` sin dependencias ni referencias externas.
- **Icono de Lanzador:** Icono adaptativo basado en una libreta/cuaderno de tapa dura personal con banda elástica vertical (estilo Moleskine), optimizado en formato WebP de máxima calidad y alta compresión (`ic_keeppr_logo.webp`), administrable mediante el script autónomo `convert_jpg_to_webp.sh`.
- **Perfil del Usuario:** El usuario opera y prueba desde un dispositivo móvil/teléfono sin acceso a una estación de trabajo PC tradicional. Por ello, todos los comandos, scripts y salidas deben ser directos, robustos y sin fricciones.
- **Canal de Distribución:** La aplicación se distribuye como APK independiente en tiendas alternativas (Uptodown, APKMirror, F-Droid) y descarga directa, **no** en Google Play. No deben agregarse dependencias restrictivas a Google Play Services ni flujos de facturación privativos.
- **Tolerancia al Tamaño del APK:** Al usuario no le preocupa el peso final del APK siempre y cuando las dependencias sean 100% funcionales y completas. Debe evitarse la implementación de soluciones caseras incompletas o fallbacks degradados cuando existan librerías y dependencias sólidas.
- **Entorno de Depuración en el Teléfono (In-App Debugging):** Dado que el usuario opera sin PC, todas las herramientas de telemetría y diagnóstico deben estar integradas visualmente en la propia aplicación (Overlay HUD flotante arrastrable de FPS, memoria JVM y Heap nativo de C++/Rust, visor de hilos en tiempo real, lector de Logcat embebido y detección automática de fugas de memoria con LeakCanary v2.14).
- **Filosofía de Desarrollo Anti-Bloat (Comunidad al Mando):** La evolución funcional está guiada directamente por las propuestas de la comunidad para evitar saturar la app con funciones superfluas que casi nadie usará. Las funciones añadidas deben responder a necesidades reales votadas por usuarios. La personalización visual (selección de modo claro/oscuro/sistema, soporte opcional para colores dinámicos Material You en Android 12+, paletas de acento personalizadas, 5 tipografías aplicables de forma individual por nota o a fragmentos específicos de texto mediante etiquetas inline) y la extensibilidad mediante Lua son ejemplos de funciones con alto valor utilitario sin recargar el núcleo.

---

## ⚙️ Arquitectura Técnica y Entorno

- **Versión mínima de Android:** `minSdk = 26` (Android 8.0 Oreo). No debe reducirse, ya que la versión 26 es indispensable para garantizar compatibilidad con el toolchain moderno de Rust y NDK en Android.
- **Lenguajes Compilados:**
  - **Kotlin:** Capa de interfaz de usuario (Compose M3), navegación, renderizado de Markdown enriquecido (con soporte para fuentes por nota y etiquetas de tipografía inline `[font:id]...[/font]`), gestión segura del ciclo de vida de notas con eliminación confirmada, persistencia local en Room (versión 3 con migraciones `MIGRATION_1_2` y `MIGRATION_2_3` para `isEncrypted`), gestión de sesiones efímeras de contraseñas y empaquetado/importación de notas (`VaultPackageHelper`).
  - **C++17:** JNI Bridge nativo (`app/src/main/cpp/native-bridge.cpp`) con motor criptográfico de alta velocidad `vault_crypto` para generación de firmas SHA-256 salteadas, cifrado/descifrado de notas y verificación en tiempo constante (constant-time).
  - **Rust (2021 Edition) & C Core:** Motor de cálculo, hashing SHA-256 criptográfico, cifrado simétrico AES-256-CBC con padding PKCS#7, derivación de clave PBKDF2-HMAC-SHA256 (10,000 iteraciones), código MAC HMAC-SHA256 y acelerador de rendimiento de notas (`app/src/main/rust`). Ejecuta en memoria nativa la extracción de métricas, conteo de palabras, resúmenes limpios de Markdown y filtrado/búsqueda insensible a mayúsculas. Se compila mediante la tarea Gradle `cargoBuild` y CMake para arquitecturas `aarch64-linux-android` (64 bits), `armv7-linux-androideabi` (32 bits / Android Go) y `x86_64-linux-android` (emuladores).
  - **C11 (Lua 5.4.6 Oficial):** Intérprete oficial en C incluido en `app/src/main/cpp/lua/`, compilado como biblioteca estática con CMake. Integrado directamente en el editor de notas (`NoteEditorScreen.kt`, `MarkdownToolbar.kt` y `LuaScriptDialog.kt`), con inyección nativa de variables globales `content` y `title` para permitir la inserción de Plantillas Diarias dinámicas o la ejecución y prueba de scripts personalizados con 3 modos de inserción (*cursor*, *al final* o *reemplazar*).
- **Sistema de Cifrado con Contraseña:**
  - Cifrado seguro de notas bajo demanda: el usuario define y confirma una contraseña, derivando claves mediante PBKDF2-HMAC-SHA256.
  - La contraseña nunca se almacena en SQLite ni en disco; reside únicamente en la memoria volátil del proceso durante la sesión de edición activa.
  - Las notas protegidas quedan excluidas de los índices y consultas de búsqueda general para impedir fugas accidentales de fragmentos en texto claro.
- **Importación y Exportación Universal:**
  - *Exportación:* Modo Markdown (.md) plano universal para sincronización con Obsidian/Notion/PC, y modo Paquete de Bóveda (.zip) con `note.md`, `vault_meta.json` (metadatos completos y tipografía asignada) y `signature.vault` (firma SHA-256 nativa).
  - *Importación:* Soporta archivos `.md`, `.txt` y paquetes `.zip` con verificación automática de firma para garantizar la autenticidad e integridad de la nota importada.
- **Integridad del Pipeline de Compilación y CI/CD:**
  - C++, Rust y Lua están integrados de forma obligatoria en Gradle y CMake. Si se solicitan nuevas funciones nativas, deben implementarse respetando esta cadena de herramientas sin omitirlas ni sustituirlas por soluciones simuladas en Kotlin.
  - Flujo `build-release.yml` en GitHub Actions para compilación de APKs Release, disparado ante Pre-Releases con etiqueta terminada en `-b` (ej. `v0.1.0-b`). Sin ofuscación R8/ProGuard (`isMinifyEnabled = false`) para estabilidad en la beta.
  - Gestión de credenciales de firma vía GitHub Secrets (`RELEASE_KEYSTORE_BASE64`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`).
  - Flujo `process-changelog-beta.yml` que inyecta automáticamente el contenido de `Chanelog-beta.md` en la descripción del Pre-Release con etiqueta `-b`.
  - Flujo `build-debug.yml` ejecutado exclusivamente de forma manual (`workflow_dispatch`) para evitar compilaciones automáticas por commit.

---

## 🛡️ Reglas de Seguridad y Buenas Prácticas

1. **Sin Propiedades del Sistema Peligrosas:** En caso de implementar utilidades de optimización o rendimiento, **nunca** utilizar comandos ni propiedades `persist.sys.*`.
2. **Propiedad Intelectual:** No emplear marcas registradas ni nombres de terceros protegidos por derechos de autor que puedan poner en riesgo al usuario.
3. **Limpieza de Artefactos Pesados:** Los directorios de compilación de Rust (`app/src/main/rust/target/`), archivos `.cxx`, objetos temporales `*.o`, `*.a`, `*.so` y bloqueos temporales deben eliminarse tras procesos de mantenimiento usando el script `./clean_native_artifacts.sh`.
4. **Mensajes de Commit:** Si existe un archivo `commit_message.txt`, su contenido debe mantenerse estrictamente en **español** y no debe modificarse a menos que el usuario lo solicite explícitamente.
5. **Enfoque de Razonamiento:** Razonar paso a paso antes de aplicar cambios en el código para no romper la compatibilidad entre capas (JNI, CMake, Cargo, Gradle).
