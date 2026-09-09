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
* **Soporte completo para 32 y 64 bits:** Binarios nativos compilados para `arm64-v8a`, `armeabi-v7a` (compatible con Android Go) y `x86_64`.
* **Procesamiento instantáneo:** Resúmenes y conteos de texto en memoria nativa con latencia cero.

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

### 🛠️ 7. Herramientas de Diagnóstico y Rendimiento en el Móvil
* **Diseñado para desarrollo y pruebas en smartphone:** Panel de diagnóstico interno accesible directamente desde la interfaz.
* **Overlay Flotante de FPS:** Medición precisa en tiempo real de cuadros por segundo para detectar micro-tirones.
* **Monitor de memoria JVM y Nativa:** Gráficos en vivo del consumo de RAM de la app.
* **Visor de Logcat integrado:** Inspecciona logs del sistema sin necesidad de una computadora ni ADB.
* **Detección de fugas:** Integración de LeakCanary en compilaciones de prueba.

---

## 📋 Información de la Versión
* **Versión:** `v0.1.0-b` (Beta)
* **Código de versión:** `1`
* **Target SDK:** Android 14 (API 36)
* **Min SDK:** Android 8.0 Oreo (API 26)
* **Canal de distribución previsto:** Uptodown / Instalación directa vía APK independiente.
