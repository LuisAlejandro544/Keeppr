# Hoja de Ruta (ROADMAP) - Keeppr

Plan de evolución técnica y funcional para la aplicación Keeppr (anteriormente VaultNotes).

---

## 📍 Fase 1: Arquitectura Base y Motor Nativo (Completada ✅)

- [x] Configuración de Android API 26+ (Android 8.0 Oreo) como versión mínima para compatibilidad NDK y Rust moderno.
- [x] Arquitectura MVVM reactiva con Jetpack Compose y Material Design 3.
- [x] Base de datos local Room con entidades de notas, categorías, etiquetas y búsqueda indexada.
- [x] Integración de toolchains nativos simultáneos en Gradle:
  - C++17 mediante CMake y JNI (`libvaultnotes_native.so`).
  - Rust mediante Cargo con soporte multiarquitectura: `arm64-v8a` (64 bits), `armeabi-v7a` (32 bits / Android Go) y `x86_64` (`libvaultnotes_rust.a`), con compilación estática autosuficiente sin dependencias externas complejas y resolución resiliente de toolchain y variables de enlazado cruzado en CI/CD.
  - Motor C oficial de Lua 5.4.6 compilado estáticamente sin wrappers de terceros.
- [x] Aceleración de procesamiento de notas y búsqueda en memoria nativa con Rust:
  - Extracción en una pasada nativa de métricas, conteo de palabras y snippets Markdown limpios (`rust_process_note_summary`).
  - Algoritmo de coincidencia y filtrado instantáneo en Rust (`rust_match_note`) coordinado con corrutinas de Kotlin en `Dispatchers.Default`.
- [x] Diálogo interactivo en la UI para inspección de motores y ejecución dinámica de scripts Lua.
- [x] Ajustes de apariencia y temas visuales (`SettingsDialog`):
  - Selector de modo de tema: Sistema, Modo Claro y Modo Oscuro.
  - Soporte para paletas dinámicas Material You en Android 12+ con interruptor on/off.
  - Paletas de acento personalizadas (Obsidian, Esmeralda, Ámbar, Azul Zafiro, Rosa Neón) para dispositivos sin Material You o con preferencia fija.
  - Selector de tipografía global predeterminada.
- [x] Personalización tipográfica granular con 5 familias nativas del sistema Android (Predeterminada, Sans-Serif, Serif, Monoespaciada, Cursiva) aplicables individualmente por nota o a fragmentos específicos de texto (inline con `[font:id]...[/font]`), con vista previa interactiva y persistencia en Room (migración v2).
- [x] Eliminación segura de notas con diálogo modal de confirmación en la UI (en tarjetas de lista y editor de notas).
- [x] Script de mantenimiento automatizado `clean_native_artifacts.sh` para purgar carpetas `target`, `.cxx` y archivos residuales.
- [x] Automatización CI con GitHub Actions (compilación limpia sin caché de APK Debug y soporte para sobrescritura de commits).
- [x] Script autónomo `setup_debug_keystore.sh` para generación forzada y limpia del keystore debug sin dependencias externas.
- [x] Suite integrada de depuración móvil en vivo (In-App Debug & Diagnostics):
  - Integración de LeakCanary (v2.14) para monitoreo automático de fugas de memoria.
  - HUD / Overlay flotante arrastrable de rendimiento con métricas en tiempo real (FPS con `Choreographer`, memoria JVM, memoria Heap Nativo C++/Rust, hilos concurrentes).
  - Dashboard de diagnóstico con 4 pestañas: Rendimiento en vivo, Explorador de Hilos con estado y prioridades, Visor de Logs (Logcat) con filtros por nivel, e Inspección del Sistema & Motores Nativos.

---

## 📍 Fase 2: Cifrado por Hardware y Bóveda Segura (En Curso 🔄)

- [x] Motor criptográfico nativo en Rust y C++: cifrado simétrico AES-256-CBC con padding PKCS#7 y generación de IV de 16 bytes.
- [x] Derivación de claves de alta resistencia PBKDF2-HMAC-SHA256 (10,000 iteraciones) con sal criptográfica de 16 bytes.
- [x] Autenticación de integridad mediante código MAC HMAC-SHA256 (32 bytes) verificado en tiempo constante contra ataques de temporización.
- [x] Protección de notas mediante contraseña del usuario con diálogo modal de confirmación (`EncryptNoteDialog`).
- [x] Desbloqueo interactivo en la lista de notas mediante diálogo modal (`DecryptNoteDialog`) con gestión de sesión efímera en memoria (`activeNoteSessionPassword`) y cero persistencia de contraseñas en SQLite o disco.
- [x] Cifrado automático al vuelo durante el autoguardado en segundo plano y cierre de notas en `NotesViewModel`.
- [x] Exclusión del contenido cifrado en las búsquedas en tiempo real para evitar fugas de datos en texto claro.
- [x] Eliminación flexible de la protección por contraseña con confirmación del usuario (`RemoveEncryptionDialog`).
- [x] Migración automática de la base de datos Room a versión 3 (`MIGRATION_2_3`) con columna `isEncrypted`.
- [ ] Autenticación biométrica complementaria (huella / reconocimiento facial) usando `androidx.biometric`.
- [ ] Modo "Bóveda Oculta": partición protegida por PIN independiente dentro de la base de datos Room.

---

## 📍 Fase 3: Automatizaciones y Extensiones en Lua (En Curso 🔄)

- [x] Ejecución de scripts y plantillas de Lua directamente desde el editor de notas (`LuaScriptDialog`):
  * Acceso rápido en la barra de herramientas Markdown ("🪄 Lua") y en el menú de desbordamiento de la barra superior.
  * Inyección nativa en C++ JNI de variables globales de contexto (`content` y `title`) hacia el estado oficial de Lua 5.4.6.
- [x] Generador de Plantilla de Registro Diario impulsado por script nativo de Lua: formato Markdown estructurado con fecha, hora, tareas y notas rápidas.
- [x] Editor de Script de Lua personalizado interactivo con salida en vivo, manejo de errores y 3 modos de inserción (*En el cursor*, *Al final* o *Reemplazar todo*).
- [ ] Procesamiento de notas en segundo plano mediante `WorkManager` con llamadas al motor Lua.
- [ ] Repositorio de scripts comunitarios descargables o importables localmente.

---

## 📍 Fase 4: Respaldo Autónomo y Portabilidad (En Curso 🔄)

- [x] Exportación e importación universal de notas:
  * Exportación a Markdown (.md) plano universal para sincronización con Obsidian, Notion y editores externos.
  * Exportación a Paquetes de Bóveda (.zip firmado) con metadatos completos (`vault_meta.json`) y firma criptográfica de integridad SHA-256 nativa.
  * Importación con validación de autenticidad y restauración de formato y tipografía.
- [ ] Sincronización punto a punto (P2P) local mediante red local WiFi/Hotspot sin intermediarios ni servidores en la nube.
- [x] Soporte para visualización y edición en Markdown enriquecido con renderizado y checkboxes interactivos.
- [x] Identidad de marca independiente y limpia: Renombrado a **Keeppr**, configuración de `applicationId = "com.keeppr.notes"` para almacenamiento limpio en `Android/data/com.keeppr.notes` sin referencias externas, y creación de nuevo icono de lanzador adaptativo inspirado en un cuaderno de tapa dura personal con banda elástica (estilo Moleskine).
- [x] Optimización de recursos gráficos nativos con WebP: Creación del script utilitario `convert_jpg_to_webp.sh` con compresión máxima multiruta (cwebp, ffmpeg, ImageMagick) y migración del logo de la app a formato `ic_keeppr_logo.webp` (reducción de peso >75% conservando calidad 100%).

---

## 📍 Fase 5: Distribución Beta y Pipeline de Release (En Curso 🔄)

- [x] **Diferenciación de Canales y Versiones Independientes:**
  * **Canary (Debug):** Nombre "Keeppr Canary", versión `v0.1.0-dev`, incluye suite de depuración in-app completa y soporte para emuladores x86_64.
  * **Beta (Release):** Nombre "Keeppr Beta", versión `v0.1.0-b`, descartando arquitecturas de emulador de PC (`x86_64`) y suprimiendo herramientas de depuración para máxima fluidez.
  * **Versión Estable:** Planificada para integrarse en un flujo productivo independiente tras completar el ciclo de validación comunitaria.
- [x] **Eliminación Global de Google Play Services y Rastreadores:**
  * Proyecto 100% libre del plugin de Google Services y librerías de Firebase/rastreadores en `build.gradle.kts` (raíz y app).
  * Arquitectura completamente offline, soberana y privada para distribución directa en Uptodown y APK independiente.
- [x] **Optimización de Arquitecturas Móviles en Release Beta:**
  * Descarte específico de arquitecturas de emulador de PC (`x86_64`) en la variante Release Beta.
  * Empaquetado optimizado centrado exclusivamente en procesadores móviles reales: `arm64-v8a` (64 bits) y `armeabi-v7a` (32 bits / Android Go).
- [x] **Aislamiento Total de Herramientas de Depuración:**
  * Desactivación condicional estricta (`BuildConfig.DEBUG`) de monitores de rendimiento, HUD flotante, visor de hilos y consola Logcat.
  * Exclusión del botón de depuración en la barra superior en modo Release.
  * LeakCanary empaquetado exclusivamente en variante debug (`debugImplementation`).
- [x] **Pipeline de Compilación Release Beta (`build-release.yml`):**
  * Disparador condicional estricto: se activa exclusivamente ante eventos de Pre-Release en GitHub con sufijo `-b` (ej. `v0.1.0-b`) o ejecución manual (`workflow_dispatch`).
  * Declaración explícita en el workflow indicando el estado del canal Beta y la hoja de ruta hacia la versión estable.
  * Compilación nativa para arquitecturas móviles (`aarch64-linux-android` y `armv7-linux-androideabi`), excluyendo tajantemente `x86_64` y `x86` del empaquetado final.
  * Soporte para firma con credenciales de producción vía GitHub Secrets (`RELEASE_KEYSTORE_BASE64`, etc.) con contingencia autofirmada.
  * Compilación de Release con optimización R8/ProGuard (`isMinifyEnabled = true`, `isShrinkResources = true`) reduciendo el APK de ~22 MB a ~4.45 MB con preservación 100% de JNI y Room.
  * Generación y nombramiento oficial del artefacto como `Keeppr-v0.1.0-b-Release.apk` junto a su suma de verificación criptográfica SHA-256.
- [x] **Pipeline y Archivo de Registro de Cambios Beta:**
  * Flujo automatizado `process-changelog-beta.yml` activado ante Pre-Releases con sufijo `-b`, publicando automáticamente el contenido de `Changelog-beta.md` en el cuerpo de la release de GitHub.
  * Registro de capacidades `Changelog-beta.md` con las 7 dimensiones clave del sistema.
- [x] **Optimización de CI y Pulido de UI para Beta:**
  * Compilación de Debug APK (`build-debug.yml`) para la variante Canary (`v0.1.0-dev`) con artefacto `Keeppr-Canary-v0.1.0-dev-APK` en ejecución manual (`workflow_dispatch`).
  * Remoción del modal de prueba redundante "Motores Nativos" de la barra principal de notas, manteniendo el soporte completo de Lua 5.4.6 enfocado en el editor (`LuaScriptDialog`).
  * Sustitución de notas de prueba pre-hechas por una única nota oficial de bienvenida y guía de inicio en `AppDatabase.kt`.
- [x] **Sistema de Actualizaciones Desacoplado e Instalación Directa In-App:**
  * Script oficial en Lua 5.4 (`updater_config.lua`) con endpoints desacoplados del binario y filtrado de pre-releases beta con sufijo `-b`.
  * Soporte para carga dinámica del script Lua desde GitHub Raw con contingencia local en assets.
  * Gestor reactivo `AppUpdateManager` que consulta la API de GitHub Releases, descarga el APK (.apk) a la caché privada reportando progreso (MB y %) e invoca directamente el instalador de paquetes de Android vía `FileProvider` sin salir al navegador.
  * Sección de comprobación y descarga integrada en el diálogo de Ajustes con Material Design 3.
- [x] **Comunidad y Gobernanza en Discord (`DISCORD_CONFIG.md`):**
  * Estructura inicial del servidor con jerarquía de roles: 👑 Creador, 🧪 Beta Tester, 💡 Colaborador y 📱 Miembro.
- [x] **Habilitación y Ajuste de Reglas R8 / ProGuard (`app/proguard-rules.pro`):**
  * 5 pasadas de optimización y reducción de recursos logrando bajar el peso del APK de ~22 MB a solo ~4.45 MB sin romper firmas nativas JNI de C++, Rust o Lua 5.4.6.
- [x] **Optimización en R8 Full Mode y Almacenamiento Instalado Ultra-Ligero:**
  * Activación de `android.enableR8.fullMode=true` para colapsar lambdas de Kotlin, fusionar clases sintéticas y compactar bytecode DEX (`classes.dex`).
  * Blindaje estricto de firmas JNI en C++, Rust y Lua (`-keepclasseswithmembernames` y `-keepclasseswithmembers`).
  * Eliminación de regla sobreprotectora de Compose runtime permitiendo a R8 podar código muerto oficial.
  * Poda de comprobaciones de nulidad repetitivas de Kotlin (`kotlin.jvm.internal.Intrinsics`).
  * Empaquetado nativo sin extracción en disco (`android:extractNativeLibs="false"` y `useLegacyPackaging = false`), ahorrando entre 3 y 5 MB en el almacenamiento interno del usuario al evitar duplicar librerías `.so` en 32 y 64 bits.
  * Poda de dependencias huérfanas de la plantilla (`Retrofit`, `Moshi`, interceptor de logs) y purga de más de 75 idiomas no soportados en `resources.arsc` mediante `resourceConfigurations = ["es", "en"]`.
  * Exclusión de metadatos de desarrollo `META-INF/*.kotlin_module` en el empaquetado final.
- [ ] Pruebas de compatibilidad en dispositivos Android Go y procesadores de 32 bits en hardware real.
- [ ] Distribución del APK Beta en Uptodown y apertura de canales de feedback comunitarios (Discord / Telegram / Reddit).

---

## 🗳️ Gobernanza de Funciones: Impulsada por la Comunidad

Para proteger a Keeppr del *feature bloat* y de código innecesario que casi nadie utiliza:

1. **Priorización por demanda real:** Solo se implementarán aquellas características propuestas y votadas activamente por la comunidad de usuarios.
2. **Núcleo ligero:** El núcleo de la aplicación se mantiene austero, rápido y centrado en la seguridad de notas.
3. **Módulos opcionales en Lua:** Las peticiones específicas de nicho se implementarán como extensiones o plantillas Lua para no sobrecargar el APK ni el consumo de memoria del sistema.
