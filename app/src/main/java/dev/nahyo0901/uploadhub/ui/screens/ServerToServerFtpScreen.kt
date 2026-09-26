package dev.nahyo0901.uploadhub.ui.screens

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
import dev.nahyo0901.uploadhub.network.ftp.servertoserver.ServerToServerFTP
import dev.nahyo0901.uploadhub.datastore.TransferOffsetDataStore
import kotlinx.coroutines.launch

@Composable
fun ServerToServerFtpScreen() {
    val context = LocalContext.current
    val offsetDataStore = TransferOffsetDataStore(context)
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    var server1 by remember { mutableStateOf("") }
    var user1 by remember { mutableStateOf("") }
    var pass1 by remember { mutableStateOf("") }
    var remoteFile1 by remember { mutableStateOf("") }

    var server2 by remember { mutableStateOf("") }
    var user2 by remember { mutableStateOf("") }
    var pass2 by remember { mutableStateOf("") }
    var remoteFile2 by remember { mutableStateOf("") }

    var status by remember { mutableStateOf("Idle") }
    var progress by remember { mutableStateOf(0f) }
    var percentage by remember { mutableStateOf(0) }
    var logs by remember { mutableStateOf(listOf<String>()) }
    var isTransferring by remember { mutableStateOf(false) }
    var isCancelled by remember { mutableStateOf(false) }
    var totalTransferred by remember { mutableStateOf(0L) }
    var fileSize by remember { mutableStateOf(0L) }

    fun log(message: String) {
        logs = logs + message
    }

    val transferId = "$server1-$remoteFile1->$server2-$remoteFile2"

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                remoteFile1 = getFileName(context, it)
                log("Selected file: $remoteFile1")
            }
        }
    )

    // Load saved offset
    LaunchedEffect(transferId) {
        val savedOffset = offsetDataStore.getOffsetOnce(transferId)
        totalTransferred = savedOffset
        if (savedOffset > 0) log("Resuming transfer from $savedOffset bytes")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text("Server → Server FTP", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        // Server1
        OutlinedTextField(server1, { server1 = it }, label = { Text("Server1 (host:port)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(user1, { user1 = it }, label = { Text("User1") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(pass1, { pass1 = it }, label = { Text("Password1") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(remoteFile1, { remoteFile1 = it }, label = { Text("Remote file path on Server1") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(12.dp))

        // Server2
        OutlinedTextField(server2, { server2 = it }, label = { Text("Server2 (host:port)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(user2, { user2 = it }, label = { Text("User2") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(pass2, { pass2 = it }, label = { Text("Password2") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(remoteFile2, { remoteFile2 = it }, label = { Text("Target file path on Server2") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))

        // Status & Progress
        Text("Status: $status")
        LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
        Text("Progress: $percentage % ($totalTransferred / $fileSize bytes)")

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    if (!isTransferring) {
                        isCancelled = false
                        isTransferring = true
                        logs = emptyList()
                        coroutineScope.launch {
                            try {
                                status = "Connecting..."
                                ServerToServerFTP().transferServerToServerWithProgress(
                                    context,
                                    server1, user1, pass1, remoteFile1,
                                    server2, user2, pass2, remoteFile2,
                                    onProgress = { transferred, size ->
                                        totalTransferred = transferred
                                        fileSize = size
                                        progress = if (size > 0) transferred.toFloat() / size else 0f
                                        percentage = if (size > 0) (progress * 100).toInt() else 0
                                    },
                                    onLog = { log(it) },
                                    isCancelled = { isCancelled },
                                    transferId = transferId,
                                    offsetDataStore = offsetDataStore
                                )
                                status = "Transfer Complete"
                            } catch (e: Exception) {
                                status = "Error: ${e.message}"
                                log("Transfer failed: ${e.message}")
                            } finally {
                                isTransferring = false
                            }
                        }
                    }
                },
                enabled = !isTransferring
            ) { Text("Start Transfer") }

            Button(
                onClick = { isCancelled = true },
                enabled = isTransferring
            ) { Text("Cancel") }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Logs:", style = MaterialTheme.typography.titleMedium)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFEEEEEE))
                .padding(8.dp)
        ) {
            logs.forEach { Text(it) }
        }
    }
}

fun getFileName(context: Context, uri: Uri): String {
    var name = "unknown"
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index >= 0 && it.moveToFirst()) name = it.getString(index)
    }
    return name
}