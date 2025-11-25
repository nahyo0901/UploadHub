package com.twice.whatislove.uploadhub.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(onCatbox: () -> Unit, onSettings: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.padding(16.dp)) {
            Text("UploadHub")
            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = onCatbox) { Text("Open Catbox") }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onSettings) { Text("Settings") }
        }
    }
}
