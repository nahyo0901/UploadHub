package com.twice.whatislove.uploadhub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.twice.whatislove.uploadhub.ui.theme.UploadHubThemeM3
import com.twice.whatislove.uploadhub.navigation.UploadHubNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UploadHubThemeM3 {
                UploadHubNavHost()
            }
        }
    }
}
