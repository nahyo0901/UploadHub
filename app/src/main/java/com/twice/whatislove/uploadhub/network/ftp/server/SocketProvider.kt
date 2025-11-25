package com.twice.whatislove.uploadhub

import java.io.Closeable
import java.net.ServerSocket
import java.net.Socket
import java.net.InetSocketAddress

interface SocketProvider : Closeable {
    suspend fun open()
    suspend fun accept(): Socket?
    fun listenAddress(): String
}

class DefaultSocketProvider(private val port: Int = 2121, private val backlog: Int = 50) : SocketProvider {
    private var serverSocket: ServerSocket? = null

    override suspend fun open() {
        serverSocket = ServerSocket()
        serverSocket!!.reuseAddress = true
        serverSocket!!.bind(InetSocketAddress(port), backlog)
    }

    override suspend fun accept(): Socket? {
        val ss = serverSocket ?: return null
        return try {
            ss.accept()
        } catch (e: Exception) {
            null
        }
    }

    override fun listenAddress(): String {
        val ss = serverSocket
        return ss?.localSocketAddress?.toString() ?: "not-open"
    }

    override fun close() {
        try { serverSocket?.close() } catch (_: Throwable) {}
    }
}