package com.twice.whatislove.uploadhub.network.ftp.client.engine

import java.io.*
import java.net.Socket

class FtpClient {

    private lateinit var controlSocket: Socket
    private lateinit var reader: BufferedReader
    private lateinit var writer: BufferedWriter

    // ---------------- CONNECT ----------------

    fun connect(host: String, port: Int = 21) {
        controlSocket = Socket(host, port)
        reader = BufferedReader(InputStreamReader(controlSocket.getInputStream()))
        writer = BufferedWriter(OutputStreamWriter(controlSocket.getOutputStream()))

        readResponse() // welcome message
    }

    fun login(user: String, pass: String) {
        sendCommand("USER $user")
        sendCommand("PASS $pass")
        sendCommand("TYPE I") // binary mode
    }

    fun disconnect() {
        sendCommand("QUIT")
        controlSocket.close()
    }

    // ---------------- CORE COMMANDS ----------------

    private fun sendCommand(cmd: String): FtpResponse {
        writer.write("$cmd\r\n")
        writer.flush()
        return readResponse()
    }

    private fun readResponse(): FtpResponse {
        val line = reader.readLine() ?: throw FtpException("Server closed connection")
        val code = line.substring(0, 3).toInt()
        return FtpResponse(code, line)
    }

    private fun enterPassiveMode(): Socket {
        val resp = sendCommand("PASV")
        if (!resp.isPositive()) throw FtpException("PASV failed")
        return DataConnection.openPassiveSocket(resp.message)
    }

    // ---------------- UPLOAD ----------------

    fun uploadFile(
        file: File,
        remotePath: String,
        progress: (sent: Long, total: Long) -> Unit
    ) {
        val dataSocket = enterPassiveMode()
        sendCommand("STOR $remotePath")

        val output = BufferedOutputStream(dataSocket.getOutputStream())
        val input = FileInputStream(file)

        val buffer = ByteArray(8192)
        var sent = 0L
        val total = file.length()

        while (true) {
            val read = input.read(buffer)
            if (read == -1) break
            output.write(buffer, 0, read)
            sent += read
            progress(sent, total)
        }

        input.close()
        output.close()
        dataSocket.close()

        readResponse()
    }

    // ---------------- DOWNLOAD ----------------

    fun downloadFile(
        remotePath: String,
        localFile: File,
        progress: (recv: Long, total: Long) -> Unit
    ) {
        val sizeResp = sendCommand("SIZE $remotePath")
        val total = sizeResp.message.substringAfter(" ").toLongOrNull() ?: -1L

        val dataSocket = enterPassiveMode()
        sendCommand("RETR $remotePath")

        val input = BufferedInputStream(dataSocket.getInputStream())
        val output = FileOutputStream(localFile)

        val buffer = ByteArray(8192)
        var received = 0L

        while (true) {
            val read = input.read(buffer)
            if (read == -1) break
            output.write(buffer, 0, read)
            received += read
            if (total > 0) progress(received, total)
        }

        output.close()
        input.close()
        dataSocket.close()

        readResponse()
    }
}