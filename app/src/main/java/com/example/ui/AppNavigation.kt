package com.example.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.ui.screens.*

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(onNavigateToOnboarding = {
                navController.navigate("onboarding") {
                    popUpTo("splash") { inclusive = true }
                }
            })
        }
        composable("onboarding") {
            OnboardingScreen(onFinishOnboarding = {
                navController.navigate("home") {
                    popUpTo("onboarding") { inclusive = true }
                }
            })
        }
        composable("home") {
            HomeScreen(
                onNavigateToImport = { navController.navigate("import") },
                onNavigateToModels = { navController.navigate("models_manager") },
                onNavigateToSettings = { navController.navigate("settings") }
            )
        }
        composable("models_manager") {
            ModelsManagerScreen(onBack = { navController.popBackStack() })
        }
        composable("settings") {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable("import") {
            ImportScreen(
                onBack = { navController.popBackStack() },
                onContinue = { videoUri -> 
                    // Safe url encoding for navigation
                    val encodedUri = java.net.URLEncoder.encode(videoUri, "UTF-8")
                    navController.navigate("processing_settings/$encodedUri")
                }
            )
        }
        composable(
            route = "processing_settings/{videoUri}",
            arguments = listOf(navArgument("videoUri") { type = NavType.StringType })
        ) { backStackEntry ->
            val videoUri = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("videoUri") ?: "", "UTF-8")
            ProcessingSettingsScreen(
                videoUri = videoUri,
                onBack = { navController.popBackStack() },
                onStartProcessing = { aspectRatio, autoCaptions, clipCount, clipDuration ->
                    val encodedUri = java.net.URLEncoder.encode(videoUri, "UTF-8")
                    navController.navigate("processing/$encodedUri/$aspectRatio/$clipCount/$clipDuration")
                }
            )
        }
        composable(
            route = "processing/{videoUri}/{aspectRatio}/{clipCount}/{clipDuration}",
            arguments = listOf(
                navArgument("videoUri") { type = NavType.StringType },
                navArgument("aspectRatio") { type = NavType.StringType },
                navArgument("clipCount") { type = NavType.IntType },
                navArgument("clipDuration") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val videoUri = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("videoUri") ?: "", "UTF-8")
            val aspectRatio = backStackEntry.arguments?.getString("aspectRatio") ?: "9:16"
            val clipCount = backStackEntry.arguments?.getInt("clipCount") ?: 3
            val clipDuration = backStackEntry.arguments?.getInt("clipDuration") ?: 30
            
            ProcessingScreen(
                videoUri = videoUri,
                aspectRatio = aspectRatio,
                clipCount = clipCount,
                clipDuration = clipDuration,
                onCancel = { navController.popBackStack("home", inclusive = false) },
                onProcessingComplete = { 
                    val encodedUri = java.net.URLEncoder.encode(videoUri, "UTF-8")
                    navController.navigate("editor/$encodedUri") {
                        popUpTo("home") { inclusive = false }
                    }
                }
            )
        }
        composable(
            route = "editor/{videoUri}",
            arguments = listOf(navArgument("videoUri") { type = NavType.StringType })
        ) { backStackEntry ->
            val videoUri = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("videoUri") ?: "", "UTF-8")
            EditorScreen(
                videoUri = videoUri,
                onBack = { navController.popBackStack("home", inclusive = false) }
            )
        }
    }
}
