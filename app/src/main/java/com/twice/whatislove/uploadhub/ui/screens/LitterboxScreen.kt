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
import com.twice.whatislove.uploadhub.litterbox.LitterboxApi
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun LitterboxScreen() {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    var selectedFile by remember { mutableStateOf<File?>(null) }
    var retentionTime by remember { mutableStateOf("72h") }
    var status by remember { mutableStateOf("Idle") }
    var progress by remember { mutableStateOf(0f) }
    var uploadedUrl by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let {
                selectedFile = File(uri.path ?: "")
                uploadedUrl = null
                status = "File selected: ${selectedFile?.name}"
                progress = 0f
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Litterbox Upload",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Upload files temporarily using Litterbox. Files are automatically deleted after the selected retention time.",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Retention time selector
        Text("Retention time:", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("24h", "48h", "72h", "1w").forEach { time ->
                Button(
                    onClick = { retentionTime = time },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (retentionTime == time) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text(time)
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
                    selectedFile?.let { file ->
                        coroutineScope.launch {
                            status = "Uploading..."
                            progress = 0f
                            try {
                                val url = LitterboxApi.upload(file, retentionTime) { sent, total ->
                                    progress = if (total > 0) sent / total.toFloat() else 0f
                                }
                                uploadedUrl = url
                                status = "Upload successful!"
                            } catch (e: Exception) {
                                status = "Upload failed: ${e.message}"
                            }
                        }
                    }
                },
                enabled = selectedFile != null
            ) {
                Text("Upload")
            }
        }

        uploadedUrl?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "URL: $it",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}