package dev.nahyo0901.uploadhub.network.ftp.client

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.net.InetAddress
import java.net.Socket
import java.util.Locale
import kotlin.text.Charsets.UTF_8

/**
 * Minimal coroutine-friendly FTP client using PASV data connections.
 *
 * Not a full-featured replacement for Apache Commons Net; intended for
 * simple uploads/downloads and directory listings on trusted networks.
 *
 * Usage:
 *   val client = FtpClient()
 *   client.connect(host, port)
 *   client.login(user, pass)
 *   client.list() // returns lines
 *   client.download(remotePath, localFile)
 *   client.upload(localFile, remotePath)
 *   client.quit()
 */
class FtpClient(
    private val connectTimeoutMs: Int = 15_000,
    private val readTimeoutMs: Int = 30_000
) : Closeable {
    private var controlSocket: Socket? = null
    private var reader: BufferedReader? = null
    private var writer: BufferedWriter? = null
    private val dataConnection = DataConnection()

    private suspend fun readResponseLine(): String = withContext(Dispatchers.IO) {
        val line = reader?.readLine() ?: throw FtpException("Control connection closed")
        line
    }

    private suspend fun readResponse(): FtpResponse = withContext(Dispatchers.IO) {
        val first = readResponseLine()
        // FTP multiline responses start with "123-" and end with "123 message"
        val code = first.take(3).toIntOrNull() ?: throw FtpException("Invalid response: $first")
        if (first.length > 3 && first[3] == '-') {
            val sb = StringBuilder()
            sb.append(first.substring(4))
            while (true) {
                val line = readResponseLine()
                if (line.length >= 4 && line.substring(0, 3).toIntOrNull() == code && line[3] == ' ') {
                    sb.append("\n").append(line.substring(4))
                    break
                } else {
                    sb.append("\n").append(line)
                }
            }
            FtpResponse(code, sb.toString())
        } else {
            FtpResponse(code, if (first.length > 4) first.substring(4) else "")
        }
    }

    private suspend fun sendCommand(cmd: String): FtpResponse = withContext(Dispatchers.IO) {
        writer ?: throw FtpException("Not connected")
        writer!!.write(cmd + "\r\n")
        writer!!.flush()
        readResponse()
    }

    /**
     * Connect to FTP server control socket.
     */
    suspend fun connect(host: String, port: Int = 21) = withContext(Dispatchers.IO) {
        disconnectQuietly()
        controlSocket = Socket()
        controlSocket!!.soTimeout = readTimeoutMs
        controlSocket!!.connect(java.net.InetSocketAddress(host, port), connectTimeoutMs)
        reader = BufferedReader(InputStreamReader(controlSocket!!.getInputStream(), UTF_8))
        writer = BufferedWriter(OutputStreamWriter(controlSocket!!.getOutputStream(), UTF_8))
        val resp = readResponse()
        if (resp.code !in 200..399) throw FtpException("Failed to connect: ${resp.message}", resp)
    }

    /**
     * Login with username and password.
     */
    suspend fun login(username: String, password: String): FtpResponse {
        val r1 = sendCommand("USER $username")
        if (r1.code == 331) {
            val r2 = sendCommand("PASS $password")
            if (r2.code !in 200..299) throw FtpException("Login failed: ${r2.message}", r2)
            return r2
        } else if (r1.code in 200..299) {
            return r1
        } else {
            throw FtpException("Login failed: ${r1.message}", r1)
        }
    }

    /**
     * Change working directory.
     */
    suspend fun cwd(path: String): FtpResponse {
        val r = sendCommand("CWD $path")
        if (r.code !in 200..299) throw FtpException("CWD failed: ${r.message}", r)
        return r
    }

    /**
     * Print working directory.
     */
    suspend fun pwd(): String {
        val r = sendCommand("PWD")
        if (r.code !in 200..299) throw FtpException("PWD failed: ${r.message}", r)
        // message often contains quoted path
        return r.message.trim().trim('"')
    }

    /**
     * List directory contents (PASV). Returns raw lines from server listing.
     */
    suspend fun list(path: String? = null): List<String> = withContext(Dispatchers.IO) {
        val pasv = enterPassiveMode()
        try {
            val cmd = if (path.isNullOrBlank()) "LIST" else "LIST $path"
            val pre = sendCommand(cmd)
            if (pre.code !in 150..199) throw FtpException("LIST failed to start: ${pre.message}", pre)
            val input = dataConnection.inputStream().bufferedReader(Charsets.UTF_8)
            val lines = mutableListOf<String>()
            input.use { br ->
                var line: String? = br.readLine()
                while (line != null) {
                    lines.add(line)
                    line = br.readLine()
                }
            }
            val post = readResponse()
            if (post.code !in 200..299) throw FtpException("LIST failed: ${post.message}", post)
            lines
        } finally {
            dataConnection.closeQuietly()
        }
    }

    /**
     * Download remote file to local file (RETR via PASV).
     */
    suspend fun download(remotePath: String, localFile: File) = withContext(Dispatchers.IO) {
        val pasv = enterPassiveMode()
        try {
            val pre = sendCommand("RETR $remotePath")
            if (pre.code !in 150..199) throw FtpException("RETR failed to start: ${pre.message}", pre)
            localFile.parentFile?.let { if (!it.exists()) it.mkdirs() }
            dataConnection.inputStream().use { input ->
                FileOutputStream(localFile).use { out ->
                    input.copyTo(out)
                }
            }
            val post = readResponse()
            if (post.code !in 200..299) throw FtpException("RETR failed: ${post.message}", post)
        } finally {
            dataConnection.closeQuietly()
        }
    }

    /**
     * Upload local file to remote path (STOR via PASV).
     */
    suspend fun upload(localFile: File, remotePath: String) = withContext(Dispatchers.IO) {
        if (!localFile.exists() || !localFile.isFile) throw FtpException("Local file not found: ${localFile.path}")
        val pasv = enterPassiveMode()
        try {
            val pre = sendCommand("STOR $remotePath")
            if (pre.code !in 150..199) throw FtpException("STOR failed to start: ${pre.message}", pre)
            dataConnection.outputStream().use { out ->
                FileInputStream(localFile).use { input ->
                    input.copyTo(out)
                    out.flush()
                }
            }
            val post = readResponse()
            if (post.code !in 200..299) throw FtpException("STOR failed: ${post.message}", post)
        } finally {
            dataConnection.closeQuietly()
        }
    }

    /**
     * Quit and close control connection.
     */
    suspend fun quit() {
        try {
            sendCommand("QUIT")
        } catch (_: Throwable) {
        } finally {
            disconnectQuietly()
        }
    }

    /**
     * Enter passive mode and open data connection.
     * Returns Pair(host, port) for debugging if needed.
     */
    private suspend fun enterPassiveMode(): Pair<String, Int> = withContext(Dispatchers.IO) {
        val resp = sendCommand("PASV")
        if (resp.code !in 200..299) throw FtpException("PASV failed: ${resp.message}", resp)
        // parse "(h1,h2,h3,h4,p1,p2)"
        val start = resp.message.indexOf('(')
        val end = resp.message.indexOf(')')
        if (start < 0 || end < 0 || end <= start) throw FtpException("Invalid PASV response: ${resp.message}", resp)
        val parts = resp.message.substring(start + 1, end).split(',').map { it.trim() }
        if (parts.size < 6) throw FtpException("Invalid PASV response: ${resp.message}", resp)
        val host = parts.subList(0, 4).joinToString(".")
        val p1 = parts[4].toIntOrNull() ?: throw FtpException("Invalid PASV port", resp)
        val p2 = parts[5].toIntOrNull() ?: throw FtpException("Invalid PASV port", resp)
        val port = p1 * 256 + p2

        // If host is 0.0.0.0 or local, prefer control socket remote address
        val resolvedHost = if (host == "0.0.0.0" || host.startsWith("127.") || host == "0.0.0.0") {
            controlSocket?.inetAddress?.hostAddress ?: host
        } else host

        dataConnection.openPassive(resolvedHost, port, readTimeoutMs)
        Pair(resolvedHost, port)
    }

    private fun disconnectQuietly() {
        try {
            dataConnection.closeQuietly()
        } catch (_: Throwable) {
        }
        try {
            reader?.close()
        } catch (_: Throwable) {
        }
        try {
            writer?.close()
        } catch (_: Throwable) {
        }
        try {
            controlSocket?.close()
        } catch (_: Throwable) {
        } finally {
            reader = null
            writer = null
            controlSocket = null
        }
    }

    override fun close() {
        disconnectQuietly()
    }
}