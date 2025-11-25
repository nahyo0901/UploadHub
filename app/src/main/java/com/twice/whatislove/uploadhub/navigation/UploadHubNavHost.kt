package com.twice.whatislove.uploadhub.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.twice.whatislove.uploadhub.ui.about.AboutScreen
import com.twice.whatislove.uploadhub.ui.catbox.CatboxScreen
import com.twice.whatislove.uploadhub.ui.home.HomeScreen
import com.twice.whatislove.uploadhub.ui.litterbox.LitterboxScreen
import com.twice.whatislove.uploadhub.ui.rclone.RcloneScreen
import com.twice.whatislove.uploadhub.ui.settings.SettingsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadHubNavHost() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "home") {
        composable("home") { HomeScreen(onCatbox = { nav.navigate("catbox") }, onSettings = { nav.navigate("settings") }) }
        composable("catbox") { CatboxScreen() }
        composable("litterbox") { LitterboxScreen() }
        composable("rclone") { RcloneScreen() }
        composable("settings") { SettingsScreen() }
        composable("about") { AboutScreen() }
    }
}
