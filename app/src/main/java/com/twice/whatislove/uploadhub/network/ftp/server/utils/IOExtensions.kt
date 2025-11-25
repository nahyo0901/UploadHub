package com.twice.whatislove.uploadhub.network.ftp.server.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.InputStream
import java.io.OutputStream
import kotlin.math.max

/**
 * IO helper extensions for the FTP server core.
 *
 * Place this file at:
 * app/src/main/java/com/twice/whatislove/uploadhub/network/ftp/server/utils/IOExtensions.kt
 */

/** Default buffer size used by copy helpers. */
private const val DEFAULT_BUFFER_SIZE = 8 * 1024

/**
 * Read all bytes from this InputStream in a way that works across Android API levels.
 */
fun InputStream.readAllBytesSafe(initialBufferSize: Int = DEFAULT_BUFFER_SIZE): ByteArray {
    val buffer = ByteArrayOutputStream(max(initialBufferSize, 1024))
    this.use { input ->
        val tmp = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = input.read(tmp)
            if (read <= 0) break
            buffer.write(tmp, 0, read)
        }
    }
    return buffer.toByteArray()
}

/**
 * Copy from this InputStream to [out] using a buffer.
 *
 * @return total number of bytes copied
 */
fun InputStream.copyToSafe(out: OutputStream, bufferSize: Int = DEFAULT_BUFFER_SIZE): Long {
    var bytesCopied = 0L
    val buffer = ByteArray(max(1, bufferSize))
    while (true) {
        val read = this.read(buffer)
        if (read <= 0) break
        out.write(buffer, 0, read)
        bytesCopied += read
    }
    return bytesCopied
}

/**
 * Copy with a progress callback invoked periodically with the cumulative bytes copied.
 *
 * The callback is invoked on the calling thread; if used from coroutines, call from Dispatchers.IO.
 */
fun InputStream.copyToWithProgress(
    out: OutputStream,
    bufferSize: Int = DEFAULT_BUFFER_SIZE,
    progressCallback: ((bytesCopied: Long) -> Unit)? = null,
    progressIntervalBytes: Long = 64 * 1024L
): Long {
    var bytesCopied = 0L
    val buffer = ByteArray(max(1, bufferSize))
    var nextReport = progressIntervalBytes
    while (true) {
        val read = this.read(buffer)
        if (read <= 0) break
        out.write(buffer, 0, read)
        bytesCopied += read
        if (progressCallback != null && bytesCopied >= nextReport) {
            progressCallback(bytesCopied)
            nextReport = bytesCopied + progressIntervalBytes
        }
    }
    progressCallback?.invoke(bytesCopied)
    return bytesCopied
}

/**
 * Suspendable copy that runs on [Dispatchers.IO].
 *
 * Use this from coroutine contexts to avoid blocking the main thread.
 */
suspend fun InputStream.copyToSuspend(
    out: OutputStream,
    bufferSize: Int = DEFAULT_BUFFER_SIZE
): Long = withContext(Dispatchers.IO) {
    this@copyToSuspend.copyToSafe(out, bufferSize)
}

/**
 * Suspendable readAllBytes that runs on [Dispatchers.IO].
 */
suspend fun InputStream.readAllBytesSuspend(initialBufferSize: Int = DEFAULT_BUFFER_SIZE): ByteArray =
    withContext(Dispatchers.IO) {
        this@readAllBytesSuspend.readAllBytesSafe(initialBufferSize)
    }

/**
 * Write a full byte array to the OutputStream and flush.
 *
 * Note: this will close the OutputStream after writing.
 */
fun OutputStream.writeAll(bytes: ByteArray) {
    this.use { out ->
        out.write(bytes)
        out.flush()
    }
}

/**
 * Close a Closeable quietly, swallowing exceptions.
 */
fun Closeable?.closeQuietly() {
    if (this == null) return
    try {
        this.close()
    } catch (_: Throwable) {
    }
}

/**
 * Normalize a simple path segment combination.
 *
 * This helper collapses redundant slashes and resolves '.' and '..' segments.
 * It returns a path that always starts with '/' unless the result is empty.
 *
 * Note: This is intentionally simple. The FileSystemAdapter implementation should
 * perform canonicalization and sandboxing against the server root.
 */
fun normalizePathSegment(base: String, child: String): String {
    if (child.isEmpty()) return base
    val combined = if (base.endsWith("/")) "$base$child" else "$base/$child"
    val parts = combined.split('/').filter { it.isNotEmpty() && it != "." }
    val stack = ArrayList<String>()
    for (p in parts) {
        when (p) {
            ".." -> if (stack.isNotEmpty()) stack.removeAt(stack.size - 1)
            else -> stack.add(p)
        }
    }
    return if (stack.isEmpty()) "/" else "/" + stack.joinToString("/")
}