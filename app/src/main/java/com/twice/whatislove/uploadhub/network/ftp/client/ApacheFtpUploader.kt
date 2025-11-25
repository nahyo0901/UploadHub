package com.twice.whatislove.uploadhub.network.ftp.client

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream

/**
 * Simple coroutine-friendly FTP uploader using Apache Commons Net.
 *
 * Note: add dependency `org.apache.commons:commons-net:3.9.0` (or newer) to your Gradle config.
 */
class ApacheFtpUploader(
    private val connectTimeoutMs: Int = 15_000,
    private val dataTimeoutMs: Int = 30_000,
    private val bufferSize: Int = 8 * 1024
) {

    /**
     * Upload a local file to the remote path.
     *
     * @param host FTP host
     * @param port FTP port (default 21)
     * @param user username
     * @param pass password
     * @param remotePath remote target path (including filename)
     * @param localFile local file path
     * @param progress optional callback invoked with bytes uploaded (on calling thread)
     *
     * @return true if upload succeeded, false if server returned failure response
     * @throws IOException for network / IO errors
     */
    suspend fun upload(
        host: String,
        port: Int = 21,
        user: String,
        pass: String,
        remotePath: String,
        localFile: String,
        progress: ((bytesUploaded: Long) -> Unit)? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val ftp = FTPClient().apply {
            // timeouts
            defaultTimeout = connectTimeoutMs
            connectTimeout = connectTimeoutMs
            soTimeout = dataTimeoutMs
            bufferSize = this@ApacheFtpUploader.bufferSize
        }

        try {
            ftp.connect(host, port)
            val reply = ftp.replyCode
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftp.disconnect()
                throw IOException("FTP server refused connection, replyCode=$reply")
            }

            val loggedIn = ftp.login(user, pass)
            if (!loggedIn) {
                ftp.logout()
                ftp.disconnect()
                return@withContext false
            }

            ftp.enterLocalPassiveMode()
            ftp.setFileType(FTP.BINARY_FILE_TYPE)

            val file = File(localFile)
            if (!file.exists() || !file.isFile) throw IOException("Local file not found: $localFile")

            FileInputStream(file).use { fis ->
                // Option A: use storeFile (simpler)
                // return@withContext ftp.storeFile(remotePath, fis)

                // Option B: stream with storeFileStream + completePendingCommand for large files
                val out = ftp.storeFileStream(remotePath)
                    ?: throw IOException("Failed to open remote output stream for $remotePath")

                var bytesCopied = 0L
                val buffer = ByteArray(bufferSize)
                var read: Int
                try {
                    while (true) {
                        read = fis.read(buffer)
                        if (read <= 0) break
                        out.write(buffer, 0, read)
                        bytesCopied += read
                        progress?.invoke(bytesCopied)
                    }
                    out.flush()
                } finally {
                    try { out.close() } catch (_: Throwable) {}
                }

                val completed = ftp.completePendingCommand()
                if (!completed) {
                    throw IOException("Failed to complete FTP transfer for $remotePath")
                }
                return@withContext true
            }
        } catch (e: IOException) {
            throw e
        } finally {
            try {
                if (ftp.isConnected) {
                    try { ftp.logout() } catch (_: Throwable) {}
                    try { ftp.disconnect() } catch (_: Throwable) {}
                }
            } catch (_: Throwable) {}
        }
    }
}