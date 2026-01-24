package com.twice.whatislove.uploadhub.network.ftp.server

import kotlinx.coroutines.*
import java.io.Closeable
import java.net.Socket
import kotlinx.coroutines.isActive
import kotlin.coroutines.CoroutineContext

class Server(
    private val socketProvider: SocketProvider,
    private val fileSystem: FileSystemAdapter,
    coroutineContext: CoroutineContext = Dispatchers.IO
) : Closeable {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(coroutineContext + job)

    @Volatile
    private var running = false

    fun start() {
        if (running) return
        running = true

        scope.launch {
            try {
                socketProvider.open()
                acceptLoop()
            } catch (t: Throwable) {
                t.printStackTrace()
            } finally {
                try { socketProvider.close() } catch (_: Throwable) {}
            }
        }

        println("FTP server started on ${socketProvider.listenAddress()}")
    }

    private suspend fun acceptLoop() {
        while (scope.isActive && running) {
            val socket = try {
                withContext(Dispatchers.IO) {
                    socketProvider.accept()
                }
            } catch (t: Throwable) {
                t.printStackTrace()
                null
            }

            if (socket == null) {
                delay(100) // prevent tight loop on failure
                continue
            }

            scope.launch {
                handleClient(socket)
            }
        }
    }

    private suspend fun handleClient(socket: Socket) {
        try {
            ClientHandler(socket, fileSystem).handle()
        } catch (t: Throwable) {
            t.printStackTrace()
        } finally {
            try { socket.close() } catch (_: Throwable) {}
        }
    }

    fun stop() {
        if (!running) return
        running = false

        job.cancel()

        try { socketProvider.close() } catch (_: Throwable) {}
        println("FTP server stopped")
    }

    override fun close() {
        stop()
    }
}