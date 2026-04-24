package com.twice.whatislove.uploadhub.ui.screens

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import com.twice.whatislove.uploadhub.network.ftp.server.FtpServerService
import java.io.File

@Composable
fun FtpServerScreen() {

    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // ----------------------------
    // UI STATE
    // ----------------------------

    var isRunning by remember { mutableStateOf(false) }
    var port by remember { mutableStateOf("2121") }
    var rootDirUri by remember { mutableStateOf<Uri?>(null) }
    var logs by remember { mutableStateOf(listOf<String>()) }

    fun log(msg: String) {
        logs = logs + msg
    }

    // ----------------------------
    // Folder picker
    // ----------------------------

    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        rootDirUri = uri
        uri?.let { log("Selected root folder") }
    }

    // ----------------------------
    // Listen to Service broadcasts
    // ----------------------------

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action == FtpServerService.ACTION_STATUS) {
                    isRunning = intent.getBooleanExtra(FtpServerService.EXTRA_RUNNING, false)
                    val msg = intent.getStringExtra(FtpServerService.EXTRA_LOG) ?: ""
                    if (msg.isNotBlank()) log(msg)
                }
            }
        }

        val filter = IntentFilter(FtpServerService.ACTION_STATUS)
        context.registerReceiver(receiver, filter)

        onDispose { context.unregisterReceiver(receiver) }
    }

    // ----------------------------
    // UI
    // ----------------------------

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.Start
    ) {

        Text("FTP Server", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        // ================= SETTINGS =================

        Text("Server Settings", style = MaterialTheme.typography.titleMedium)

        OutlinedTextField(
            value = port,
            onValueChange = { port = it },
            label = { Text("Port") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        Button(onClick = { folderPicker.launch(null) }) {
            Text("Choose Root Folder")
        }

        rootDirUri?.let {
            Text("Root folder selected", color = Color.Gray)
        }

        Spacer(Modifier.height(16.dp))

        // ================= CONTROLS =================

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

            Button(
                enabled = !isRunning,
                onClick = {
                    val rootFile = rootDirUri?.let { File(it.path ?: "") }
                    FtpServerService.startService(
                        context,
                        port.toIntOrNull() ?: 2121,
                        rootFile
                    )
                }
            ) { Text("Start Server") }

            Button(
                enabled = isRunning,
                onClick = {
                    FtpServerService.stopService(context)
                }
            ) { Text("Stop Server") }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            if (isRunning) "Status: RUNNING" else "Status: STOPPED",
            color = if (isRunning) Color(0xFF2E7D32) else Color.Red
        )

        Spacer(Modifier.height(20.dp))

        // ================= LOG VIEW =================

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
