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
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

object CatboxApi {

    private const val CATBOX_URL = "https://catbox.moe/user/api.php"

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .callTimeout(5, TimeUnit.MINUTES)
        .build()

    /**
     * Upload a single file to Catbox
     */
    suspend fun uploadFile(
        file: File,
        progress: ((sent: Long, total: Long) -> Unit)? = null
    ): String = withContext(Dispatchers.IO) {

        if (!file.exists() || !file.isFile) {
            throw IOException("File not found: ${file.absolutePath}")
        }

        val baseBody = file.asRequestBody("application/octet-stream".toMediaTypeOrNull())

        val requestBody = if (progress != null) {
            CountingRequestBody(baseBody) { sent, total ->
                progress(sent, total)
            }
        } else {
            baseBody
        }

        val multipart = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("reqtype", "fileupload")
            .addFormDataPart("fileToUpload", file.name, requestBody)
            .build()

        val request = Request.Builder()
            .url(CATBOX_URL)
            .post(multipart)
            .build()

        client.newCall(request).execute().use { response ->
            handleResponse(response)
        }
    }

    /**
     * Upload multiple files in one request
     */
    suspend fun uploadFiles(
        files: List<File>,
        progress: ((sent: Long, total: Long) -> Unit)? = null
    ): String = withContext(Dispatchers.IO) {

        if (files.isEmpty()) throw IllegalArgumentException("No files provided")

        files.forEach {
            if (!it.exists() || !it.isFile) {
                throw IOException("File not found: ${it.absolutePath}")
            }
        }

        val builder = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("reqtype", "fileupload")

        files.forEach { file ->
            builder.addFormDataPart(
                "fileToUpload",
                file.name,
                file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
            )
        }

        val multipart = builder.build()

        val requestBody = if (progress != null) {
            CountingRequestBody(multipart) { sent, total ->
                progress(sent, total)
            }
        } else {
            multipart
        }

        val request = Request.Builder()
            .url(CATBOX_URL)
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            handleResponse(response)
        }
    }

    private fun handleResponse(response: Response): String {
        if (!response.isSuccessful) {
            throw IOException("Upload failed: HTTP ${response.code}")
        }

        val body = response.body?.string()?.trim().orEmpty()
        if (body.isEmpty()) throw IOException("Empty response from Catbox")

        return body
    }

    /**
     * RequestBody wrapper that reports upload progress.
     *
     * IMPORTANT:
     * We wrap the BufferedSink directly — NO sink.sink() calls.
     */
    private class CountingRequestBody(
        private val delegate: RequestBody,
        private val onProgress: (bytesWritten: Long, contentLength: Long) -> Unit
    ) : RequestBody() {

        override fun contentType() = delegate.contentType()

        override fun contentLength(): Long {
            return try {
                delegate.contentLength()
            } catch (_: IOException) {
                -1L
            }
        }

        override fun writeTo(sink: okio.BufferedSink) {
            val countingSink = object : ForwardingSink(sink) {
                var bytesWritten = 0L
                val total = contentLength()

                override fun write(source: Buffer, byteCount: Long) {
                    super.write(source, byteCount)
                    bytesWritten += byteCount
                    onProgress(bytesWritten, total)
                }
            }.buffer()

            delegate.writeTo(countingSink)
            countingSink.flush()
        }
    }
}