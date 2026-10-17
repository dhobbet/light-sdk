package com.davidhobbet.audiobooks

import android.util.Log
import io.ktor.http.ContentType
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyAndClose
import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * A small web server (Ktor is on the SDK allow-list) for loading books onto
 * the phone: open the printed address from a computer on the same wi-fi
 * network and upload one book (title + chapter MP3s) at a time.
 */
class UploadServer(
    private val library: AudiobookLibrary,
    private val onLibraryChanged: () -> Unit,
) {

    private var server: EmbeddedServer<*, *>? = null

    fun start() {
        if (server != null) return
        server = embeddedServer(Netty, port = PORT) {
            routing {
                get("/") {
                    call.respondText(indexHtml(), ContentType.Text.Html)
                }
                post("/upload") {
                    var bookTitle = "Untitled"
                    var saved = 0
                    call.receiveMultipart().forEachPart { part ->
                        when (part) {
                            is PartData.FormItem -> {
                                if (part.name == "book") {
                                    val cleaned = sanitize(part.value)
                                    if (cleaned.isNotBlank()) bookTitle = cleaned
                                }
                            }
                            is PartData.FileItem -> {
                                val fileName = sanitize(part.originalFileName ?: "")
                                if (fileName.endsWith(".mp3", ignoreCase = true)) {
                                    val bookDir = File(library.root, bookTitle).also { it.mkdirs() }
                                    val target = File(bookDir, fileName)
                                    part.provider().copyAndClose(target.writeChannel())
                                    saved++
                                }
                            }
                            else -> Unit
                        }
                        part.dispose()
                    }
                    Log.d(TAG, "Uploaded $saved chapter(s) to '$bookTitle'")
                    onLibraryChanged()
                    call.respondRedirect("/?uploaded=$saved")
                }
            }
        }.also { it.start(wait = false) }
    }

    fun stop() {
        server?.stop(gracePeriodMillis = 250, timeoutMillis = 1_000)
        server = null
    }

    /** wi-fi addresses to show the user, e.g. "http://192.168.1.23:8765" */
    fun addresses(): List<String> =
        runCatching {
            NetworkInterface.getNetworkInterfaces().toList()
                .filter { it.isUp && !it.isLoopback }
                .flatMap { it.inetAddresses.toList() }
                .filterIsInstance<Inet4Address>()
                .filter { it.isSiteLocalAddress }
                .map { "http://${it.hostAddress}:$PORT" }
        }.getOrDefault(emptyList())

    private fun indexHtml(): String {
        val books = library.scan()
        val bookList = if (books.isEmpty()) {
            "<li>No books on the phone yet.</li>"
        } else {
            books.joinToString("") {
                "<li>${escape(it.title)} &mdash; ${it.chapters.size} chapter(s)</li>"
            }
        }
        return """
            <!doctype html>
            <html>
            <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <title>Audiobooks &middot; Light Phone</title>
            <style>
                body { font-family: monospace; max-width: 34rem; margin: 3rem auto; padding: 0 1rem; color: #111; }
                h1 { letter-spacing: 0.2em; font-size: 1.1rem; }
                h2 { letter-spacing: 0.1em; font-size: 0.9rem; margin-top: 2.5rem; }
                label { display: block; margin: 1.2rem 0 0.3rem; }
                input, button { font-family: monospace; font-size: 1rem; }
                input[type=text] { width: 100%; padding: 0.4rem; border: 1px solid #111; }
                button { margin-top: 1.2rem; padding: 0.5rem 1.5rem; border: 1px solid #111; background: #111; color: #fff; cursor: pointer; }
                ul { padding-left: 1.2rem; }
            </style>
            </head>
            <body>
            <h1>AUDIOBOOKS</h1>
            <p>Upload one book at a time. Chapters play in filename order,
            so name the files so they sort correctly (01.mp3, 02.mp3, &hellip;).</p>
            <form method="post" action="/upload" enctype="multipart/form-data">
                <label for="book">Book title</label>
                <input type="text" id="book" name="book" required>
                <label for="files">Chapter MP3s</label>
                <input type="file" id="files" name="files" accept=".mp3,audio/mpeg" multiple required>
                <br>
                <button type="submit">Upload</button>
            </form>
            <h2>ON THE PHONE</h2>
            <ul>$bookList</ul>
            </body>
            </html>
        """.trimIndent()
    }

    private fun sanitize(name: String): String =
        name.replace(Regex("""[/\\]"""), "-")
            .replace("..", ".")
            .filter { it.code >= 32 }
            .trim()
            .take(120)

    private fun escape(text: String): String =
        text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    companion object {
        private const val TAG = "UploadServer"
        const val PORT = 8765
    }
}
