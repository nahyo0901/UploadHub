package dev.nahyo0901.uploadhub.network.ftp.client

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

internal class DataConnection {
    private var socket: Socket? = null

    suspend fun openPassive(host: String, port: Int, timeoutMs: Int = 30_000) {
        withContext(Dispatchers.IO) {
            closeQuietly()
            socket = Socket()
            socket!!.soTimeout = timeoutMs
            socket!!.connect(InetSocketAddress(host, port), timeoutMs)
        }
    }

    fun inputStream(): InputStream = BufferedInputStream(socket!!.getInputStream())
    fun outputStream(): OutputStream = BufferedOutputStream(socket!!.getOutputStream())

    fun closeQuietly() {
        try {
            socket?.close()
        } catch (_: Throwable) {
        } finally {
            socket = null
        }
    }
}