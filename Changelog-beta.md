# 📝 Keeppr — Registro de Cambios y Capacidades (Beta v0.1.0-b)

¡Bienvenido a la primera versión Beta (**v0.1.0-b**) de **Keeppr**!  
Keeppr es una aplicación móvil de notas personales de alto rendimiento, diseñada para la privacidad absoluta, la velocidad nativa y la extensibilidad mediante automatizaciones.

---

## 🚀 ¿Qué ofrece Keeppr en esta versión Beta?

### 🔐 1. Privacidad Extrema y Cifrado Criptográfico Nativo (Rust & C++)
* **Cifrado local grado militar:** Implementación nativa en Rust (`AES-256-CBC` con relleno PKCS#7).
* **Derivación de llaves robusta:** PBKDF2 con HMAC-SHA256 y 10,000 iteraciones con sal pseudoaleatoria para proteger contraseñas contra ataques de fuerza bruta.
* **Verificación de integridad:** HMAC-SHA256 en tiempo constante (*constant-time compare*) para evitar ataques de temporización.
* **Seguridad en memoria:** La contraseña de la nota reside únicamente en la memoria volátil de la sesión activa y se purga inmediatamente al cerrar la nota.
* **Búsqueda ciega:** El texto de notas protegidas queda excluido de búsquedas en texto plano para prevenir fugas de información.

### ⚡ 2. Rendimiento Híbrido Multi-Arquitectura
* **Motores nativos integrados:** Núcleo de C++ y Rust enlazados directamente por JNI sin dependencias lentas de interpretación.
* **Optimización para hardware móvil real:** En el APK Release Beta se descartan las arquitecturas de emulador de PC (`x86_64`), compilando exclusivamente para procesadores de smartphones y tablets: `arm64-v8a` (64 bits) y `armeabi-v7a` (32 bits / Android Go). (La variante Canary de desarrollo conserva soporte de emuladores x86_64).
* **Procesamiento instantáneo:** Resúmenes y conteos de texto en memoria nativa con latencia cero.
* **100% libre de servicios privativos:** Eliminación global de Google Play Services y SDKs de Firebase/rastreadores.

### 🪄 3. Automatizaciones y Plantillas Dinámicas con Lua 5.4.6
* **Motor Lua embebido:** Intérprete oficial de Lua 5.4.6 integrado en el pipeline nativo.
* **Plantillas rápidas:** Generador inteligente de *Registro Diario* con fecha, hora, checklists y secciones de reflexión en un solo toque.
* **Consola y editor de scripts:** Permite ejecutar código Lua personalizado interactuando con variables globales (`content`, `title`), con previsualización de resultados y 3 modos de inserción (*En el cursor*, *Al final*, *Reemplazar todo*).

### ✍️ 4. Editor Markdown Completo y Visualización Dual
* **Barra de formato táctil:** Acceso rápido a encabezados (H1, H2, H3), negrita, cursiva, listas ordenadas, checklists de tareas, citas, bloques de código, tablas y enlaces.
* **Selector de modo responsivo:** Alterna sin esfuerzo entre el modo de edición y la vista previa renderizada en tiempo real.
* **Selector de emojis integrado:** Acceso directo a emojis por categorías sin depender del teclado del sistema.

### 📦 5. Soberanía de Datos (Sin "Vendor Lock-in")
* **Exportación abierta en Markdown plano:** Guarda y comparte tus notas en formato `.md` estándar compatible con Obsidian, Logseq, Typora o editores de escritorio.
* **Paquetes de Bóveda (`.zip`):** Exportación e importación segura de notas individuales o bóvedas completas con manifiesto estructurado y hash de integridad SHA-256.

### 🎨 6. Diseño Material 3 Adaptable y Personalización
* **Theming Moderno M3:** Compatibilidad total con colores dinámicos (Material You) en Android 12+.
* **6 Paletas de color elegantes:** Opciones de acento preconfiguradas (Pizarra, Índigo, Bosque, Borgoña, Ámbar y Océano).
* **Tipografías personalizables:** Ajuste tipográfico independiente por nota (Inter, Serif, Monoespaciada, Cursiva, Sans).
* **Modo Oscuro / Claro / Sistema:** Adaptación fluida a la preferencia del dispositivo.

### 🛠️ 7. Herramientas de Diagnóstico y Rendimiento (Exclusivas de Canary Debug)
* **Diseñado para desarrollo y pruebas en smartphone:** Panel de diagnóstico interno accesible directamente desde la interfaz en la variante **Keeppr Canary** (`v0.1.0-dev`).
* **Overlay Flotante de FPS:** Medición precisa en tiempo real de cuadros por segundo para detectar micro-tirones.
* **Monitor de memoria JVM y Nativa:** Gráficos en vivo del consumo de RAM de la app.
* **Visor de Logcat integrado:** Inspecciona logs del sistema sin necesidad de una computadora ni ADB.
* **Detección de fugas:** Integración de LeakCanary en compilaciones de prueba.
* **Aislamiento en Release Beta:** En el APK de **Keeppr Beta** (`v0.1.0-b`), todas las herramientas de depuración e interfaces de desarrollo se eliminan y desactivan por completo para ofrecer la máxima fluidez y pureza al usuario final.

### 🏷️ 8. Detalle de Nomenclatura y Distribución
* **Nombre Oficial del Archivo APK:** `Keeppr-v0.1.0-b-Release.apk`
* **Desglose del Esquema de Nomenclatura:**
  - `Keeppr`: Nombre de marca del proyecto.
  - `v0.1.0`: Versión semántica base (`MAJOR.MINOR.PATCH`).
  - `-b`: Identificador obligatorio de canal **Beta** comunitaria (Pre-Release). Permite al gestor de actualizaciones (`updater_config.lua`) y al CI reconocer entregas de prueba antes del canal estable.
  - `-Release`: Variante de compilación para distribución en usuarios finales (desprovista de herramientas de depuración, sin dependencias de prueba como LeakCanary y firmada).
  - `.apk`: Paquete binario estándar de Android para instalación directa fuera de tiendas privativas.
* **Exclusión de Arquitecturas de PC:** El binario Release descarta estrictamente librerías nativas para emuladores x86/x86_64 (`lib/x86_64` y `lib/x86`), garantizando un empaquetado 100% puro para procesadores móviles (`arm64-v8a` de 64 bits y `armeabi-v7a` de 32 bits).
* **Verificación de Integridad:** Se adjuntan sumas de verificación `SHA256SUMS.txt` en cada publicación de GitHub y canal Uptodown para comprobar que el archivo descargado no haya sufrido corrupción ni manipulación en tránsito.

### ⚡ 9. Optimización Extrema de Peso con R8 y Experiencia Inicial
* **Reducción del APK de 22 MB a ~4.45 MB:** Activación completa de R8 (`isMinifyEnabled = true`, `isShrinkResources = true`) con `proguard-android-optimize.txt`, reduciendo el peso de descarga en más del 79%.
* **5 Pasadas de Optimización:** Inlining agresivo y fusión de clases preservando la compatibilidad absoluta con JNI nativo (`NativeEngine`), Room Database y Kotlin Coroutines.
* **Nota de Bienvenida Oficial y Única:** Sustitución de notas de demostración por una única nota de bienvenida explicativa que detalla el uso del editor Markdown, checklists interactivas, cifrado AES-256-GCM / Argon2id, automatizaciones Lua y funcionamiento 100% local.

---

## 📋 Información de la Versión
* **Nombre de la App (Release):** Keeppr Beta
* **Nombre del Binario APK:** `Keeppr-v0.1.0-b-Release.apk`
* **Tamaño del APK Release:** ~4.45 MB (optimizado con R8 y reducción de recursos)
* **Versión de Release (Pre-Release):** `v0.1.0-b` (Beta comunitaria)
* **Nombre de la App (Debug):** Keeppr Canary
* **Versión de Debug (Desarrollo):** `v0.1.0-dev`
* **Versión Estable:** Planificada para publicación oficial tras completar las fases de prueba comunitaria.
* **Código de versión:** `1`
* **Target SDK:** Android 14 (API 36)
* **Min SDK:** Android 8.0 Oreo (API 26)
* **Arquitecturas del APK Beta:** `arm64-v8a` (64 bits) y `armeabi-v7a` (32 bits / Android Go). Emuladores de PC (`x86_64` y `x86`) descartados.
* **Canal de distribución previsto:** Uptodown / Instalación directa vía APK independiente (100% offline, sin Google Play Services).
