package com.davidhobbet.audiobooks

import java.io.File

data class Chapter(
    val file: File,
    val index: Int,
    val title: String,
)

data class Book(
    val id: String,
    val title: String,
    val chapters: List<Chapter>,
)

/**
 * Books live in the tool's private files directory:
 *   files/audiobooks/<Book Title>/<chapter>.mp3
 * One folder per book, one MP3 per chapter, chapters ordered by filename
 * (numerically aware, so "Chapter 2" sorts before "Chapter 10").
 */
class AudiobookLibrary(filesDir: File) {

    val root: File = File(filesDir, "audiobooks").also { it.mkdirs() }

    fun scan(): List<Book> =
        root.listFiles { f: File -> f.isDirectory }
            ?.sortedWith(compareBy(naturalNameOrder) { it.name })
            ?.mapNotNull { dir ->
                val mp3s = dir.listFiles { f: File ->
                    f.isFile && f.extension.equals("mp3", ignoreCase = true)
                }?.sortedWith(compareBy(naturalNameOrder) { it.name }).orEmpty()
                if (mp3s.isEmpty()) null
                else Book(
                    id = dir.name,
                    title = dir.name,
                    chapters = mp3s.mapIndexed { i, f -> Chapter(f, i, chapterTitle(f)) },
                )
            }
            .orEmpty()

    private fun chapterTitle(file: File): String =
        file.nameWithoutExtension.replace('_', ' ').trim()

    companion object {
        /** Filename comparator that orders embedded numbers numerically. */
        val naturalNameOrder: Comparator<String> = Comparator { a, b ->
            var i = 0
            var j = 0
            while (i < a.length && j < b.length) {
                val ca = a[i]
                val cb = b[j]
                if (ca.isDigit() && cb.isDigit()) {
                    var endA = i
                    while (endA < a.length && a[endA].isDigit()) endA++
                    var endB = j
                    while (endB < b.length && b[endB].isDigit()) endB++
                    val numA = a.substring(i, endA).trimStart('0')
                    val numB = b.substring(j, endB).trimStart('0')
                    val cmp = if (numA.length != numB.length) {
                        numA.length - numB.length
                    } else {
                        numA.compareTo(numB)
                    }
                    if (cmp != 0) return@Comparator cmp
                    i = endA
                    j = endB
                } else {
                    val cmp = ca.lowercaseChar().compareTo(cb.lowercaseChar())
                    if (cmp != 0) return@Comparator cmp
                    i++
                    j++
                }
            }
            (a.length - i) - (b.length - j)
        }
    }
}
