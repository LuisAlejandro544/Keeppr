package com.example.data.repository

import android.util.LruCache
import com.example.data.local.NoteDao
import com.example.data.model.Note
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach

class NoteRepository(private val noteDao: NoteDao) {

    // Caché en memoria RAM de primer nivel (máx 60 notas recientes).
    // Acelera la apertura repetitiva de notas y reduce lecturas de disco innecesarias a SQLite.
    private val memoryCache = LruCache<Long, Note>(60)

    val allNotes: Flow<List<Note>> = noteDao.getAllNotes()
        .onEach { list ->
            // Pre-calentar o sincronizar la caché en memoria con los datos más recientes
            list.take(30).forEach { note ->
                memoryCache.put(note.id, note)
            }
        }

    fun getNoteById(id: Long): Flow<Note?> = noteDao.getNoteById(id)
        .onEach { note ->
            if (note != null) {
                memoryCache.put(note.id, note)
            } else {
                memoryCache.remove(id)
            }
        }

    fun getCachedNote(id: Long): Note? = memoryCache.get(id)

    fun searchNotes(query: String): Flow<List<Note>> = noteDao.searchNotes(query)

    suspend fun insert(note: Note): Long {
        val newId = noteDao.insertNote(note)
        memoryCache.put(newId, note.copy(id = newId))
        return newId
    }

    suspend fun update(note: Note) {
        memoryCache.put(note.id, note)
        noteDao.updateNote(note)
    }

    suspend fun delete(note: Note) {
        memoryCache.remove(note.id)
        noteDao.deleteNote(note)
    }

    suspend fun deleteById(id: Long) {
        memoryCache.remove(id)
        noteDao.deleteNoteById(id)
    }

    fun clearCache() {
        memoryCache.evictAll()
    }
}
