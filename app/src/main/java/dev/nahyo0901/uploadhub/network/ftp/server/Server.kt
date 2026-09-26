package dev.nahyo0901.uploadhub.network.ftp.server

import kotlinx.coroutines.*
import java.io.Closeable
import java.net.Socket
import kotlin.coroutines.CoroutineContext

class Server(
    private val socketProvider: SocketProvider,
    private val fileSystem: FileSystemAdapter,
    coroutineContext: CoroutineContext = Dispatchers.IO
) : Closeable {

    private val job   = SupervisorJob()
    private val scope = CoroutineScope(coroutineContext + job)

    @Volatile private var running = false

    // Completed when the server socket is open and accepting connections.
    // CompletableExceptionally when binding fails.
    private val _ready = CompletableDeferred<Boolean>()
    internal val readyDeferred: Deferred<Boolean> get() = _ready

    fun start() {
        if (running) return
        running = true

        scope.launch {
            try {
                socketProvider.open()
                _ready.complete(true)   // socket is bound — signal readiness
                acceptLoop()
            } catch (t: Throwable) {
                _ready.completeExceptionally(t)
                t.printStackTrace()
            } finally {
                try { socketProvider.close() } catch (_: Throwable) {}
            }
        }

        println("FTP server starting on ${socketProvider.listenAddress()}")
    }

    private suspend fun acceptLoop() {
        while (scope.isActive && running) {
            val socket = try {
                withContext(Dispatchers.IO) { socketProvider.accept() }
            } catch (t: Throwable) {
                t.printStackTrace()
                null
            }

            if (socket == null) {
                delay(100)
                continue
            }

            scope.launch { handleClient(socket) }
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

    override fun close() = stop()
}
