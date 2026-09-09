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
                    .fallbackToDestructiveMigration()
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
            val note1 = Note(
                title = "Bienvenido a VaultNotes",
                icon = "✨",
                tags = "guia, obsidian, notion",
                isPinned = true,
                content = """
# ✨ Bienvenido a VaultNotes

Combina la potencia de **Markdown** con la estructura limpia de bloques estilo **Notion**.

> [!NOTE]
> Esta app está diseñada para tomar notas con rapidez y flexibilidad. Puedes editar el texto plano y alternar a **Vista Previa** con un solo toque.

---

## 🎯 Características Principales

- [x] Soporte completo de encabezados (H1, H2, H3)
- [x] Checklists interactivas (¡márcalas directamente en Vista Previa!)
- [ ] Bloques destacados (Callouts) informativos
- [ ] Resaltado de código y citas elegantes

### 💡 Ejemplo de Bloques Callout

> [!TIP]
> Puedes cambiar el emoji de cada nota tocando el ícono superior en el editor.

> [!WARNING]
> Recuerda revisar la barra de accesos rápidos para insertar formatos rápidamente en tu teléfono.

### 💻 Bloque de Código
```kotlin
fun saludar(nombre: String): String {
    return "¡Hola, " + nombre + "! Bienvenido a VaultNotes"
}
```

> "El conocimiento se construye nota a nota, conectando pensamientos."
                """.trimIndent()
            )

            val note2 = Note(
                title = "Mi Lista de Tareas y Objetivos",
                icon = "🎯",
                tags = "personal, metas",
                isPinned = false,
                content = """
# 🎯 Objetivos de la Semana

Revisa tus tareas y márcalas al completarlas:

### 🚀 Tareas Clave
- [x] Explorar el editor de Markdown
- [ ] Crear mi primera nota personalizada
- [ ] Probar el cambio entre Vista Previa y Edición
- [ ] Organizar notas con #etiquetas

> [!INFO]
> Al tocar una casilla en **Vista Previa**, se actualizará automáticamente tu texto Markdown en tiempo real.
                """.trimIndent()
            )

            val note3 = Note(
                title = "Ideas para Proyectos",
                icon = "💡",
                tags = "ideas, tech",
                isPinned = false,
                content = """
# 💡 Lluvia de Ideas

Notas rápidas para futuros proyectos y desarrollos.

### 📌 Conceptos
- **Arquitectura Limpia**: Mantener separación entre datos y UI.
- **Modo Offline**: Almacenamiento local ultrarrápido con SQLite y Room.
- **Diseño Ergonómico**: Botones accesibles para usar con una sola mano en el móvil.

---
*Creado en VaultNotes*
                """.trimIndent()
            )

            dao.insertNote(note1)
            dao.insertNote(note2)
            dao.insertNote(note3)
        }
    }
}
