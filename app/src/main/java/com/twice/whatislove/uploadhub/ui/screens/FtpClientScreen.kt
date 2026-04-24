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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.*
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/* ============================================================
   FTP CLIENT SCREEN
   ============================================================ */

@Composable
fun FtpClientScreen() {

    val context = LocalContext.current
    val scroll = rememberScrollState()

    val ftpClient = remember { FTPClient() }

    // connection state
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("21") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // transfer state
    var remotePath by remember { mutableStateOf("") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }

    // ui state
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
        uri?.let { log("Selected: $uri") }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(scroll).padding(16.dp)
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

            Button(enabled = !isConnected && !busy, onClick = {
                busy = true
                log("Connecting...")
                LaunchedEffect(Unit) {
                    val ok = ftpConnect(ftpClient, host, port.toInt(), username, password, ::log)
                    isConnected = ok
                    busy = false
                }
            }) { Text("Connect") }

            Button(enabled = isConnected && !busy, onClick = {
                busy = true
                LaunchedEffect(Unit) {
                    ftpDisconnect(ftpClient, ::log)
                    isConnected = false
                    busy = false
                }
            }) { Text("Disconnect") }
        }

        Spacer(Modifier.height(20.dp))

        /* ================= TRANSFER ================= */

        Text("File Transfer", style = MaterialTheme.typography.titleMedium)

        OutlinedTextField(
            remotePath,
            { remotePath = it },
            label = { Text("Remote Path (ex: /file.zip)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = isConnected && !busy, onClick = { picker.launch("*/*") }) {
                Text("Select File")
            }

            Button(
                enabled = isConnected && selectedUri != null && !busy,
                onClick = {
                    busy = true
                    progress = 0f
                    LaunchedEffect(Unit) {
                        ftpUpload(context, ftpClient, selectedUri!!, remotePath, ::log) {
                            progress = it
                        }
                        busy = false
                    }
                }
            ) { Text("Upload") }

            Button(
                enabled = isConnected && !busy,
                onClick = {
                    busy = true
                    progress = 0f
                    LaunchedEffect(Unit) {
                        ftpDownload(context, ftpClient, remotePath, ::log) {
                            progress = it
                        }
                        busy = false
                    }
                }
            ) { Text("Download") }
        }

        Spacer(Modifier.height(16.dp))
        LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(16.dp))

        Text("Logs")
        Column(
            Modifier.fillMaxWidth().background(Color(0xFFEFEFEF)).padding(8.dp)
        ) {
            logs.forEach { Text(it) }
        }
    }
}

/* ============================================================
   FTP CONNECTION HELPERS
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
   FTP TRANSFER HELPERS
   ============================================================ */

suspend fun ftpUpload(
    context: Context,
    ftp: FTPClient,
    uri: Uri,
    remotePath: String,
    log: (String) -> Unit,
    onProgress: (Float) -> Unit
) = withContext(Dispatchers.IO) {

    val size = context.contentResolver.openFileDescriptor(uri, "r")!!.statSize.toFloat()
    val input = context.contentResolver.openInputStream(uri)!!

    val output: OutputStream = ftp.storeFileStream(remotePath)
    val buffer = ByteArray(8192)

    var transferred = 0f
    var read: Int

    while (input.read(buffer).also { read = it } != -1) {
        output.write(buffer, 0, read)
        transferred += read
        onProgress(transferred / size)
    }

    output.close()
    input.close()
    ftp.completePendingCommand()

    log("Upload completed")
}

suspend fun ftpDownload(
    context: Context,
    ftp: FTPClient,
    remotePath: String,
    log: (String) -> Unit,
    onProgress: (Float) -> Unit
) = withContext(Dispatchers.IO) {

    val size = ftp.mlistFile(remotePath)?.size?.toFloat() ?: 0f
    val input: InputStream = ftp.retrieveFileStream(remotePath)

    val file = File(context.getExternalFilesDir(null), remotePath.substringAfterLast("/"))
    val output = file.outputStream()

    val buffer = ByteArray(8192)
    var transferred = 0f
    var read: Int

    while (input.read(buffer).also { read = it } != -1) {
        output.write(buffer, 0, read)
        transferred += read
        if (size > 0) onProgress(transferred / size)
    }

    output.close()
    input.close()
    ftp.completePendingCommand()

    log("Downloaded → ${file.absolutePath}")
}
