package com.davidhobbet.audiobooks

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first

data class BookProgress(val chapterIndex: Int, val positionMs: Long)

/** Persists per-book resume points and the global playback speed. */
class ProgressStore(private val dataStore: DataStore<Preferences>) {

    private val speedKey = floatPreferencesKey("playback.speed")
    private val lastBookKey = stringPreferencesKey("playback.lastBook")

    private fun progressKey(bookId: String) = stringPreferencesKey("$PROGRESS_PREFIX$bookId")

    suspend fun saveProgress(bookId: String, progress: BookProgress) {
        dataStore.edit { prefs ->
            prefs[progressKey(bookId)] = "${progress.chapterIndex}:${progress.positionMs}"
        }
    }

    suspend fun loadProgress(bookId: String): BookProgress? =
        parse(dataStore.data.first()[progressKey(bookId)])

    suspend fun loadAllProgress(): Map<String, BookProgress> {
        val prefs = dataStore.data.first()
        return prefs.asMap().entries.mapNotNull { (key, value) ->
            if (!key.name.startsWith(PROGRESS_PREFIX)) return@mapNotNull null
            val progress = parse(value as? String) ?: return@mapNotNull null
            key.name.removePrefix(PROGRESS_PREFIX) to progress
        }.toMap()
    }


    suspend fun saveLastBook(bookId: String) {
        dataStore.edit { prefs -> prefs[lastBookKey] = bookId }
    }

    suspend fun loadLastBook(): String? = dataStore.data.first()[lastBookKey]

    suspend fun saveSpeed(speed: Float) {
        dataStore.edit { prefs -> prefs[speedKey] = speed }
    }

    suspend fun loadSpeed(): Float = dataStore.data.first()[speedKey] ?: 1.0f

    private fun parse(raw: String?): BookProgress? {
        if (raw == null) return null
        val parts = raw.split(":")
        val chapter = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val position = parts.getOrNull(1)?.toLongOrNull() ?: 0L
        return BookProgress(chapter, position)
    }

    private companion object {
        const val PROGRESS_PREFIX = "progress."
    }
}
