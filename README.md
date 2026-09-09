# Keeppr (Motor Nativo Híbrido: Kotlin + C++ + Rust + Lua)

Aplicación móvil de gestión segura de notas y apuntes de alto rendimiento, diseñada con persistencia local robusta en Room, interfaz moderna en Jetpack Compose y un motor nativo multinúcleo en C++, Rust y Lua (C puro) compilado para distribución directa en formato APK (Uptodown y tiendas independientes).

- **Nombre Oficial:** Keeppr
- **Versión Actual:** `v0.1.0-b` (Beta)
- **Identificador de Paquete (Application ID):** `com.keeppr.notes` (almacenamiento en `Android/data/com.keeppr.notes`)
- **Identidad Visual / Icono:** Cuaderno de tapa dura personal con banda elástica vertical (estilo Moleskine) en formato WebP de máxima calidad y alta compresión (`ic_keeppr_logo.webp`), integrado como icono adaptativo Android sobre paleta oscura y acentos cálidos.
- **Registro de Capacidades Beta:** Consultar `Chanelog-beta.md` para el desglose completo de novedades de la versión.

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

### 4. Conversor de Imágenes a WebP (Máxima Compresión y Calidad)
Script utilitario para convertir recursos gráficos y logos de formato JPG a WebP sin pérdida perceptible de calidad:
```bash
chmod +x convert_jpg_to_webp.sh
# Conversión predeterminada del logo de la app:
./convert_jpg_to_webp.sh
# Conversión de cualquier imagen personalizada:
./convert_jpg_to_webp.sh ruta/imagen.jpg [ruta_salida.webp] [max_quality|lossless]
```

### 5. Ejecución de Pruebas Unitarias
```bash
gradle :app:testDebugUnitTest
```

---

## 🤖 Automatización CI/CD (GitHub Actions)

El proyecto cuenta con pipelines de integración y entrega continua para compilación nativa en la nube:

1. **Compilación Debug (`build-debug.yml`):** Configurado para ejecución manual (`workflow_dispatch`), compilando los núcleos nativos (C++, Rust y Lua) y empaquetando el APK de depuración como artefacto descargable cuando sea necesario, sin saturar ejecuciones con cada commit.
2. **Compilación Release Beta (`build-release.yml`):** Se activa automáticamente al publicar un **Pre-Release en GitHub con etiqueta terminada en `-b`** (ej. `v0.1.0-b`) o mediante ejecución manual (`workflow_dispatch`). Compila las arquitecturas de 64 y 32 bits, firma el APK y lo adjunta directamente a la versión en GitHub. Actualmente sin ofuscación R8/ProGuard (`isMinifyEnabled = false`) para estabilidad beta.
3. **Publicación de Changelog Beta (`process-changelog-beta.yml`):** Se activa automáticamente al detectar un **Pre-Release con sufijo `-b`** (o ejecución manual), inyectando el contenido completo de `Chanelog-beta.md` directamente en la descripción y notas de la versión en GitHub y archivándolo como respaldo.
4. **Sincronización de Mensajes de Commit (`override-commit-message.yml`):** Mantiene el historial de commits alineado con el contenido de `commit_message.txt`.

### 🔑 Configuración de Secretos en GitHub (GitHub Secrets)
Para que el flujo `build-release.yml` firme el APK con tu propia clave oficial de producción, configura los siguientes secretos en tu repositorio (**Settings** > **Secrets and variables** > **Actions** > **New repository secret**):

| Nombre del Secreto | Descripción | Ejemplo / Valor |
|---|---|---|
| `RELEASE_KEYSTORE_BASE64` | Archivo `.jks` o `.keystore` codificado en Base64 | Cadena de texto generada con `base64 -w 0 mi_llave.jks` |
| `RELEASE_STORE_PASSWORD` | Contraseña del almacén de claves (Keystore) | `mi_password_seguro` |
| `RELEASE_KEY_ALIAS` | Alias de la clave dentro del almacén | `upload` |
| `RELEASE_KEY_PASSWORD` | Contraseña de la clave privada | `mi_password_seguro` |

> *Nota:* Si aún no has configurado estos secretos, el workflow detectará su ausencia y generará automáticamente una clave de contingencia autofirmada para que el empaquetado no falle y puedas probar el APK de inmediato.

---

## 💡 Características Principales

1. **Persistencia Local con Room (SQLite Seguro):**
   - Almacenamiento fuera de línea garantizado sin depender de servicios en la nube privativos.
   - Búsqueda en tiempo real por texto, etiquetas y categorías (con protección y exclusión de contenido cifrado).
   - Fijado de notas prioritarias y conteo automático de palabras y caracteres.
   - Eliminación segura de notas con diálogo modal de confirmación, accesible tanto desde la tarjeta en la lista principal como desde la barra superior del editor.
   - Guardado individual de la tipografía base por nota (`fontTheme`) y estado de protección (`isEncrypted`) con migración automática de base de datos a versión 3 (`MIGRATION_2_3`).

2. **Cifrado Criptográfico Nativo con Contraseña (AES-256 + PBKDF2 en Rust/C++):**
   - **Cifrado de grado militar offline:** Cifrado simétrico AES-256-CBC con padding PKCS#7 y generación de vector de inicialización (IV) de 16 bytes directamente en el motor nativo de Rust y C++.
   - **Derivación de clave robusta:** PBKDF2-HMAC-SHA256 con 10,000 iteraciones y sal aleatoria de 16 bytes para resistir ataques de fuerza bruta y diccionarios.
   - **Autenticación de Integridad:** Código de autenticación de mensajes HMAC-SHA256 (32 bytes) verificado en tiempo constante contra ataques de temporización (timing attacks).
   - **Seguridad en memoria y cero persistencia de claves:** La contraseña nunca se almacena en disco ni en base de datos. Se mantiene en una sesión volátil en memoria (`activeNoteSessionPassword`) mientras la nota está abierta y se purga de inmediato al cerrarla.
   - **Privacidad en la interfaz:** Las notas cifradas muestran un distintivo de candado y un extracto protegido en la lista principal. Para abrirlas se requiere desbloqueo mediante contraseña. Además, el contenido cifrado queda excluido de la búsqueda general para prevenir fugas de información sensible.
   - **Gestión flexible:** El usuario puede proteger cualquier nota con contraseña, confirmar la clave antes de guardar y desprotegerla en cualquier momento si lo desea.

3. **Núcleo de Cómputo y Aceleración en Rust:**
   - Criptografía, operaciones de hashing y procesamiento de texto en bajo nivel compiladas como librería estática nativa (`libvaultnotes_rust.a`).
   - Implementación pura y sin dependencias externas complejas (zero-dependency self-contained), garantizando compatibilidad total con toolchains estándar de Rust (Cargo) y evitando fallos de compatibilidad con ediciones modernas (`edition2024`).
   - Tarea de construcción automatizada `cargoBuild` en Gradle con detección inteligente del NDK y soporte de enlazado y archivado cruzado (`aarch64-linux-android`, `x86_64-linux-android`, `armv7-linux-androideabi`).
   - Aceleración nativa de carga y renderizado de notas: extracción de extractos limpios de Markdown (`snippet`), cálculo de métricas y tiempo de lectura en una sola pasada en memoria nativa (`rust_process_note_summary`).
   - Algoritmo de búsqueda rápida insensible a mayúsculas y acentos (`rust_match_note`) ejecutado en memoria nativa y coordinado con corrutinas reactivas en Kotlin (`Dispatchers.Default`).
   - Interfaz C FFI (`extern "C"`) de cero coste de abstracción y compatible con 64 bits (`arm64-v8a`, `x86_64`) y 32 bits (`armeabi-v7a`).

4. **Intérprete C Oficial de Lua 5.4.6 y Automatizaciones en Notas:**
   - Motor oficial de Lua compilado en C11 estático sin intermediarios pesados.
   - **Acceso directo desde el editor de notas:** Accesible tanto desde el menú de opciones de la barra superior como mediante el botón interactivo "🪄 Lua" en la barra de herramientas Markdown.
   - **Inyección de contexto en memoria:** El motor C inyecta las variables globales `content` (texto de la nota activa) y `title` (título de la nota) para permitir scripts reactivos y transformaciones instantáneas.
   - **Plantilla de Registro Diario:** Script nativo de Lua que genera con un solo toque un formato estructurado en Markdown con fecha y hora del sistema, listas de objetivos con casillas interactivas, notas rápidas y sección de reflexión.
   - **Editor de Script Personalizado:** Permite escribir o pegar scripts en Lua 5.4 con fuente monoespaciada, atajos de variables (`content`, `title`, `os.date()`, `return`), prueba aislada con visualización de salida/errores y tres modos de inserción en la nota activa (*En el cursor*, *Al final* o *Reemplazar todo*).

5. **Puente JNI en C++17:**
   - Enlace bidireccional entre la JVM/ART de Kotlin y las librerías nativas con manejo de excepciones y validación de tipos JNI.

6. **Personalización Tipográfica Granular (Por Nota y por Fragmento Inline):**
   - Selector interactivo accesible en el editor de notas y en la barra de herramientas (`MarkdownToolbar` con botón "Aa Fuente") con previsualización en tiempo real.
   - **Sin imposición global:** La tipografía no altera toda la aplicación de manera global; se aplica a nivel individual por nota o a fragmentos específicos de texto.
   - **Alcance dual:**
     * **Toda la nota:** Configura la familia tipográfica base de la nota activa (almacenada en Room y reflejada en su tarjeta de lista y editor).
     * **Texto seleccionado / Fragmento:** Envuelve o inserta etiquetas de formato `[font:id]...[/font]`, permitiendo combinar múltiples tipografías dentro del mismo documento Markdown.
   - 5 familias tipográficas nativas de Android: *Predeterminada (Sistema)*, *Sans-Serif Moderna*, *Serif Clásica (Editorial)*, *Monoespaciada (Código)* y *Cursiva Manuscrita*.
   - Persistencia 100% offline sin dependencias de red ni servicios externos.

7. **Ajustes de Apariencia y Personalización de Tema:**
   - Diálogo modal de ajustes dedicado (`SettingsDialog`) accesible mediante el botón de paleta en la barra superior.
   - **Modo de Tema:** Selección entre *Seguir el Sistema*, *Modo Claro* forzado y *Modo Oscuro* forzado.
   - **Material You (Color Dinámico):** Extracción nativa de la paleta de colores del fondo de pantalla del sistema en dispositivos Android 12+ (API 31+), con interruptor para activar/desactivar en vivo.
   - **Paletas de Acento Personalizadas:** Cuando Material You está desactivado o en dispositivos Android 8 a 11 (API 26-30), permite elegir entre 5 paletas de color con acentos cuidadosamente calibrados: *Obsidian Púrpura*, *Esmeralda Cripto*, *Ámbar Cálido*, *Azul Zafiro* y *Rosa Neón*.
   - **Tipografía Global:** Permite configurar la tipografía predeterminada de la interfaz completa junto al sistema granular de fuentes por nota.
   - Persistencia local inmediata mediante `SharedPreferences` reactivas sincronizadas en el `NotesViewModel`.

8. **Distribución Autónoma:**
   - Preparado para tiendas de aplicaciones de terceros (Uptodown, F-Droid, APK directo) cumpliendo con políticas de privacidad e integridad del sistema.

9. **Suite de Diagnóstico y Depuración en Vivo (In-App Debug Suite):**
   - **LeakCanary (v2.14):** Detección automática y en tiempo real de fugas de memoria (Memory Leaks) en vistas, actividades y componentes en ejecución sin necesidad de PC.
   - **HUD / Overlay Flotante de Rendimiento:** Indicador superpuesto y arrastrable que muestra FPS en vivo (vía `Choreographer`), memoria JVM usada y máxima, memoria del Heap Nativo (C++/Rust) y número de hilos concurrentes.
   - **Dashboard / Panel de Control de Depuración:**
     * *Rendimiento:* Métricas en tiempo real de FPS, consumo de memoria JVM vs. Heap Nativo, disparador manual de recolección de basura (GC) y estado de batería.
     * *Hilos Activos:* Inspección en tiempo real de cada hilo en ejecución (nombre, estado, prioridad y grupo).
     * *Visor de Logs del Proceso:* Lector integrado de Logcat con filtrado por nivel de severidad (Verbose a Error) y búsqueda de texto en vivo.
     * *Diagnóstico del Sistema & Nativos:* Inspección de hardware, ABI activa (`arm64-v8a`, `armeabi-v7a`, `x86_64`) y estado de los núcleos C++, Lua 5.4.6 y Rust.

---

10. **Importación y Exportación Universal (Markdown y Paquetes Vault con Firma Criptográfica):**
    - **Exportación Dual desde el Editor:**
      * *Markdown (.md):* Archivo de texto plano universal para llevar notas a Obsidian, Notion, Logseq, PC o editores externos sin bloqueos de plataforma.
      * *Paquete Vault (.zip firmado):* Archivo comprimido autónomo que contiene el documento `note.md`, metadatos completos en `vault_meta.json` (título, etiquetas, icono, tipografía individual) y la firma de autenticidad `signature.vault`.
    - **Firma Criptográfica Nativa (SHA-256):**
      * Cómputo nativo con SHA-256 y salt de integridad en bajo nivel, garantizando verificación en tiempo constante contra manipulaciones externas.
    - **Importación Flexible:**
      * Importa archivos `.md`, `.txt` o paquetes `.zip` directamente desde el explorador de archivos del sistema mediante Storage Access Framework (SAF).
      * Al importar un paquete de bóveda, valida automáticamente la firma nativa para garantizar la integridad y restaurar la tipografía y metadatos con total fidelidad.

---

## 👥 Gobernanza y Propuestas de la Comunidad (Filosofía Anti-Bloat)

Para evitar que Keeppr se convierta en una aplicación sobrecargada con herramientas innecesarias que degraden el rendimiento:

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
├── .github/workflows/            # Flujos de CI de GitHub Actions (build-debug, build-release, process-changelog-beta, override-commit)
├── Chanelog-beta.md               # Registro de capacidades y novedades de la versión Beta v0.1.0-b
├── commit_message.txt             # Mensaje estructurado en español sincronizado con git
├── setup_debug_keystore.sh       # Generador autónomo y limpio de debug.keystore
├── clean_native_artifacts.sh     # Script para purgar artefactos pesados (target, .cxx)
├── convert_jpg_to_webp.sh        # Utilidad de conversión de imágenes a WebP de alta fidelidad
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
