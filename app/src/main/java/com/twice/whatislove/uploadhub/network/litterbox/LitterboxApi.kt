package com.twice.whatislove.uploadhub.litterbox

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.Buffer
import okio.ForwardingSink
import okio.buffer
import okio.sink
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Coroutine-friendly Litterbox (catbox.moe internal) uploader.
 *
 * - Suspendable API (call from coroutine scope).
 * - Optional progress callback reporting bytesSent and totalBytes (total may be -1 if unknown).
 * - Uploads single or multiple files in one request.
 *
 * Endpoint: https://litterbox.catbox.moe/resources/internals/api.php
 */
object LitterboxApi {
    private const val API_URL = "https://litterbox.catbox.moe/resources/internals/api.php"

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .callTimeout(5, TimeUnit.MINUTES)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Upload a single file.
     *
     * @param file file to upload
     * @param time retention time string accepted by the API (e.g., "72h")
     * @param progress optional callback invoked on the calling coroutine thread with (bytesSent, totalBytes)
     * @return server response body (usually a URL)
     * @throws IOException on network or IO errors
     */
    suspend fun upload(
        file: File,
        time: String = "72h",
        progress: ((bytesSent: Long, totalBytes: Long) -> Unit)? = null
    ): String = withContext(Dispatchers.IO) {
        if (!file.exists() || !file.isFile) throw IOException("File not found: ${file.absolutePath}")

        val fileBody = file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
        val requestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("reqtype", "fileupload")
            .addFormDataPart("time", time)
            .addFormDataPart("fileToUpload", file.name, fileBody)
            .build()

        val bodyToSend = if (progress != null) CountingRequestBody(requestBody) { written, total ->
            progress(written, total)
        } else requestBody

        val req = Request.Builder().url(API_URL).post(bodyToSend).build()
        client.newCall(req).execute().use { resp ->
            handleResponse(resp)
        }
    }

    /**
     * Upload multiple files in a single request.
     *
     * @param files list of files to upload
     * @param time retention time string
     * @param progress optional callback invoked with (bytesSent, totalBytes)
     * @return server response body (often newline-separated URLs)
     */
    suspend fun uploadMultiple(
        files: List<File>,
        time: String = "72h",
        progress: ((bytesSent: Long, totalBytes: Long) -> Unit)? = null
    ): String = withContext(Dispatchers.IO) {
        if (files.isEmpty()) throw IllegalArgumentException("No files provided")
        files.forEach { if (!it.exists() || !it.isFile) throw IOException("File not found: ${it.absolutePath}") }

        val builder = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("reqtype", "fileupload")
            .addFormDataPart("time", time)

        files.forEach { file ->
            builder.addFormDataPart(
                "fileToUpload",
                file.name,
                file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
            )
        }

        val multipart = builder.build()
        val bodyToSend = if (progress != null) CountingRequestBody(multipart) { written, total ->
            progress(written, total)
        } else multipart

        val req = Request.Builder().url(API_URL).post(bodyToSend).build()
        client.newCall(req).execute().use { resp ->
            handleResponse(resp)
        }
    }

    /**
     * Validate response and return trimmed body string or throw IOException.
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
     * RequestBody wrapper that reports progress while writing.
     *
     * Callback is invoked on the thread performing the upload (Dispatchers.IO when used above).
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
            val total = contentLength()
            val forwarding = object : ForwardingSink(sink.sink()) {
                var bytesWritten = 0L
                override fun write(source: Buffer, byteCount: Long) {
                    super.write(source, byteCount)
                    bytesWritten += byteCount
                    onProgress(bytesWritten, if (total >= 0) total else -1L)
                }
            }.buffer()

            delegate.writeTo(forwarding)
            forwarding.flush()
        }
    }
}