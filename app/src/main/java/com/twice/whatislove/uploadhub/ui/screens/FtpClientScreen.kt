package com.twice.whatislove.uploadhub.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
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
import kotlinx.coroutines.launch
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPReply
import java.io.InputStream
import java.io.OutputStream

@Composable
fun FtpClientScreen() {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()

    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("21") }
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var remotePath by remember { mutableStateOf("") }

    var isConnected by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var logs by remember { mutableStateOf(listOf<String>()) }

    val ftpClient = remember { FTPClient() }

    fun log(msg: String) {
        logs = logs + msg
    }

    // file picker for upload
    var selectedUri by remember { mutableStateOf<Uri?>(null) }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        selectedUri = uri
        log("Selected file: $uri")
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(scroll).padding(16.dp),
        horizontalAlignment = Alignment.Start
    ) {

        Text("FTP Client", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        // CONNECTION SECTION
        Text("Connection", style = MaterialTheme.typography.titleMedium)

        OutlinedTextField(host, { host = it }, label = { Text("Host") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(port, { port = it }, label = { Text("Port") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(user, { user = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(pass, { pass = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

            Button(onClick = {
                scope.launch(Dispatchers.IO) {
                    try {
                        ftpClient.connect(host, port.toInt())
                        if (!FTPReply.isPositiveCompletion(ftpClient.replyCode)) {
                            log("Connection refused")
                            return@launch
                        }
                        ftpClient.login(user, pass)
                        ftpClient.enterLocalPassiveMode()
                        isConnected = true
                        log("Connected to FTP server")
                    } catch (e: Exception) {
                        log("Error: ${e.message}")
                    }
                }
            }, enabled = !isConnected) {
                Text("Connect")
            }

            Button(onClick = {
                scope.launch(Dispatchers.IO) {
                    ftpClient.logout()
                    ftpClient.disconnect()
                    isConnected = false
                    log("Disconnected")
                }
            }, enabled = isConnected) {
                Text("Disconnect")
            }
        }

        Spacer(Modifier.height(20.dp))

        // TRANSFER SECTION
        Text("File Transfer", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            remotePath,
            { remotePath = it },
            label = { Text("Remote path (ex: /test.zip)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

            Button(
                enabled = isConnected,
                onClick = { filePicker.launch("*/*") }
            ) {
                Text("Select File")
            }

            Button(
                enabled = isConnected && selectedUri != null,
                onClick = {
                    scope.launch(Dispatchers.IO) {
                        uploadFile(context, ftpClient, selectedUri!!, remotePath,
                            onProgress = { progress = it },
                            onLog = { log(it) })
                    }
                }
            ) { Text("Upload") }

            Button(
                enabled = isConnected,
                onClick = {
                    scope.launch(Dispatchers.IO) {
                        downloadFile(context, ftpClient, remotePath,
                            onProgress = { progress = it },
                            onLog = { log(it) })
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
