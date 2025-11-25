package com.twice.whatislove.uploadhub

import kotlinx.coroutines.*
import java.io.Closeable
import java.net.Socket
import kotlin.coroutines.CoroutineContext

class Server(
    private val socketProvider: SocketProvider,
    private val fileSystem: FileSystemAdapter,
    private val coroutineContext: CoroutineContext = Dispatchers.IO
) : Closeable {
    private val scope = CoroutineScope(coroutineContext + SupervisorJob())
    @Volatile
    private var running = false

    fun start() {
        if (running) return
        running = true
        scope.launch {
            socketProvider.open()
            try {
                acceptLoop()
            } finally {
                socketProvider.close()
            }
        }
        println("FTP server started on ${socketProvider.listenAddress()}")
    }

    private suspend fun acceptLoop() {
        while (isActive && running) {
            val socket = withContext(Dispatchers.IO) {
                socketProvider.accept()
            } ?: break
            scope.launch {
                try {
                    ClientHandler(socket, fileSystem).handle()
                } catch (t: Throwable) {
                    t.printStackTrace()
                } finally {
                    try { socket.close() } catch (_: Throwable) {}
                }
            }
        }
    }

    fun stop() {
        running = false
        scope.cancel()
        socketProvider.close()
        println("FTP server stopped")
    }

    override fun close() {
        stop()
    }
}