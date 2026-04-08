package com.twice.whatislove.uploadhub.network.ftp.servertoserver

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPReply
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class ServerToServerFTP {

    companion object {
        private const val TAG = "ServerToServerFTP"
        private const val BUFFER_SIZE = 1024 * 8
    }

    suspend fun transferServerToServer(
        server1: String,
        user1: String,
        pass1: String,
        remoteFile1: String,
        server2: String,
        user2: String,
        pass2: String,
        remoteFile2: String
    ) = withContext(Dispatchers.IO) {

        val ftp1 = FTPClient()
        val ftp2 = FTPClient()
        var inputStream: InputStream? = null
        var outputStream: OutputStream? = null

        try {
            // CONNECT SERVER 1
            val (host1, port1) = parseHostPort(server1)
            ftp1.connect(host1, port1)
            if (!FTPReply.isPositiveCompletion(ftp1.replyCode))
                throw IOException("Server1 refused connection")
            ftp1.login(user1, pass1)
            ftp1.enterLocalPassiveMode()
            ftp1.setFileType(FTPClient.BINARY_FILE_TYPE)
            Log.d(TAG, "Connected to server1")

            // CONNECT SERVER 2
            val (host2, port2) = parseHostPort(server2)
            ftp2.connect(host2, port2)
            if (!FTPReply.isPositiveCompletion(ftp2.replyCode))
                throw IOException("Server2 refused connection")
            ftp2.login(user2, pass2)
            ftp2.enterLocalPassiveMode()
            ftp2.setFileType(FTPClient.BINARY_FILE_TYPE)
            Log.d(TAG, "Connected to server2")

            // OPEN STREAMS (APP RELAY MODE)
            inputStream = ftp1.retrieveFileStream(remoteFile1)
                ?: throw IOException("Cannot open input stream: $remoteFile1")

            outputStream = ftp2.storeFileStream(remoteFile2)
                ?: throw IOException("Cannot open output stream: $remoteFile2")

            // PIPE DATA (Server1 → Phone RAM → Server2)
            val buffer = ByteArray(BUFFER_SIZE)
            var bytesRead: Int
            var totalBytes = 0L

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytes += bytesRead
            }

            outputStream.flush()

            ftp1.completePendingCommand()
            ftp2.completePendingCommand()

            Log.d(TAG, "Transfer finished. Bytes: $totalBytes")

        } catch (e: Exception) {
            Log.e(TAG, "Transfer failed", e)
            throw e
        } finally {
            inputStream?.close()
            outputStream?.close()
            if (ftp1.isConnected) { ftp1.logout(); ftp1.disconnect() }
            if (ftp2.isConnected) { ftp2.logout(); ftp2.disconnect() }
        }
    }

    private fun parseHostPort(server: String): Pair<String, Int> {
        val parts = server.split(":")
        val host = parts[0]
        val port = if (parts.size == 2) parts[1].toIntOrNull() ?: 21 else 21
        return host to port
    }
}