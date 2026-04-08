package com.twice.whatislove.uploadhub.network.ftp.servertoserver

import android.content.Context
import android.util.Log
import com.twice.whatislove.uploadhub.data.TransferOffsetDataStore
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
     * Transfers a file from server1 → server2 with:
     * - Real-time progress
     * - Resume support
     * - Cancel support
     * - Logs
     *
     * @param context Android context for potential offset persistence
     * @param server1 FTP server1 host:port
     * @param user1 FTP server1 username
     * @param pass1 FTP server1 password
     * @param remoteFile1 Path of the file on server1
     * @param server2 FTP server2 host:port
     * @param user2 FTP server2 username
     * @param pass2 FTP server2 password
     * @param remoteFile2 Path of target file on server2
     * @param onProgress Lambda(totalTransferred, totalSize)
     * @param onLog Lambda(logMessage)
     * @param isCancelled Lambda(): Boolean returns true if transfer should cancel
     * @param transferId Unique id for saving offset (resume)
     * @param offsetDataStore DataStore helper for persistent offset
     */
    suspend fun transferServerToServerWithProgress(
        context: Context,
        server1: String,
        user1: String,
        pass1: String,
        remoteFile1: String,
        server2: String,
        user2: String,
        pass2: String,
        remoteFile2: String,
        onProgress: (transferred: Long, totalSize: Long) -> Unit,
        onLog: (String) -> Unit,
        isCancelled: () -> Boolean,
        transferId: String,
        offsetDataStore: TransferOffsetDataStore
    ) = withContext(Dispatchers.IO) {
        val ftp1 = FTPClient()
        val ftp2 = FTPClient()
        var inputStream: InputStream? = null
        var outputStream: OutputStream? = null
        var totalTransferred: Long = 0

        try {
            // --- Connect to server1 ---
            val (host1, port1) = parseHostPort(server1)
            ftp1.connect(host1, port1)
            if (!FTPReply.isPositiveCompletion(ftp1.replyCode)) throw IOException("Server1 refused connection")
            ftp1.login(user1, pass1)
            onLog("Connected to server1: $host1")

            // --- Connect to server2 ---
            val (host2, port2) = parseHostPort(server2)
            ftp2.connect(host2, port2)
            if (!FTPReply.isPositiveCompletion(ftp2.replyCode)) throw IOException("Server2 refused connection")
            ftp2.login(user2, pass2)
            onLog("Connected to server2: $host2")

            // --- Setup passive/active mode ---
            ftp2.enterRemotePassiveMode()
            ftp1.enterRemoteActiveMode(InetAddress.getByName(ftp2.passiveHost), ftp2.passivePort)

            // --- Get file size ---
            val size1 = ftp1.mlistFile(remoteFile1)?.size ?: throw IOException("Cannot get remote file size")
            val savedOffset = offsetDataStore.getOffsetOnce(transferId)
            totalTransferred = savedOffset
            onProgress(totalTransferred, size1)
            onLog("Starting transfer from byte $totalTransferred / $size1")

            // --- Open streams ---
            inputStream = ftp1.retrieveFileStream(remoteFile1) ?: throw IOException("Cannot open input stream from server1")
            outputStream = ftp2.storeFileStream(remoteFile2) ?: throw IOException("Cannot open output stream to server2")

            // Skip already transferred bytes for resume
            if (totalTransferred > 0) {
                inputStream.skip(totalTransferred)
                onLog("Resuming transfer at offset $totalTransferred")
            }

            // --- Transfer loop ---
            val buffer = ByteArray(4096)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                if (isCancelled()) {
                    offsetDataStore.saveOffset(transferId, totalTransferred)
                    onLog("Transfer cancelled at $totalTransferred bytes")
                    break
                }
                outputStream.write(buffer, 0, bytesRead)
                totalTransferred += bytesRead
                onProgress(totalTransferred, size1)

                // Persist offset every 1MB
                if (totalTransferred % (1024 * 1024) < bytesRead) {
                    offsetDataStore.saveOffset(transferId, totalTransferred)
                }
            }

            outputStream.flush()
            ftp1.completePendingCommand()
            ftp2.completePendingCommand()
            onLog("Transfer finished: $totalTransferred / $size1 bytes")
            // Clear saved offset on success
            offsetDataStore.clearOffset(transferId)

        } catch (e: Exception) {
            onLog("Error: ${e.message}")
            throw e
        } finally {
            inputStream?.close()
            outputStream?.close()
            if (ftp1.isConnected) { ftp1.logout(); ftp1.disconnect() }
            if (ftp2.isConnected) { ftp2.logout(); ftp2.disconnect() }
            onLog("Disconnected from both FTP servers")
        }
    }

    private fun parseHostPort(server: String): Pair<String, Int> {
        val parts = server.split(":")
        val host = parts[0]
        val port = if (parts.size == 2) parts[1].toIntOrNull() ?: 21 else 21
        return host to port
    }
}