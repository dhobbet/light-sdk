package com.thelightphone.sdk.server.toolmanager

import com.thelightphone.toolmanager.FormSubmission
import com.thelightphone.toolmanager.datatree.CustomDataTree
import com.thelightphone.toolmanager.datatree.WriteCheck
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Path

// Pairs with the tool manager's FormSpec. The UI serializes a filled-out form as a FormSubmission
// and uploads it as a json file to the spec's path - this tree never stores that file, it just
// decodes it and hands the responses (keyed by FormItem.id) to onSubmit. A failed onSubmit fails
// the upload, which the UI reports as a failed submission.
abstract class FormDataTree(
    private val maxSubmissionBytes: Int = 64 * 1024,
) : CustomDataTree() {

    abstract suspend fun onSubmit(responses: Map<String, String>): Result<Unit>

    override suspend fun getBytes(filePath: Path): Result<InputStream> =
        Result.failure(SecurityException("Cannot read from form data tree"))

    override suspend fun checkWrite(filePath: Path): WriteCheck = WriteCheck.Safe

    override suspend fun <T> writeBytes(filePath: Path, block: suspend (OutputStream) -> T): Result<T> = runCatching {
        val buffer = BoundedByteArrayOutputStream(maxSubmissionBytes)
        val blockResult = block(buffer)
        val submission = formJson.decodeFromString(FormSubmission.serializer(), buffer.toString(Charsets.UTF_8))
        onSubmit(submission.responses.associate { it.item.id to it.response }).getOrThrow()
        blockResult
    }

    private class BoundedByteArrayOutputStream(private val limit: Int) : ByteArrayOutputStream() {
        override fun write(b: Int) {
            checkLimit(1)
            super.write(b)
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            checkLimit(len)
            super.write(b, off, len)
        }

        private fun checkLimit(incoming: Int) {
            if (size() + incoming > limit) throw IOException("form submission exceeds $limit bytes")
        }
    }

    private companion object {
        val formJson = Json { ignoreUnknownKeys = true }
    }
}
