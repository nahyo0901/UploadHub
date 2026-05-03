package com.twice.whatislove.uploadhub.network.ftp.server

import java.io.File

object ServerLauncher {

    private var server: Server? = null

    sealed class StartResult {
        data class Success(val port: Int) : StartResult()
        data class Error(val message: String) : StartResult()
    }

    /**
     * Blocking start.
     * This call will BLOCK until the server stops or throws.
     */
    fun startBlocking(port: Int, rootDir: File): StartResult {
        if (server != null) {
            return StartResult.Error("Server already running")
        }

        return try {
            val socketProvider = DefaultSocketProvider(port = port)
            val fsAdapter = SimpleFileSystemAdapter(rootDir)

            val newServer = Server(
                socketProvider = socketProvider,
                fileSystem = fsAdapter
            )

            server = newServer

            // IMPORTANT: this blocks
            newServer.start()

            // If start() returns normally it means it stopped
            server = null
            StartResult.Error("Server stopped unexpectedly")

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