package com.twice.whatislove.uploadhub.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPReply
import java.io.File
import java.io.InputStream
import java.io.OutputStream

@Composable
fun FtpClientScreen() {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()

    val ftpClient = remember { FTPClient() }

    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("21") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var remotePath by remember { mutableStateOf("") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }

    var isConnected by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var logs by remember { mutableStateOf(listOf<String>()) }
    var busy by remember { mutableStateOf(false) }

    fun log(msg: String) {
        logs = logs + msg
    }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        selectedUri = uri
        uri?.let { log("Selected: $it") }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(16.dp)
    ) {

        Text("FTP Client", style = MaterialTheme.typography.headlineMedium)

        Spacer(Modifier.height(16.dp))

        /* ================= CONNECTION ================= */

        Text("Connection", style = MaterialTheme.typography.titleMedium)

        OutlinedTextField(host, { host = it }, label = { Text("Host") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(port, { port = it }, label = { Text("Port") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(username, { username = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(password, { password = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

            Button(
                enabled = !isConnected && !busy,
                onClick = {
                    scope.launch {
                        busy = true
                        log("Connecting...")

                        val ok = ftpConnect(
                            ftpClient,
                            host,
                            port.toIntOrNull() ?: 21,
                            username,
                            password,
                            ::log
                        )

                        isConnected = ok
                        busy = false
                    }
                }
            ) {
                Text("Connect")
            }

            Button(
                enabled = isConnected && !busy,
                onClick = {
                    scope.launch {
                        busy = true
                        ftpDisconnect(ftpClient, ::log)
                        isConnected = false
                        busy = false
                    }
                }
            ) {
                Text("Disconnect")
            }
        }

        Spacer(Modifier.height(20.dp))

        /* ================= TRANSFER ================= */

        Text("File Transfer", style = MaterialTheme.typography.titleMedium)

        OutlinedTextField(
            remotePath,
            { remotePath = it },
            label = { Text("Remote Path") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

            Button(
                enabled = isConnected && !busy,
                onClick = { picker.launch("*/*") }
            ) {
                Text("Select File")
            }

            Button(
                enabled = isConnected && selectedUri != null && !busy,
                onClick = {
                    val uri = selectedUri ?: return@Button

                    scope.launch {
                        busy = true
                        progress = 0f

                        ftpUpload(
                            context,
                            ftpClient,
                            uri,
                            remotePath,
                            ::log
                        ) {
                            progress = it
                        }

                        busy = false
                    }
                }
            ) {
                Text("Upload")
            }

            Button(
                enabled = isConnected && !busy,
                onClick = {
                    scope.launch {
                        busy = true
                        progress = 0f

                        ftpDownload(
                            context,
                            ftpClient,
                            remotePath,
                            ::log
                        ) {
                            progress = it
                        }

                        busy = false
                    }
                }
            ) {
                Text("Download")
            }
        }

        Spacer(Modifier.height(16.dp))

        /* ================= PROGRESS ================= */

        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        /* ================= LOGS ================= */

        Text("Logs")

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFEFEFEF))
                .padding(8.dp)
        ) {
            logs.forEach { Text(it) }
        }
    }
}

/* ============================================================
   FTP HELPERS
   ============================================================ */

suspend fun ftpConnect(
    ftp: FTPClient,
    host: String,
    port: Int,
    user: String,
    pass: String,
    log: (String) -> Unit
): Boolean = withContext(Dispatchers.IO) {
    try {
        ftp.connect(host, port)

        if (!FTPReply.isPositiveCompletion(ftp.replyCode)) {
            log("Connection refused")
            return@withContext false
        }

        if (!ftp.login(user, pass)) {
            log("Login failed")
            return@withContext false
        }

        ftp.enterLocalPassiveMode()
        ftp.setFileType(FTP.BINARY_FILE_TYPE)

        log("Connected")
        true
    } catch (e: Exception) {
        log("Error: ${e.message}")
        false
    }
}

suspend fun ftpDisconnect(
    ftp: FTPClient,
    log: (String) -> Unit
) = withContext(Dispatchers.IO) {
    try {
        if (ftp.isConnected) {
            ftp.logout()
            ftp.disconnect()
            log("Disconnected")
        }
    } catch (_: Exception) {}
}

/* ============================================================
   UPLOAD
   ============================================================ */

suspend fun ftpUpload(
    context: Context,
    ftp: FTPClient,
    uri: Uri,
    remotePath: String,
    log: (String) -> Unit,
    onProgress: (Float) -> Unit
) = withContext(Dispatchers.IO) {

    val descriptor = context.contentResolver.openFileDescriptor(uri, "r")
    val size = descriptor?.statSize?.toFloat() ?: 0f
    descriptor?.close()

    val input = context.contentResolver.openInputStream(uri)
    if (input == null) {
        log("Cannot open file")
        return@withContext
    }

    val output = ftp.storeFileStream(remotePath)
    if (output == null) {
        input.close()
        log("FTP stream error")
        return@withContext
    }

    val buffer = ByteArray(8192)
    var transferred = 0f
    var read: Int

    while (input.read(buffer).also { read = it } != -1) {
        output.write(buffer, 0, read)
        transferred += read
        if (size > 0f) onProgress(transferred / size)
    }

    input.close()
    output.close()
    ftp.completePendingCommand()

    log("Upload completed")
}

/* ============================================================
   DOWNLOAD
   ============================================================ */

suspend fun ftpDownload(
    context: Context,
    ftp: FTPClient,
    remotePath: String,
    log: (String) -> Unit,
    onProgress: (Float) -> Unit
) = withContext(Dispatchers.IO) {

    val input: InputStream = ftp.retrieveFileStream(remotePath)
        ?: run {
            log("Remote file not found")
            return@withContext
        }

    val file = File(
        context.getExternalFilesDir(null),
        remotePath.substringAfterLast("/")
    )

    val output: OutputStream = file.outputStream()

    val buffer = ByteArray(8192)
    var transferred = 0f
    var read: Int

    while (input.read(buffer).also { read = it } != -1) {
        output.write(buffer, 0, read)
        transferred += read
        onProgress(transferred / (transferred + 1))
    }

    input.close()
    output.close()
    ftp.completePendingCommand()

    log("Downloaded → ${file.absolutePath}")
}