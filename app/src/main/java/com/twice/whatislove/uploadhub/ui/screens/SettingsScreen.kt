package com.twice.whatislove.uploadhub.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {

        // Title
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        // App section
        SectionHeader("App")

        SettingItem(
            title = "Theme",
            description = "Follow system"
        )

        SettingItem(
            title = "Keep screen on during uploads",
            description = "Disabled"
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Upload section
        SectionHeader("Uploads")

        SettingItem(
            title = "Default upload service",
            description = "Catbox"
        )

        SettingItem(
            title = "Confirm before upload",
            description = "Enabled"
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Network section
        SectionHeader("Network")

        SettingItem(
            title = "Wi-Fi only uploads",
            description = "Disabled"
        )

        SettingItem(
            title = "Background uploads",
            description = "Enabled"
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Info section
        SectionHeader("About")

        SettingItem(
            title = "Version",
            description = "0.1.0 (dev)"
        )

        SettingItem(
            title = "Open source licenses",
            description = "View licenses"
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun SettingItem(
    title: String,
    description: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall
        )
    }
}