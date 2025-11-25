package com.twice.whatislove.uploadhub.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme()
private val LightColors = lightColorScheme()

@Composable
fun UploadHubThemeM3(content: @Composable () -> Unit) {
    val dark = false
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        content = content
    )
}
