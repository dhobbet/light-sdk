package com.davidhobbet.audiobooks

import com.thelightphone.sdk.EntryPoint
import com.thelightphone.sdk.LightEntryPoint
import com.thelightphone.toolmanager.ClientLeafNode
import com.thelightphone.toolmanager.ClientToolManifest
import com.thelightphone.toolmanager.FileBrowserSpec

/**
 * Tool Manager integration: lets users load audiobooks from a computer through
 * LightOS's own browser-based file transfer, with no network code in this tool.
 * Chapters for one book are uploaded into an inbox; the next time Audiobooks
 * opens on the phone, it offers to name the new book and files the chapters.
 */
@EntryPoint
object ToolEntryPoint : LightEntryPoint {

    override fun getToolManagerManifest(): ClientToolManifest = ClientToolManifest(
        title = "Audiobooks",
        roots = listOf(
            ClientLeafNode(
                FileBrowserSpec(
                    label = "NEW BOOK CHAPTERS",
                    path = InboxProcessor.NEW_BOOK_CHAPTERS_DIR,
                    headerText = "Upload the MP3 chapters of one book here. " +
                        "Chapters play in filename order (01.mp3, 02.mp3, ...). " +
                        "Then open Audiobooks on the phone to name the book.",
                )
            ),
        ),
    )
}
