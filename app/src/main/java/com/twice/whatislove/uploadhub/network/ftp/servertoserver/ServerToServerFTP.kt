package com.twice.whatislove.uploadhub.network.ftp.servertoserver

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPReply
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress

class ServerToServerFTP {

    companion object {
        private const val TAG = "ServerToServerFTP"
    }

    /**
     * Directly transfers a file from server1 → server2 without saving locally.
     *
     * @param server1 FTP server1 address (host[:port])
     * @param user1 FTP server1 username
     * @param pass1 FTP server1 password
     * @param remoteFile1 Path of the file on server1
     * @param server2 FTP server2 address (host[:port])
     * @param user2 FTP server2 username
     * @param pass2 FTP server2 password
     * @param remoteFile2 Target file path on server2
     */
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
            // --- Connect to server1 ---
            val (host1, port1) = parseHostPort(server1)
            ftp1.connect(host1, port1)
            if (!FTPReply.isPositiveCompletion(ftp1.replyCode)) throw IOException("Server1 refused connection")
            ftp1.login(user1, pass1)
            Log.d(TAG, "Connected and logged in to server1: $host1")

            // --- Connect to server2 ---
            val (host2, port2) = parseHostPort(server2)
            ftp2.connect(host2, port2)
            if (!FTPReply.isPositiveCompletion(ftp2.replyCode)) throw IOException("Server2 refused connection")
            ftp2.login(user2, pass2)
            Log.d(TAG, "Connected and logged in to server2: $host2")

            // --- Setup passive/active modes for server-to-server transfer ---
            ftp2.enterRemotePassiveMode()
            ftp1.enterRemoteActiveMode(InetAddress.getByName(ftp2.passiveHost), ftp2.passivePort)

            // --- Open streams ---
            inputStream = ftp1.retrieveFileStream(remoteFile1)
                ?: throw IOException("Cannot open input stream from server1 file: $remoteFile1")
            outputStream = ftp2.storeFileStream(remoteFile2)
                ?: throw IOException("Cannot open output stream to server2 file: $remoteFile2")

            // --- Pipe data directly ---
            val buffer = ByteArray(4096)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }
            outputStream.flush()
            Log.d(TAG, "File transferred from server1 → server2: $remoteFile1 → $remoteFile2")

            // --- Complete pending commands ---
            ftp1.completePendingCommand()
            ftp2.completePendingCommand()

        } catch (e: Exception) {
            Log.e(TAG, "Server-to-server FTP transfer failed", e)
            throw e
        } finally {
            inputStream?.close()
            outputStream?.close()
            if (ftp1.isConnected) { ftp1.logout(); ftp1.disconnect() }
            if (ftp2.isConnected) { ftp2.logout(); ftp2.disconnect() }
            Log.d(TAG, "Disconnected from both FTP servers")
        }
    }

    private fun parseHostPort(server: String): Pair<String, Int> {
        val parts = server.split(":")
        val host = parts[0]
        val port = if (parts.size == 2) parts[1].toIntOrNull() ?: 21 else 21
        return host to port
    }
}
