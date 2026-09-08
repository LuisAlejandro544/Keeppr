# Contexto de Inteligencia Artificial (AI_CONTEXT.md)

Este documento provee el contexto técnico, limitaciones del entorno y directrices de diseño para cualquier modelo de lenguaje o agente de IA que interactúe con el repositorio de VaultNotes.

---

## 🎯 Perfil del Proyecto y del Usuario

- **Propósito:** VaultNotes es una aplicación de notas y bóveda local de alta velocidad escrita en Kotlin y Jetpack Compose, potenciada con un backend nativo multinúcleo en C++, Rust y Lua.
- **Perfil del Usuario:** El usuario opera y prueba desde un dispositivo móvil/teléfono sin acceso a una estación de trabajo PC tradicional. Por ello, todos los comandos, scripts y salidas deben ser directos, robustos y sin fricciones.
- **Canal de Distribución:** La aplicación se distribuye como APK independiente en tiendas alternativas (Uptodown, APKMirror, F-Droid) y descarga directa, **no** en Google Play. No deben agregarse dependencias restrictivas a Google Play Services ni flujos de facturación privativos.
- **Tolerancia al Tamaño del APK:** Al usuario no le preocupa el peso final del APK siempre y cuando las dependencias sean 100% funcionales y completas. Debe evitarse la implementación de soluciones caseras incompletas o fallbacks degradados cuando existan librerías y dependencias sólidas.

---

## ⚙️ Arquitectura Técnica y Entorno

- **Versión mínima de Android:** `minSdk = 26` (Android 8.0 Oreo). No debe reducirse, ya que la versión 26 es indispensable para garantizar compatibilidad con el toolchain moderno de Rust y NDK en Android.
- **Lenguajes Compilados:**
  - **Kotlin:** Capa de interfaz de usuario (Compose M3), navegación y persistencia (Room).
  - **C++17:** JNI Bridge nativo (`app/src/main/cpp/native-bridge.cpp`).
  - **Rust (2021 Edition):** Motor de cálculo y criptografía (`app/src/main/rust`). Se compila mediante la tarea Gradle `cargoBuild` a las arquitecturas `aarch64-linux-android` y `x86_64-linux-android`.
  - **C11 (Lua 5.4.6 Oficial):** Intérprete oficial en C incluido en `app/src/main/cpp/lua/`, compilado como biblioteca estática con CMake.
- **Integridad del Pipeline de Compilación:**
  - C++, Rust y Lua están integrados de forma obligatoria en Gradle y CMake. Si se solicitan nuevas funciones nativas, deben implementarse respetando esta cadena de herramientas sin omitirlas ni sustituirlas por soluciones simuladas en Kotlin.

---

## 🛡️ Reglas de Seguridad y Buenas Prácticas

1. **Sin Propiedades del Sistema Peligrosas:** En caso de implementar utilidades de optimización o rendimiento, **nunca** utilizar comandos ni propiedades `persist.sys.*`.
2. **Propiedad Intelectual:** No emplear marcas registradas ni nombres de terceros protegidos por derechos de autor que puedan poner en riesgo al usuario.
3. **Limpieza de Artefactos Pesados:** Los directorios de compilación de Rust (`app/src/main/rust/target/`), archivos `.cxx`, objetos temporales `*.o`, `*.a`, `*.so` y bloqueos temporales deben eliminarse tras procesos de mantenimiento usando el script `./clean_native_artifacts.sh`.
4. **Mensajes de Commit:** Si existe un archivo `commit_message.txt`, su contenido debe mantenerse estrictamente en **español** y no debe modificarse a menos que el usuario lo solicite explícitamente.
5. **Enfoque de Razonamiento:** Razonar paso a paso antes de aplicar cambios en el código para no romper la compatibilidad entre capas (JNI, CMake, Cargo, Gradle).
