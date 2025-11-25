package com.twice.whatislove.uploadhub.catbox

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.Response
import okio.Buffer
import okio.ForwardingSink
import okio.buffer
import okio.sink
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

object CatboxApi {
    private const val CATBOX_URL = "https://catbox.moe/user/api.php"

    /** Shared OkHttp client configured with sensible timeouts. */
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .callTimeout(5, TimeUnit.MINUTES)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Upload a single file to Catbox.
     *
     * @param file file to upload
     * @param progress optional callback invoked on the calling coroutine thread with bytesSent and totalBytes
     * @return the server response body as string (Catbox returns the uploaded file URL on success)
     * @throws IOException on network or IO errors
     */
    suspend fun uploadFile(
        file: File,
        progress: ((bytesSent: Long, totalBytes: Long) -> Unit)? = null
    ): String = withContext(Dispatchers.IO) {
        if (!file.exists() || !file.isFile) throw IOException("File not found: ${file.absolutePath}")

        val fileRequestBody = file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
        val countingBody = if (progress != null) {
            CountingRequestBody(fileRequestBody) { bytesWritten, contentLength ->
                progress(bytesWritten, contentLength)
            }
        } else {
            fileRequestBody
        }

        val body = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("reqtype", "fileupload")
            .addFormDataPart("fileToUpload", file.name, countingBody)
            .build()

        val req = Request.Builder().url(CATBOX_URL).post(body).build()

        client.newCall(req).execute().use { resp ->
            handleResponse(resp)
        }
    }

    /**
     * Upload multiple files in a single request. Returns the server response string.
     *
     * Catbox accepts multiple files in one request; the response is typically newline-separated URLs.
     */
    suspend fun uploadFiles(
        files: List<File>,
        progress: ((bytesSent: Long, totalBytes: Long) -> Unit)? = null
    ): String = withContext(Dispatchers.IO) {
        if (files.isEmpty()) throw IllegalArgumentException("No files provided")
        files.forEach { if (!it.exists() || !it.isFile) throw IOException("File not found: ${it.absolutePath}") }

        // Build multipart with optional counting wrapper around the whole multipart body
        val builder = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("reqtype", "fileupload")

        files.forEach { file ->
            builder.addFormDataPart(
                "fileToUpload",
                file.name,
                file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
            )
        }

        val multipart = builder.build()
        val countingBody = if (progress != null) {
            CountingRequestBody(multipart) { bytesWritten, contentLength ->
                progress(bytesWritten, contentLength)
            }
        } else {
            multipart
        }

        val req = Request.Builder().url(CATBOX_URL).post(countingBody).build()
        client.newCall(req).execute().use { resp ->
            handleResponse(resp)
        }
    }

    /**
     * Helper to validate response and return body string or throw IOException.
     */
    private fun handleResponse(resp: Response): String {
        if (!resp.isSuccessful) {
            throw IOException("Upload failed: HTTP ${resp.code}")
        }
        val body = resp.body?.string() ?: ""
        if (body.isBlank()) throw IOException("Empty response from server")
        return body.trim()
    }

    /**
     * RequestBody wrapper that reports progress.
     *
     * The callback is invoked on the thread performing the upload (Dispatchers.IO when used above).
     */
    private class CountingRequestBody(
        private val delegate: RequestBody,
        private val onProgress: (bytesWritten: Long, contentLength: Long) -> Unit
    ) : RequestBody() {

        override fun contentType() = delegate.contentType()

        override fun contentLength(): Long {
            return try {
                delegate.contentLength()
            } catch (e: IOException) {
                -1L
            }
        }

        override fun writeTo(sink: okio.BufferedSink) {
            val countingSink = object : ForwardingSink(sink.sink()) {
                var bytesWritten = 0L
                val total = contentLength()

                override fun write(source: Buffer, byteCount: Long) {
                    super.write(source, byteCount)
                    bytesWritten += byteCount
                    onProgress(bytesWritten, if (total >= 0) total else -1L)
                }
            }.buffer()

            delegate.writeTo(countingSink)
            countingSink.flush()
        }
    }
}