package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.Note
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [Note::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN fontTheme TEXT NOT NULL DEFAULT 'default'")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN isEncrypted INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vault_notes_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    // Eliminado fallbackToDestructiveMigration para garantizar que jamás se borren
                    // los datos ni notas locales del usuario ante inconsistencias de versión.
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialNotes(database.noteDao())
                    }
                }
            }
        }

        private suspend fun populateInitialNotes(dao: NoteDao) {
            val welcomeNote = Note(
                title = "¡Bienvenido a Keeppr!",
                icon = "✨",
                tags = "bienvenida, guia, inicio",
                isPinned = true,
                content = """
# ✨ ¡Bienvenido a Keeppr!

**Keeppr** es tu espacio personal para capturar ideas, notas y listas con máxima privacidad, rapidez y control total en tu dispositivo móvil.

> [!NOTE]
> Keeppr funciona **100% offline**: tus notas nunca salen de tu teléfono, sin cuentas obligatorias, sin servidores intermedios y sin rastreadores.

---

## 🚀 ¿Qué puedes hacer por el momento?

### 📝 1. Editor de Markdown Enriquecido
- Escribe texto con formato **negrita**, *cursiva*, tachado y citas.
- Inserta encabezados (`# H1`, `## H2`, `### H3`) y listas ordenadas o con viñetas.
- Alterna instantáneamente con el botón superior entre el modo de **Edición** y la **Vista Previa**.

### ✅ 2. Listas de Tareas Interactivas
- Crea checklists con formato `- [ ] Tarea pendiente` y `- [x] Tarea completada`.
- ¡Márcalas y desmárcalas directamente tocando la casilla en la **Vista Previa**!

### 🔒 3. Cifrado y Privacidad de Notas
- Protege notas confidenciales asignando una contraseña individual desde el menú superior del editor.
- Seguridad de alto nivel impulsada por el núcleo nativo (**AES-256-CBC** con derivación de claves **PBKDF2-HMAC-SHA256**).

### 🔍 4. Búsqueda y Organización Rápida
- Busca al instante por título, contenido o etiquetas con filtrado acelerado por **Rust nativo**.
- Organiza tu flujo de trabajo mediante etiquetas (#tags) y fija notas prioritarias con el botón 📌.

### 📜 5. Automatización con Scripts Lua
- Ejecuta scripts en **Lua 5.4.6** para transformar texto, procesar plantillas o automatizar tareas repetitivas sobre tu nota activa.

### 📦 6. Copias de Seguridad y Portabilidad
- Exporta e importa tus notas como paquetes seguros (`.vault`) o archivos Markdown estándar para respaldar tu información localmente.

---

> [!TIP]
> Toca el ícono del emoji superior en cualquier momento para personalizar la identidad visual de tu nota.
                """.trimIndent()
            )

            dao.insertNote(welcomeNote)
        }
    }
}
