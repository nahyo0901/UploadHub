package dev.nahyo0901.uploadhub.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dev.nahyo0901.uploadhub.datastore.SettingsDataStore

private val DarkColors  = darkColorScheme()
private val LightColors = lightColorScheme()

@Composable
fun UploadHubThemeM3(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val store   = remember { SettingsDataStore(context) }
    val dark    by store.darkMode.collectAsState(initial = false)
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        content     = content
    )
}
