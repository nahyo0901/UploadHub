package com.twice.whatislove.uploadhub.network.ftp.server

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.PrintWriter
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

class ClientHandler(private val socket: Socket, private val fs: FileSystemAdapter) {
    private val running = AtomicBoolean(true)
    private var currentDir: File = File(".")

    suspend fun handle() {
        withContext(Dispatchers.IO) {
            socket.getInputStream().bufferedReader().use { reader ->
                socket.getOutputStream().bufferedWriter().use { writer ->
                    val out = PrintWriter(writer, true)
                    out.println("220 Simple Kotlin FTP Server")
                    var username: String? = null
                    while (running.get()) {
                        val line = reader.readLine() ?: break
                        val parts = line.split(" ", limit = 2)
                        val cmd = parts[0].uppercase()
                        val arg = parts.getOrNull(1) ?: ""
                        when (cmd) {
                            "USER" -> {
                                username = arg
                                out.println("331 Username ok, need password")
                            }
                            "PASS" -> {
                                out.println("230 User logged in")
                            }
                            "PWD" -> {
                                out.println("257 \"${currentDir.path}\" is current directory")
                            }
                            "CWD" -> {
                                val newPath = fs.resolve(currentDir, arg)
                                if (fs.exists(newPath)) {
                                    currentDir = newPath
                                    out.println("250 Directory changed")
                                } else {
                                    out.println("550 Failed to change directory")
                                }
                            }
                            "LIST" -> {
                                out.println("150 Here comes the directory listing")
                                val entries = fs.list(currentDir)
                                entries.forEach { e ->
                                    val lineOut = if (e.isDirectory) {
                                        "drwxr-xr-x 1 owner group ${e.size} ${e.name}"
                                    } else {
                                        "-rw-r--r-- 1 owner group ${e.size} ${e.name}"
                                    }
                                    out.println(lineOut)
                                }
                                out.println("226 Directory send OK")
                            }
                            "RETR" -> {
                                val target = fs.resolve(currentDir, arg)
                                if (!fs.exists(target)) {
                                    out.println("550 File not found")
                                } else {
                                    out.println("150 Opening data connection for RETR")
                                    fs.openRead(target).use { input ->
                                        val outStream = BufferedOutputStream(socket.getOutputStream())
                                        val inStream = BufferedInputStream(input)
                                        inStream.copyTo(outStream)
                                        outStream.flush()
                                    }
                                    out.println("226 Transfer complete")
                                }
                            }
                            "STOR" -> {
                                val target = fs.resolve(currentDir, arg)
                                out.println("150 Ok to send data")
                                fs.openWrite(target).use { outStreamRaw ->
                                    val outStream = BufferedOutputStream(outStreamRaw)
                                    val inStream = BufferedInputStream(socket.getInputStream())
                                    inStream.copyTo(outStream)
                                    outStream.flush()
                                }
                                out.println("226 Transfer complete")
                            }
                            "QUIT" -> {
                                out.println("221 Goodbye")
                                running.set(false)
                            }
                            else -> {
                                out.println("502 Command not implemented")
                            }
                        }
                    }
                }
            }
        }
    }
}