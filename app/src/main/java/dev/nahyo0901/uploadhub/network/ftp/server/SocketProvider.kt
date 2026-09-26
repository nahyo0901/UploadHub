package dev.nahyo0901.uploadhub.network.ftp.server

import java.io.Closeable
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

/**
 * Abstraction over a listening socket so the server core can be tested and
 * platform-specific behavior (Android vs JVM) can be swapped easily.
 */
interface SocketProvider : Closeable {
    suspend fun open()
    suspend fun accept(): Socket?
    fun listenAddress(): String
}

/**
 * Default ServerSocket-based provider.
 *
 * Note: accept() is a blocking call on the underlying ServerSocket. It's
 * invoked from a coroutine with Dispatchers.IO in the Server implementation.
 */
class DefaultSocketProvider(
    private val port: Int = 2121,
    private val backlog: Int = 50
) : SocketProvider {
    @Volatile
    private var serverSocket: ServerSocket? = null

    override suspend fun open() {
        serverSocket = ServerSocket().apply {
            reuseAddress = true
            bind(InetSocketAddress(port), backlog)
        }
    }

    override suspend fun accept(): Socket? {
        val ss = serverSocket ?: return null
        return try {
            ss.accept()
        } catch (t: Throwable) {
            // If the server socket is closed while blocking in accept, an exception is expected.
            null
        }
    }

    override fun listenAddress(): String {
        val ss = serverSocket
        return ss?.localSocketAddress?.toString() ?: "not-open"
    }

    override fun close() {
        try {
            serverSocket?.close()
        } catch (_: Throwable) {
        } finally {
            serverSocket = null
        }
    }
}