package dev.nahyo0901.uploadhub.network.ftp.server

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

class ClientHandler(private val socket: Socket, private val fs: FileSystemAdapter) {

    private val running    = AtomicBoolean(true)
    private var currentDir = fs.root()   // rooted to FS adapter, not File(".")

    @Volatile
    private var pasvServer: ServerSocket? = null

    suspend fun handle() = withContext(Dispatchers.IO) {
        val reader = socket.getInputStream().bufferedReader()
        val writer = PrintWriter(OutputStreamWriter(socket.getOutputStream()), true)

        writer.println("220 UploadHub FTP Server ready")

        while (running.get()) {
            val line  = reader.readLine() ?: break
            val parts = line.trim().split(" ", limit = 2)
            val cmd   = parts[0].uppercase()
            val arg   = parts.getOrNull(1)?.trim() ?: ""

            when (cmd) {

                "USER" -> writer.println("331 Username ok, need password")
                "PASS" -> writer.println("230 User logged in")
                "SYST" -> writer.println("215 UNIX Type: L8")
                "NOOP" -> writer.println("200 OK")
                "TYPE" -> writer.println("200 Type set")

                "FEAT" -> {
                    writer.println("211-Features:")
                    writer.println(" PASV")
                    writer.println(" SIZE")
                    writer.println(" TYPE")
                    writer.println("211 End")
                }

                "PWD" -> {
                    val rel = try {
                        fs.root().toURI().relativize(currentDir.toURI()).path.trimEnd('/')
                    } catch (_: Exception) { "" }
                    val display = if (rel.isEmpty()) "/" else "/$rel"
                    writer.println("257 \"$display\" is current directory")
                }

                "CWD" -> {
                    val target = when {
                        arg == "/"  -> fs.root()
                        arg == ".." -> {
                            val parent = currentDir.parentFile
                            if (parent != null &&
                                parent.canonicalPath.startsWith(fs.root().canonicalPath)) parent
                            else currentDir
                        }
                        arg.startsWith("/") -> fs.resolve(fs.root(), arg.trimStart('/'))
                        else                -> fs.resolve(currentDir, arg)
                    }
                    if (fs.exists(target) && target.isDirectory) {
                        currentDir = target
                        writer.println("250 Directory changed")
                    } else {
                        writer.println("550 No such file or directory")
                    }
                }

                "CDUP" -> {
                    val parent = currentDir.parentFile
                    if (parent != null &&
                        parent.canonicalPath.startsWith(fs.root().canonicalPath)) {
                        currentDir = parent
                        writer.println("200 CDUP command successful")
                    } else {
                        writer.println("550 Already at root")
                    }
                }

                "PASV" -> {
                    try {
                        pasvServer?.close()
                        val localAddr = socket.localAddress
                        val srv       = ServerSocket(0, 1, localAddr)
                        pasvServer    = srv
                        val port      = srv.localPort
                        val addr      = localAddr.hostAddress?.replace('.', ',') ?: "127,0,0,1"
                        val p1        = port ushr 8
                        val p2        = port and 0xFF
                        writer.println("227 Entering Passive Mode ($addr,$p1,$p2)")
                    } catch (e: Exception) {
                        writer.println("425 Can't open data connection: ${e.message}")
                    }
                }

                "LIST" -> {
                    val data = openDataConnection(writer) ?: continue
                    try {
                        writer.println("150 Here comes the directory listing")
                        val out     = PrintWriter(data.getOutputStream(), true)
                        val dateFmt = SimpleDateFormat("MMM dd HH:mm", Locale.ENGLISH)
                        val now     = dateFmt.format(Date())
                        fs.list(currentDir).forEach { e ->
                            val perms = if (e.isDirectory) "drwxr-xr-x" else "-rw-r--r--"
                            out.println("$perms 1 owner group ${e.size} $now ${e.name}")
                        }
                        out.flush()
                        data.close()
                        writer.println("226 Directory send OK")
                    } catch (e: Exception) {
                        runCatching { data.close() }
                        writer.println("426 Connection closed; transfer aborted")
                    }
                }

                "RETR" -> {
                    val target = fs.resolve(currentDir, arg)
                    if (!fs.exists(target) || target.isDirectory) {
                        writer.println("550 File not found: $arg")
                        continue
                    }
                    val data = openDataConnection(writer) ?: continue
                    try {
                        writer.println("150 Opening data connection for RETR")
                        fs.openRead(target).use { input ->
                            BufferedOutputStream(data.getOutputStream()).use { out ->
                                input.copyTo(out)
                            }
                        }
                        data.close()
                        writer.println("226 Transfer complete")
                    } catch (e: Exception) {
                        runCatching { data.close() }
                        writer.println("426 Connection closed; transfer aborted")
                    }
                }

                "STOR" -> {
                    val target = fs.resolve(currentDir, arg)
                    val data   = openDataConnection(writer) ?: continue
                    try {
                        writer.println("150 Ok to send data")
                        fs.openWrite(target).use { outFile ->
                            BufferedOutputStream(outFile).use { out ->
                                data.getInputStream().copyTo(out)
                            }
                        }
                        data.close()
                        writer.println("226 Transfer complete")
                    } catch (e: Exception) {
                        runCatching { data.close() }
                        writer.println("426 Connection closed; transfer aborted")
                    }
                }

                "SIZE" -> {
                    val target = fs.resolve(currentDir, arg)
                    if (fs.exists(target) && !target.isDirectory) {
                        writer.println("213 ${target.length()}")
                    } else {
                        writer.println("550 File not found")
                    }
                }

                "MKD" -> {
                    val newDir = fs.resolve(currentDir, arg)
                    if (newDir.mkdirs()) {
                        writer.println("257 \"$arg\" directory created")
                    } else {
                        writer.println("550 Failed to create directory")
                    }
                }

                "RMD" -> {
                    val dir = fs.resolve(currentDir, arg)
                    if (dir.exists() && dir.isDirectory && dir.deleteRecursively()) {
                        writer.println("250 Directory removed")
                    } else {
                        writer.println("550 Failed to remove directory")
                    }
                }

                "DELE" -> {
                    val file = fs.resolve(currentDir, arg)
                    if (file.exists() && file.isFile && file.delete()) {
                        writer.println("250 File deleted")
                    } else {
                        writer.println("550 Failed to delete file")
                    }
                }

                "QUIT" -> {
                    writer.println("221 Goodbye")
                    running.set(false)
                }

                else -> writer.println("502 Command not implemented: $cmd")
            }
        }
    }

    /** Accepts the PASV data connection, cleans up the listening socket. */
    private fun openDataConnection(writer: PrintWriter): Socket? {
        val srv = pasvServer ?: run {
            writer.println("425 Use PASV first")
            return null
        }
        pasvServer = null
        return try {
            srv.soTimeout = 10_000
            srv.accept()
        } catch (e: Exception) {
            writer.println("425 Can't open data connection: ${e.message}")
            null
        } finally {
            runCatching { srv.close() }
        }
    }
}
