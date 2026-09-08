# Hoja de Ruta (ROADMAP) - VaultNotes

Plan de evolución técnica y funcional para la aplicación VaultNotes.

---

## 📍 Fase 1: Arquitectura Base y Motor Nativo (Completada ✅)

- [x] Configuración de Android API 26+ (Android 8.0 Oreo) como versión mínima para compatibilidad NDK y Rust moderno.
- [x] Arquitectura MVVM reactiva con Jetpack Compose y Material Design 3.
- [x] Base de datos local Room con entidades de notas, categorías, etiquetas y búsqueda indexada.
- [x] Integración de toolchains nativos simultáneos en Gradle:
  - C++17 mediante CMake y JNI (`libvaultnotes_native.so`).
  - Rust mediante Cargo con soporte `aarch64-linux-android` y `x86_64-linux-android` (`libvaultnotes_rust.a`).
  - Motor C oficial de Lua 5.4.6 compilado estáticamente sin wrappers de terceros.
- [x] Diálogo interactivo en la UI para inspección de motores y ejecución dinámica de scripts Lua.
- [x] Script de mantenimiento automatizado `clean_native_artifacts.sh` para purgar carpetas `target`, `.cxx` y archivos residuales.

---

## 📍 Fase 2: Cifrado por Hardware y Bóveda Segura (En Curso 🔄)

- [ ] Integración de cifrado autenticado AES-GCM-256 o ChaCha20-Poly1305 ejecutado directamente en el núcleo de Rust.
- [ ] Derivación de claves de alta seguridad con Argon2id implementada en Rust para proteger notas confidenciales.
- [ ] Autenticación biométrica nativa (huella / reconocimiento facial) usando `androidx.biometric`.
- [ ] Modo "Bóveda Oculta": partición protegida por PIN independiente dentro de la base de datos Room.

---

## 📍 Fase 3: Automatizaciones y Extensiones en Lua (Próximamente ⏳)

- [ ] Sandbox seguro de Lua para transformar texto de notas (resúmenes, formateo Markdown, reemplazo regex).
- [ ] Sistema de plantillas configurables por el usuario impulsadas por scripts de Lua.
- [ ] Procesamiento de notas en segundo plano mediante `WorkManager` con llamadas al motor Lua.

---

## 📍 Fase 4: Respaldo Autónomo y Portabilidad (Planeada 📅)

- [ ] Exportación e importación de notas en archivos planos comprimidos (`.zip` / `.tar.gz`) con checksums SHA-256 calculados en Rust.
- [ ] Sincronización punto a punto (P2P) local mediante red local WiFi/Hotspot sin intermediarios ni servidores en la nube.
- [ ] Soporte para visualización y edición en Markdown enriquecido con renderizado nativo.

---

## 📍 Fase 5: Distribución y Optimización para Tiendas de Terceros (Planeada 📅)

- [ ] Empaquetado APK standalone sin dependencias de Google Play Services, optimizado para plataformas como Uptodown, F-Droid y APKMirror.
- [ ] Pruebas de compatibilidad en dispositivos Android Go y procesadores de gama baja a media.
- [ ] Documentación para empaquetadores comunitarios e integradores de código abierto.
