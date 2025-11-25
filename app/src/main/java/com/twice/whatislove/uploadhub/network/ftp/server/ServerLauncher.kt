package com.twice.whatislove.uploadhub.network.ftp.server

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

object ServerLauncher {
    private var server: Server? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Start the FTP server.
     *
     * @param port port to bind (default 2121)
     * @param rootDir directory used as FTP root (app filesDir or external dir)
     */
    fun start(port: Int = 2121, rootDir: File) {
        if (server != null) return
        val socketProvider = DefaultSocketProvider(port = port)
        val fsAdapter = SimpleFileSystemAdapter(rootDir)
        server = Server(socketProvider = socketProvider, fileSystem = fsAdapter)
        scope.launch {
            try {
                server?.start()
            } catch (t: Throwable) {
                t.printStackTrace()
                stop()
            }
        }
    }

    /** Stop the running server if any. Safe to call multiple times. */
    fun stop() {
        try {
            server?.stop()
        } catch (t: Throwable) {
            t.printStackTrace()
        } finally {
            server = null
        }
    }

    /** Return whether the server appears to be running. */
    fun isRunning(): Boolean = server != null
}