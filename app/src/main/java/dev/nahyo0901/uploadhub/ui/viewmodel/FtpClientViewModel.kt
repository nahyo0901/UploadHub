package dev.nahyo0901.uploadhub.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPReply
import java.io.File

class FtpClientViewModel : ViewModel() {

    // Single FTPClient instance that survives recomposition and config changes
    private val ftp = FTPClient()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs

    private fun log(msg: String) = _logs.update { it + msg }

    fun connect(host: String, port: Int, user: String, pass: String) {
        viewModelScope.launch {
            _busy.value = true
            log("Connecting...")
            val ok = withContext(Dispatchers.IO) {
                try {
                    ftp.connect(host, port)
                    if (!FTPReply.isPositiveCompletion(ftp.replyCode)) return@withContext false
                    if (!ftp.login(user, pass)) return@withContext false
                    ftp.enterLocalPassiveMode()
                    ftp.setFileType(FTP.BINARY_FILE_TYPE)
                    true
                } catch (e: Exception) {
                    log("Error: ${e.message}")
                    false
                }
            }
            _isConnected.value = ok
            log(if (ok) "Connected" else "Connection failed")
            _busy.value = false
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            _busy.value = true
            withContext(Dispatchers.IO) {
                runCatching { if (ftp.isConnected) { ftp.logout(); ftp.disconnect() } }
            }
            _isConnected.value = false
            log("Disconnected")
            _busy.value = false
        }
    }

    fun upload(context: Context, uri: Uri, remotePath: String) {
        viewModelScope.launch {
            _busy.value = true
            _progress.value = 0f
            withContext(Dispatchers.IO) {
                try {
                    val size = context.contentResolver
                        .openFileDescriptor(uri, "r")
                        ?.use { it.statSize.toFloat() } ?: 0f

                    val input = context.contentResolver.openInputStream(uri)
                        ?: return@withContext also { log("Cannot open file") }

                    val output = ftp.storeFileStream(remotePath)
                        ?: return@withContext also { input.close(); log("FTP stream error") }

                    val buffer = ByteArray(8192)
                    var transferred = 0f
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        transferred += read
                        if (size > 0f) _progress.value = transferred / size
                    }
                    input.close()
                    output.close()
                    ftp.completePendingCommand()
                    log("Upload complete")
                } catch (e: Exception) {
                    log("Upload error: ${e.message}")
                }
            }
            _progress.value = 1f
            _busy.value = false
        }
    }

    fun download(context: Context, remotePath: String) {
        viewModelScope.launch {
            _busy.value = true
            _progress.value = 0f
            withContext(Dispatchers.IO) {
                try {
                    // Get accurate file size before transfer
                    ftp.sendCommand("SIZE", remotePath)
                    val sizeReply = ftp.replyString.trim()
                    val totalSize = if (sizeReply.startsWith("213"))
                        sizeReply.substring(4).trim().toLongOrNull() ?: 0L
                    else 0L

                    val input = ftp.retrieveFileStream(remotePath)
                        ?: return@withContext also { log("Remote file not found") }

                    val file = File(
                        context.getExternalFilesDir(null),
                        remotePath.substringAfterLast("/")
                    )
                    val output = file.outputStream()
                    val buffer = ByteArray(8192)
                    var transferred = 0L
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        transferred += read
                        if (totalSize > 0) _progress.value = transferred.toFloat() / totalSize
                    }
                    input.close()
                    output.close()
                    ftp.completePendingCommand()
                    log("Downloaded → ${file.absolutePath}")
                } catch (e: Exception) {
                    log("Download error: ${e.message}")
                }
            }
            _progress.value = 1f
            _busy.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        runCatching { if (ftp.isConnected) ftp.disconnect() }
    }
}
