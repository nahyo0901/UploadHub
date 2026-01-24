package com.twice.whatislove.uploadhub.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.twice.whatislove.uploadhub.ui.screens.AboutScreen
import com.twice.whatislove.uploadhub.ui.screens.CatboxScreen
import com.twice.whatislove.uploadhub.ui.home.HomeScreen
import com.twice.whatislove.uploadhub.ui.screens.LitterboxScreen
import com.twice.whatislove.uploadhub.ui.screens.RcloneScreen
import com.twice.whatislove.uploadhub.ui.screens.SettingsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadHubNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onCatbox = { navController.navigate("catbox") },
                onLitterbox = { navController.navigate("litterbox") },
                onRclone = { navController.navigate("rclone") },
                onSettings = { navController.navigate("settings") },
                onAbout = { navController.navigate("about") }
            )
        }

        composable("catbox") { CatboxScreen() }
        composable("litterbox") { LitterboxScreen() }
        composable("rclone") { RcloneScreen() }
        composable("settings") { SettingsScreen() }
        composable("about") { AboutScreen() }
    }
}