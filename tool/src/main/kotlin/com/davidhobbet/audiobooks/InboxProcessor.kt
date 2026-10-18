package com.davidhobbet.audiobooks

import java.io.File

/**
 * Handles chapters uploaded through the LightOS Tool Manager. The Tool Manager
 * drops MP3s into files/shared/new_book_chapters; once the user names the book
 * on the phone, the chapters move to files/shared/audiobooks/<title>/.
 */
object InboxProcessor {

    const val NEW_BOOK_CHAPTERS_DIR = "new_book_chapters"
    const val SHARED_DIR = "shared"
    const val BOOKS_DIR = "audiobooks"

    /** MP3s waiting in the inbox, if any. */
    fun pendingChapters(filesDir: File): Int = inboxFiles(filesDir).size

    /** Moves the inbox chapters into a book named [title]. Returns how many moved. */
    fun fileAs(filesDir: File, title: String): Int {
        val cleaned = sanitize(title)
        if (cleaned.isBlank()) return 0
        val chapters = inboxFiles(filesDir)
        if (chapters.isEmpty()) return 0
        val shared = File(filesDir, SHARED_DIR)
        val bookDir = File(File(shared, BOOKS_DIR), cleaned).also { it.mkdirs() }
        var moved = 0
        chapters.forEach { file ->
            val target = File(bookDir, sanitize(file.name))
            val ok = file.renameTo(target) || runCatching {
                file.copyTo(target, overwrite = true)
                file.delete()
            }.isSuccess
            if (ok) moved++
        }
        return moved
    }

    private fun inboxFiles(filesDir: File): List<File> =
        File(File(filesDir, SHARED_DIR), NEW_BOOK_CHAPTERS_DIR)
            .listFiles { f: File -> f.isFile && f.extension.equals("mp3", ignoreCase = true) }
            .orEmpty()
            .toList()

    private fun sanitize(name: String): String =
        name.replace(Regex("""[/\\]"""), "-")
            .replace("..", ".")
            .filter { it.code >= 32 }
            .trim()
            .take(120)
}
