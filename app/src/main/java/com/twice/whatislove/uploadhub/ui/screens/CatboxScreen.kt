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
import com.twice.whatislove.uploadhub.catbox.CatboxApi
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun CatboxScreen() {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    var selectedFile by remember { mutableStateOf<File?>(null) }
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
            text = "Catbox Upload",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Upload files anonymously to catbox.moe. Uploaded files are publicly accessible via a direct link.",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(24.dp))

        // File info
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
                                val url = CatboxApi.uploadFile(file) { sent, total ->
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