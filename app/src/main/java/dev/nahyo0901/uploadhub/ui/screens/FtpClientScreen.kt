package dev.nahyo0901.uploadhub.ui.screens

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
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.nahyo0901.uploadhub.ui.viewmodel.FtpClientViewModel

@Composable
fun FtpClientScreen(vm: FtpClientViewModel = viewModel()) {

    val context     = LocalContext.current
    val scroll      = rememberScrollState()

    val isConnected by vm.isConnected.collectAsState()
    val busy        by vm.busy.collectAsState()
    val progress    by vm.progress.collectAsState()
    val logs        by vm.logs.collectAsState()

    var host       by remember { mutableStateOf("") }
    var port       by remember { mutableStateOf("21") }
    var username   by remember { mutableStateOf("") }
    var password   by remember { mutableStateOf("") }
    var remotePath by remember { mutableStateOf("") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedUri = uri
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
        OutlinedTextField(host,     { host = it },     label = { Text("Host") },     modifier = Modifier.fillMaxWidth())
        OutlinedTextField(port,     { port = it },     label = { Text("Port") },     modifier = Modifier.fillMaxWidth())
        OutlinedTextField(username, { username = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(password, { password = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                enabled = !isConnected && !busy,
                onClick = { vm.connect(host, port.toIntOrNull() ?: 21, username, password) }
            ) { Text("Connect") }

            Button(
                enabled = isConnected && !busy,
                onClick = { vm.disconnect() }
            ) { Text("Disconnect") }
        }

        Spacer(Modifier.height(20.dp))

        /* ================= TRANSFER ================= */

        Text("File Transfer", style = MaterialTheme.typography.titleMedium)

        OutlinedTextField(
            remotePath, { remotePath = it },
            label = { Text("Remote Path") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                enabled = isConnected && !busy,
                onClick = { picker.launch("*/*") }
            ) { Text("Select File") }

            Button(
                enabled = isConnected && selectedUri != null && !busy,
                onClick = { vm.upload(context, selectedUri!!, remotePath) }
            ) { Text("Upload") }

            Button(
                enabled = isConnected && !busy,
                onClick = { vm.download(context, remotePath) }
            ) { Text("Download") }
        }

        Spacer(Modifier.height(16.dp))

        /* ================= PROGRESS ================= */

        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        /* ================= LOGS ================= */

        Text("Logs", style = MaterialTheme.typography.titleMedium)
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
