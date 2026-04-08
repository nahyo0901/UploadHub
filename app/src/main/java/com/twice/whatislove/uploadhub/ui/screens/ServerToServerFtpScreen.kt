package com.twice.whatislove.uploadhub.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.InputStream

/* ---------------------------------------------------
   SAFELY GET FILE NAME FROM URI (Modern Android Safe)
--------------------------------------------------- */
fun getFileName(context: Context, uri: Uri): String {
    return try {
        var name: String? = null
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) {
                name = cursor.getString(index)
            }
        }
        name ?: uri.lastPathSegment?.substringAfterLast('/') ?: "file_${System.currentTimeMillis()}"
    } catch (e: Exception) {
        "file_${System.currentTimeMillis()}"
    }
}

/* ---------------------------------------------------
   SERVER → SERVER FTP SCREEN
--------------------------------------------------- */
@Composable
fun ServerToServerFtpScreen() {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()

    // File picker state
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("No file selected") }

    // FTP server 1
    var host1 by remember { mutableStateOf("") }
    var user1 by remember { mutableStateOf("") }
    var pass1 by remember { mutableStateOf("") }

    // FTP server 2
    var host2 by remember { mutableStateOf("") }
    var user2 by remember { mutableStateOf("") }
    var pass2 by remember { mutableStateOf("") }

    var status by remember { mutableStateOf("Idle") }
    var progress by remember { mutableStateOf(0f) }

    /* FILE PICKER (MODERN ANDROID SAFE) */
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedUri = it
            selectedFileName = getFileName(context, it)
            status = "Selected: $selectedFileName"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(24.dp)
    ) {

        Text("Server → Server FTP", style = MaterialTheme.typography.headlineMedium)

        Spacer(Modifier.height(16.dp))

        /* ---------------- SERVER 1 ---------------- */
        Text("Source FTP Server", style = MaterialTheme.typography.titleMedium)

        OutlinedTextField(host1, { host1 = it }, label = { Text("Host (ftp.example.com)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(user1, { user1 = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(pass1, { pass1 = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(20.dp))

        /* ---------------- SERVER 2 ---------------- */
        Text("Destination FTP Server", style = MaterialTheme.typography.titleMedium)

        OutlinedTextField(host2, { host2 = it }, label = { Text("Host") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(user2, { user2 = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(pass2, { pass2 = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(20.dp))

        /* FILE PICKER */
        Button(onClick = { picker.launch("*/*") }) {
            Text("Select Local File (optional)")
        }

        Text(selectedFileName)

        Spacer(Modifier.height(12.dp))

        LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(12.dp))
        Text("Status: $status")

        Spacer(Modifier.height(20.dp))

        /* TRANSFER BUTTON */
        Button(
            onClick = {
                scope.launch(Dispatchers.IO) {
                    try {
                        status = "Connecting to servers..."
                        progress = 0.1f

                        // If user selected a file → upload from phone to server2
                        if (selectedUri != null) {
                            status = "Uploading phone → FTP server..."
                            val stream: InputStream? =
                                context.contentResolver.openInputStream(selectedUri!!)

                            if (stream == null) {
                                status = "Failed to open file stream"
                                return@launch
                            }

                            // TODO: connect FTP + upload stream to server2
                            // ServerToServerFTP.uploadStreamToServer2(stream, selectedFileName, host2, user2, pass2)

                            progress = 1f
                            status = "Upload complete (stub)"
                        } else {
                            // Server → Server transfer mode
                            status = "Starting server → server transfer..."

                            // TODO: call your Kotlin port here:
                            // ServerToServerFTP.transfer(server1, user1, pass1, file1, server2, user2, pass2, file2)

                            progress = 1f
                            status = "Server → Server transfer complete (stub)"
                        }

                    } catch (e: Exception) {
                        status = "Error: ${e.message}"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Start Transfer")
        }

        Spacer(Modifier.height(40.dp))
    }
}