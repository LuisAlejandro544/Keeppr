# Instrucciones de Agente (AGENTS.md)

Este archivo contiene las directrices obligatorias y persistentes para cualquier agente de codificación o asistente que trabaje en este repositorio.

---

## 🧠 1. Regla de Razonamiento Previo Obligatorio
- Antes de aplicar cualquier cambio o herramienta, debes razonar detenidamente paso a paso.
- Identifica qué archivos intervienen, qué impacto tendrán los cambios en las capas de Kotlin, C++, Rust y Lua, y qué herramientas usarás antes de proceder.
- No respondas ni actúes sin antes estructurar tu razonamiento interno.

---

## 📱 2. Entorno y Perfil del Usuario
- **Sin PC:** El usuario utiliza y administra el proyecto exclusivamente desde un **teléfono móvil**.
- Las explicaciones deben ser claras, concisas, directas y accionables desde una pantalla móvil.
- Los scripts proporcionados deben ser autosuficientes y listos para ejecutar en un solo comando.

---

## 📦 3. Canal de Distribución y Dependencias
- **Canal:** La aplicación será distribuida a través de **Uptodown** o como APK independiente de terceros, **no** a través de Google Play.
- **Peso del APK:** Al usuario **no le importa el peso final del APK**, siempre y cuando las librerías y dependencias sean 100% funcionales y completas.
- **Evitar Soluciones Sin Dependencias:** No inventes implementaciones manuales o "artesanales" que degraden la estabilidad. Utiliza librerías y dependencias oficiales y completas cuando se requieran.

---

## ⚙️ 4. Cadena de Compilación Nativa (C++, Rust, Lua)
- Si la aplicación móvil incluye C++, Rust, Lua, Python u otro runtime nativo, **todos deben estar debidamente incluidos y configurados** en el pipeline de Gradle (`build.gradle.kts`), CMake (`CMakeLists.txt`) y Cargo (`Cargo.toml`).
- **Prohibido omitir compilaciones nativas:** No implementes funciones fallback degradadas en Kotlin si el usuario solicitó la funcionalidad con un lenguaje o framework nativo específico.
- **Versión de Android:** La versión mínima de Android del proyecto es `minSdk = 26` (Android 8.0 Oreo). Nunca reduzcas esta versión si eso compromete la compatibilidad del NDK o del runtime de Rust.

---

## 🛡️ 5. Restricciones de Sistema y Seguridad
- **Rendimiento / Game Booster:** En caso de implementar funciones de optimización para móvil, **NUNCA utilices propiedades ni comandos `persist.sys.*`**.
- **Propiedad Intelectual:** Evita nombrar archivos o identificadores con marcas comerciales o protegidas por derechos de autor que puedan comprometer al usuario.
- **Mensajes de Commit:** Si existe un archivo `commit_message.txt`, asegúrate de que toda su información esté en **español** y **no lo actualices** ni modifiques a menos que el usuario lo solicite expresamente.

---

## 🧹 6. Limpieza de Archivos Residuales
- Toda compilación de Rust genera un directorio `target` y CMake genera `.cxx`.
- Si se solicita limpiar el proyecto o se preparan entregas limpias, debe ejecutarse o mantenerse actualizado el script:
  ```bash
  ./clean_native_artifacts.sh
  ```
- No dejes carpetas `target`, archivos de bloqueo temporales innecesarios o binarios intermedios en el repositorio de código fuente.

---

## 🔍 7. Economía de Inspección
- No revises ni leas archivos de código que no sean estrictamente necesarios para la solicitud o interacción inmediata del usuario.
