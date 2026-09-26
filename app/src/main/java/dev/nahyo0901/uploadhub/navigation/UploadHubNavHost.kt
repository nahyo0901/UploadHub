package dev.nahyo0901.uploadhub.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.nahyo0901.uploadhub.ui.home.HomeScreen
import dev.nahyo0901.uploadhub.ui.screens.AboutScreen
import dev.nahyo0901.uploadhub.ui.screens.CatboxScreen
import dev.nahyo0901.uploadhub.ui.screens.LitterboxScreen
import dev.nahyo0901.uploadhub.ui.screens.FtpClientScreen
import dev.nahyo0901.uploadhub.ui.screens.FtpServerScreen
import dev.nahyo0901.uploadhub.ui.screens.ServerToServerFtpScreen
import dev.nahyo0901.uploadhub.ui.screens.SettingsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadHubNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        // Home screen with navigation lambdas
        composable("home") {
            HomeScreen(
                onCatbox = { navController.navigate("catbox") },
                onLitterbox = { navController.navigate("litterbox") },
                onFtpServer = { navController.navigate("ftpserver") },
                onFtpClient = { navController.navigate("ftpclient") },
                onServerToServerFtp = { navController.navigate("servertoserverftp") },
                onSettings = { navController.navigate("settings") },
                onAbout = { navController.navigate("about") }
            )
        }

        // Other screens
        composable("catbox") { CatboxScreen() }
        composable("litterbox") { LitterboxScreen() }
        composable("ftpserver") { FtpServerScreen() }
        composable("ftpclient") { FtpClientScreen() }
        composable("servertoserverftp") { ServerToServerFtpScreen() }
        composable("settings") { SettingsScreen() }
        composable("about") { AboutScreen() }
    }
}
