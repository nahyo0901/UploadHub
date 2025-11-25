package com.twice.whatislove.uploadhub.network.ftp.server.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.InputStream
import java.io.OutputStream
import kotlin.math.max
import kotlin.math.min

/**
 * Small collection of IO helper extensions used by the FTP server core.
 *
 * Place this file under:
 * app/src/main/java/com/twice/whatislove/uploadhub/network/ftp/server/utils/IOExtensions.kt
 */

/**
 * Read all bytes from this InputStream in a way that works across Android API levels.
 */
fun InputStream.readAllBytesSafe(initialBufferSize: Int = 8 * 1024): ByteArray {
    val buffer = ByteArrayOutputStream(max(initialBufferSize, 1024))
    this.use { input ->
        val tmp = ByteArray(8 * 1024)
        var read: Int
        while (true) {
            read = input.read(tmp)
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
    var read: Int
    while (true) {
        read = this.read(buffer)
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
    var read: Int
    var nextReport = progressIntervalBytes
    while (true) {
        read = this.read(buffer)
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
suspend fun InputStream.readAllBytesSuspend(initialBufferSize: Int = 8 * 1024): ByteArray =
    withContext(Dispatchers.IO) {
        this@readAllBytesSuspend.readAllBytesSafe(initialBufferSize)
    }

/**
 * Write a full byte array to the OutputStream and flush.
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
 * Helper to safely resolve a child path segment into a normalized string path.
 *
 * This is intentionally simple and does not attempt to enforce sandboxing; the
 * FileSystemAdapter implementation should ensure canonicalization and sandboxing.
 */
fun normalizePathSegment(base: String, child: String): String {
    if (child.isEmpty()) return base
    val combined = if (base.endsWith("/")) "$base$child" else "$base/$child"
    // collapse redundant slashes and simple ./ segments
    val parts = combined.split('/').filter { it.isNotEmpty() && it != "." }
    val stack = ArrayList<String>()
    for (p in parts) {
        when (p) {
            ".." -> if (stack.isNotEmpty()) stack.removeAt(stack.size - 1)
            else -> stack.add(p)
        }
    }
    return "/" + stack.joinToString("/")
}

/**
 * Default buffer size used by copy helpers.
 */
private const val DEFAULT_BUFFER_SIZE = 8 * 1024