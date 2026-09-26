package dev.nahyo0901.uploadhub.network.ftp.server

import kotlinx.coroutines.withTimeout
import java.io.File

object ServerLauncher {

    private var server: Server? = null

    sealed class StartResult {
        data class Success(val port: Int) : StartResult()
        data class Error(val message: String) : StartResult()
    }

    /**
     * Suspends until the server socket is open and accepting, or an error occurs.
     * Called from a coroutine in FtpServerService — suspend is intentional.
     */
    suspend fun startBlocking(port: Int, rootDir: File): StartResult {
        if (server != null) return StartResult.Error("Server already running")

        return try {
            val socketProvider = DefaultSocketProvider(port = port)
            val fsAdapter      = SimpleFileSystemAdapter(rootDir)

            val newServer = Server(
                socketProvider = socketProvider,
                fileSystem     = fsAdapter
            )

            server = newServer
            newServer.start()

            // Wait up to 5 s for the server socket to actually bind
            try {
                withTimeout(5_000) { newServer.readyDeferred.await() }
                StartResult.Success(port)
            } catch (t: Throwable) {
                server = null
                newServer.stop()
                StartResult.Error(t.message ?: "Failed to bind port $port")
            }

        } catch (t: Throwable) {
            server = null
            StartResult.Error(t.message ?: "Unknown error")
        }
    }

    fun stop() {
        try {
            server?.stop()
        } catch (_: Throwable) {
        } finally {
            server = null
        }
    }

    fun isRunning(): Boolean = server != null
}
