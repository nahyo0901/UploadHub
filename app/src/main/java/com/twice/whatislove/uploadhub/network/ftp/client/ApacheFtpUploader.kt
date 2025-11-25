package com.twice.whatislove.uploadhub.network.ftp.client

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPReply
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

/**
 * Simple coroutine-friendly FTP uploader/downloader using Apache Commons Net.
 *
 * Note: add dependency `commons-net:commons-net:3.9.0` (or newer) to your Gradle config.
 */
class ApacheFtpUploader(
    private val connectTimeoutMs: Int = 15_000,
    private val dataTimeoutMs: Int = 30_000,
    private val bufferSize: Int = 8 * 1024
) {

    /**
     * Upload a local file to the remote path.
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
                try { ftp.logout() } catch (_: Throwable) {}
                try { ftp.disconnect() } catch (_: Throwable) {}
                return@withContext false
            }

            ftp.enterLocalPassiveMode()
            ftp.setFileType(FTP.BINARY_FILE_TYPE)

            val file = File(localFile)
            if (!file.exists() || !file.isFile) throw IOException("Local file not found: $localFile")

            FileInputStream(file).use { fis ->
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
                    throw IOException("Failed to complete FTP upload for $remotePath")
                }
                return@withContext true
            }
        } finally {
            try {
                if (ftp.isConnected) {
                    try { ftp.logout() } catch (_: Throwable) {}
                    try { ftp.disconnect() } catch (_: Throwable) {}
                }
            } catch (_: Throwable) {}
        }
    }

    /**
     * Download a remote file to the local path.
     *
     * @return true if download succeeded, false if server returned failure response
     * @throws IOException for network / IO errors
     */
    suspend fun download(
        host: String,
        port: Int = 21,
        user: String,
        pass: String,
        remotePath: String,
        localFile: String,
        progress: ((bytesDownloaded: Long) -> Unit)? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val ftp = FTPClient().apply {
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
                try { ftp.logout() } catch (_: Throwable) {}
                try { ftp.disconnect() } catch (_: Throwable) {}
                return@withContext false
            }

            ftp.enterLocalPassiveMode()
            ftp.setFileType(FTP.BINARY_FILE_TYPE)

            val local = File(localFile)
            local.parentFile?.let { parent ->
                if (!parent.exists()) {
                    if (!parent.mkdirs()) {
                        throw IOException("Failed to create local directory: ${parent.absolutePath}")
                    }
                }
            }

            val input = ftp.retrieveFileStream(remotePath)
                ?: throw IOException("Failed to open remote input stream for $remotePath")

            var bytesCopied = 0L
            val buffer = ByteArray(bufferSize)
            try {
                FileOutputStream(local).use { fos ->
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        fos.write(buffer, 0, read)
                        bytesCopied += read
                        progress?.invoke(bytesCopied)
                    }
                    fos.flush()
                }
            } finally {
                try { input.close() } catch (_: Throwable) {}
            }

            val completed = ftp.completePendingCommand()
            if (!completed) {
                // If completePendingCommand fails, remove partial file and report error
                try { local.delete() } catch (_: Throwable) {}
                throw IOException("Failed to complete FTP download for $remotePath")
            }

            return@withContext true
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