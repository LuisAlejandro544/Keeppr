package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val icon: String = "📝",
    val tags: String = "",
    val fontTheme: String = "default",
    val isPinned: Boolean = false,
    val isEncrypted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val tagList: List<String>
        get() = if (tags.isBlank()) emptyList() else tags.split(",").map { it.trim() }.filter { it.isNotBlank() }

    val wordCount: Int
        get() {
            if (content.isBlank()) return 0
            var count = 0
            var inWord = false
            val len = content.length
            for (i in 0 until len) {
                if (content[i].isWhitespace()) {
                    inWord = false
                } else if (!inWord) {
                    inWord = true
                    count++
                }
            }
            return count
        }

    val characterCount: Int
        get() = content.length

    val readingTimeMinutes: Int
        get() = maxOf(1, (wordCount / 200.0).toInt())
}
