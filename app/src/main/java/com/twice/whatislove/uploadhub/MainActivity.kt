package com.twice.whatislove.uploadhub

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import com.twice.whatislove.uploadhub.ui.theme.UploadHubThemeM3
import com.twice.whatislove.uploadhub.navigation.UploadHubNavHost

class MainActivity : ComponentActivity() {

    // Launcher to request POST_NOTIFICATIONS permission
    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            // Optional: do something if granted or denied
            if (isGranted) {
                // Permission granted, your FtpServerService can post notifications
            } else {
                // Permission denied: notifications won't appear on Android 13+
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request POST_NOTIFICATIONS on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            UploadHubThemeM3 {
                UploadHubNavHost()
            }
        }
    }
}