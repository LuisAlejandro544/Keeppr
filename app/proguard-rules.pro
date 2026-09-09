# ==============================================================================
# Reglas de Optimización y Ofuscación de R8 / ProGuard para Keeppr (Producción / Release)
# ==============================================================================

# ------------------------------------------------------------------------------
# 1. OPTIMIZACIÓN AGRESIVA Y REDUCCIÓN DE CÓDIGO
# ------------------------------------------------------------------------------
# Ejecuta múltiples pasadas de optimización para inlining profundo y fusión de clases
-optimizationpasses 5

# Permite relajar la visibilidad de clases y métodos para maximizar inlining y dead code elimination
-allowaccessmodification

# Empaquetado agresivo: traslada todas las clases ofuscadas al paquete raíz para reducir strings en DEX
-repackageclasses ''

# Sobrecarga agresiva de nombres de métodos para minimizar la tabla de métodos DEX
-overloadaggressively

# Oculta nombres de archivos fuente originales para reducir tamaño de strings
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

# ------------------------------------------------------------------------------
# 2. ELIMINACIÓN DE LLAMADAS A LOGS DE DEPURACIÓN EN RELEASE
# ------------------------------------------------------------------------------
# Remueve llamadas a Log.v, Log.d y Log.i en tiempo de compilación para ahorrar espacio en .dex y ciclos de CPU
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# ------------------------------------------------------------------------------
# 3. CAPA NATIVA (C++, RUST, LUA 5.4.6 Y JNI) - CRÍTICO
# ------------------------------------------------------------------------------
# No renombrar ningún método nativo en ninguna clase para evitar UnsatisfiedLinkError en JNI
-keepclasseswithmembernames class * {
    native <methods>;
}

# Mantener intactas las clases y métodos del motor nativo
-keep class com.example.native.** { *; }
-keepclassmembers class com.example.native.** { *; }

# ------------------------------------------------------------------------------
# 4. MODELOS DE DATOS Y ROOM DATABASE (SQLITE LOCAL)
# ------------------------------------------------------------------------------
# Mantener modelos de datos de notas y paquetes
-keep class com.example.data.model.** { *; }
-keepclassmembers class com.example.data.model.** { *; }

# Mantener clases base de Room y DAOs
-keep class androidx.room.RoomDatabase
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public abstract <methods>;
}
-dontwarn androidx.room.**

# ------------------------------------------------------------------------------
# 5. JETPACK COMPOSE Y KOTLIN COROUTINES
# ------------------------------------------------------------------------------
-keep class androidx.compose.runtime.** { *; }
-dontwarn kotlinx.coroutines.**
-dontwarn java.lang.invoke.**

# ------------------------------------------------------------------------------
# 6. OKHTTP Y NETWORKING (PROVEEDORES SSL OPCIONALES)
# ------------------------------------------------------------------------------
-dontwarn org.bouncycastle.jsse.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
-dontwarn okhttp3.internal.platform.**


