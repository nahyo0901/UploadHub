package dev.nahyo0901.uploadhub.network.ftp.server

import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.system.exitProcess

/**
 * JVM entrypoint for local testing of the FTP server core.
 *
 * Usage:
 *  - Run from the command line or IDE.
 *  - Optional args: [port] [rootDir]
 *
 * Examples:
 *  - no args: uses port 2121 and current working directory as root
 *  - "2221 /tmp/ftproot"
 *
 * Note: This is intended for local development only. On Android, use FtpServerService.
 */
fun main(args: Array<String>) {
    val port    = args.getOrNull(0)?.toIntOrNull() ?: 2121
    val rootDir = args.getOrNull(1)?.let { File(it) } ?: File(".")

    if (!rootDir.exists()) {
        val created = rootDir.mkdirs()
        if (!created) {
            System.err.println("Failed to create root directory: ${rootDir.absolutePath}")
            exitProcess(1)
        }
    }

    println("Starting FTP server for testing")
    println("  port = $port")
    println("  root = ${rootDir.absolutePath}")

    // startBlocking is a suspend function — bridge from the JVM thread with runBlocking
    Thread {
        runBlocking {
            val result = ServerLauncher.startBlocking(port = port, rootDir = rootDir)
            when (result) {
                is ServerLauncher.StartResult.Success ->
                    println("FTP Server started on port ${result.port}")

                is ServerLauncher.StartResult.Error -> {
                    System.err.println("Failed to start FTP server: ${result.message}")
                    exitProcess(1)
                }
            }
        }
    }.start()

    // Shutdown hook to stop server cleanly
    Runtime.getRuntime().addShutdownHook(Thread {
        println("Shutting down FTP server...")
        try { ServerLauncher.stop() } catch (t: Throwable) { t.printStackTrace() }
    })

    // Keep the JVM alive while the server runs
    try {
        Thread.currentThread().join()
    } catch (_: InterruptedException) {
        ServerLauncher.stop()
        Thread.currentThread().interrupt()
    }
}
