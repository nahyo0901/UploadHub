package dev.nahyo0901.uploadhub.ui.screens

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
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
import androidx.core.content.ContextCompat
import dev.nahyo0901.uploadhub.network.ftp.server.FtpServerService
import java.io.File

@Composable
fun FtpServerScreen() {

    val context    = LocalContext.current
    val scrollState = rememberScrollState()

    var isRunning  by remember { mutableStateOf(false) }
    var port       by remember { mutableStateOf("2121") }
    var rootDirUri by remember { mutableStateOf<Uri?>(null) }
    var logs       by remember { mutableStateOf(listOf<String>()) }

    fun log(msg: String) { logs = logs + msg }

    // =============================
    // Folder Picker (SAF)
    // =============================

    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        rootDirUri = uri
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            log("Root folder selected")
        }
    }

    // =============================
    // Broadcast Receiver
    // =============================

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
        ContextCompat.registerReceiver(
            context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED
        )
        onDispose { context.unregisterReceiver(receiver) }
    }

    // =============================
    // UI
    // =============================

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text("FTP Server", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

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

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

            Button(
                enabled = !isRunning,
                onClick = {
                    // Resolve SAF tree Uri to a real File path for primary storage
                    val rootFile = rootDirUri?.let { getRealPathFromTreeUri(it) }
                    FtpServerService.startService(
                        context,
                        port.toIntOrNull() ?: 2121,
                        rootFile          // null → service falls back to filesDir/ftp_root
                    )
                }
            ) { Text("Start Server") }

            Button(
                enabled = isRunning,
                onClick = { FtpServerService.stopService(context) }
            ) { Text("Stop Server") }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text  = if (isRunning) "Status: RUNNING" else "Status: STOPPED",
            color = if (isRunning) Color(0xFF2E7D32) else Color.Red
        )

        Spacer(Modifier.height(20.dp))

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

/**
 * Converts a SAF OpenDocumentTree Uri to a real File path.
 * Works for primary (internal) storage only.
 * Returns null for SD cards — the service then falls back to its own filesDir/ftp_root.
 */
private fun getRealPathFromTreeUri(treeUri: Uri): File? {
    return try {
        val docId    = DocumentsContract.getTreeDocumentId(treeUri)
        val split    = docId.split(":")
        val type     = split.getOrNull(0) ?: return null
        val relative = split.getOrNull(1) ?: ""
        if (type.equals("primary", ignoreCase = true)) {
            File(Environment.getExternalStorageDirectory(), relative)
        } else null
    } catch (_: Exception) { null }
}
