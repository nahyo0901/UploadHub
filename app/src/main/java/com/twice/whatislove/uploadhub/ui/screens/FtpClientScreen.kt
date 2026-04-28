package com.twice.whatislove.uploadhub.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

data class FtpFileItem(
    val name: String,
    val isDirectory: Boolean
)

@Composable
fun FtpClientScreen() {

    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("21") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var isConnected by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }

    var files by remember {
        mutableStateOf(
            listOf<FtpFileItem>()
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text("FTP Client", style = MaterialTheme.typography.headlineMedium)

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = host,
            onValueChange = { host = it },
            label = { Text("Host") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = port,
            onValueChange = { port = it },
            label = { Text("Port") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                isConnected = !isConnected
                if (isConnected) {
                    files = listOf(
                        FtpFileItem("Documents", true),
                        FtpFileItem("photo.jpg", false),
                        FtpFileItem("backup.zip", false)
                    )
                } else {
                    files = emptyList()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isConnected) "Disconnect" else "Connect")
        }

        Spacer(Modifier.height(16.dp))

        if (isLoading) {
            Column {
                Text("Transfer progress")

                Spacer(Modifier.height(8.dp))

                // ⭐ THE ONLY SAFE WAY TO CALL THIS
                LinearProgressIndicator(
                    progress = progress.coerceIn(0f, 1f),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        if (isConnected) {
            Text("Files", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            LazyColumn {
                items(files) { file ->
                    FileRow(
                        file = file,
                        onDownload = {
                            isLoading = true
                            progress = 0f

                            // fake progress for UI preview
                            LaunchedEffect(Unit) {
                                for (i in 1..100) {
                                    progress = i / 100f
                                    kotlinx.coroutines.delay(20)
                                }
                                isLoading = false
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun FileRow(
    file: FtpFileItem,
    onDownload: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(if (file.isDirectory) "📁 ${file.name}" else "📄 ${file.name}")

            if (!file.isDirectory) {
                Button(onClick = onDownload) {
                    Text("Download")
                }
            }
        }
    }
}