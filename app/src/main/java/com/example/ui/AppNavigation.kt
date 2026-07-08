package com.example.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
                    navController.navigate("processing_settings")
                }
            )
        }
        composable("processing_settings") {
            ProcessingSettingsScreen(
                videoUri = "",
                onBack = { navController.popBackStack() },
                onStartProcessing = { aspectRatio, autoCaptions, clipCount, clipDuration ->
                    navController.navigate("processing")
                }
            )
        }
        composable("processing") {
            ProcessingScreen(
                onCancel = { navController.popBackStack("home", inclusive = false) },
                onProcessingComplete = { 
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }
    }
}
