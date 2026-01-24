package com.twice.whatislove.uploadhub.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun RcloneScreen() {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    var selectedFile by remember { mutableStateOf<File?>(null) }
    var selectedRemote by remember { mutableStateOf("None") }
    var status by remember { mutableStateOf("Idle") }
    var progress by remember { mutableStateOf(0f) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let {
                selectedFile = File(uri.path ?: "")
                status = "File selected: ${selectedFile?.name}"
                progress = 0f
            }
        }
    )

    val remotes = listOf("Google Drive", "S3", "WebDAV", "Dropbox")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Rclone",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Sync and upload files using rclone-compatible cloud remotes such as Google Drive, S3, or WebDAV.",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Remote selection
        Text("Selected remote:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            remotes.forEach { remote ->
                Button(
                    onClick = { selectedRemote = remote },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedRemote == remote) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text(remote)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Selected file info
        Text(
            text = selectedFile?.name ?: "No file selected",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Status
        Text(
            text = "Status: $status",
            style = MaterialTheme.typography.bodyMedium
        )

        if (progress in 0f..1f) {
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Buttons
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { filePickerLauncher.launch("*/*") }) {
                Text("Select File")
            }

            Button(
                onClick = {
                    if (selectedFile != null && selectedRemote != "None") {
                        coroutineScope.launch {
                            status = "Uploading to $selectedRemote..."
                            progress = 0f
                            // Dummy upload simulation
                            repeat(20) {
                                delay(100)
                                progress += 0.05f
                            }
                            progress = 1f
                            status = "Upload to $selectedRemote complete!"
                        }
                    } else {
                        status = "Select file and remote first"
                    }
                },
                enabled = selectedFile != null && selectedRemote != "None"
            ) {
                Text("Upload / Sync")
            }
        }
    }
}