package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.NavController

@Composable
fun PermissionCameraWrapper(navController: NavController) {

    val cameraContext = LocalContext.current
    val hasPermission = remember {
        ContextCompat.checkSelfPermission(
            cameraContext,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    val launcher = rememberLauncherForActivityResult(               // launcher setup
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->                                   // if allow, go to cameraScreen
            if (isGranted) {
                navController.navigate("camera")
            }
        }
    )

    LaunchedEffect(hasPermission) {                         // runs when hasPermission changes
        if (!hasPermission) {
            launcher.launch(Manifest.permission.CAMERA)     // launch system menu
        } else {
            navController.navigate("camera")   // or go to cameraScreen
        }
    }

    if (!hasPermission) {
        Column {
            Text("Camera permission is required to access the camera.")
            Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) {
                Text("Request Permission")
            }
        }
    }
}