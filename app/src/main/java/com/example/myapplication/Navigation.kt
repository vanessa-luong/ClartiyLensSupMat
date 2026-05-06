package com.example.myapplication

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

@Composable
fun Navigation() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "camera_wrapper"
    ) {
        composable("camera_wrapper") {
            PermissionCameraWrapper(
                navController = navController
            )
        }

        composable("camera") {
            CameraScreen(navController)
        }

        composable("filter/{imageUri}",
            listOf(navArgument("imageUri") {
                type = NavType.StringType
            })
        ) { backStackEntry ->
            val imageUri =
                backStackEntry.arguments?.getString("imageUri")
            FilterScreen(
                navController = navController,
                imageUri = imageUri)
        }
    }
}