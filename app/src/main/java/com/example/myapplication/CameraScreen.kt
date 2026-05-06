package com.example.myapplication

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

var isLoading = mutableStateOf(false)       // lazy global var
@Composable
fun CameraScreen(navController: NavController ) {
    val cameraContext = LocalContext.current
    val cameraController = remember { LifecycleCameraController(cameraContext).apply {      // control img capture
        setEnabledUseCases(CameraController.IMAGE_CAPTURE)
    }}

    LaunchedEffect(Unit) {
        isLoading.value = false
    }

    // COMPOSABLE
    Column(modifier = Modifier.fillMaxSize()) {
        CameraScreenTopRow(cameraContext, cameraController)

        Box(Modifier.weight(1f)) {      // box to apply dim
            CameraPreview(
                controller = cameraController,
                modifier = Modifier.fillMaxSize()
            )
            if (isLoading.value) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))   // dim
                CircularProgressIndicator(modifier = Modifier
                    .align(Alignment.Center)
                    .size(80.dp)
                )
            }
        }

        CameraScreenBottomRow(cameraContext, cameraController, navController)   // bottom row + recent gallery
    }
    ClickedRecentPhoto(cameraContext, navController)                    // full screen recent photo

}

fun saveToGallery(context: Context, sourceUri: Uri?, sourceBitmap: Bitmap?) {                       // partially based on Ahmed Guedmioui
    val resolver = context.contentResolver

    val timestamp = System.currentTimeMillis()
    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ClarityLens")
        put(MediaStore.Images.Media.DISPLAY_NAME, "${timestamp}.jpg")
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpg")
        put(MediaStore.Images.Media.DATE_TAKEN, timestamp)
        put(MediaStore.Images.Media.IS_PENDING, 1)
    }

    val imgContentUri =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // Q = lvl 29 = android 10
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
    val destinationUri = resolver.insert(imgContentUri, contentValues)

    destinationUri?.let {
        val outputStream = resolver.openOutputStream(destinationUri)        // writes to destination uri
        outputStream?.use {                                             // .use closes the stream after its done

            // URI INPUT
            sourceUri?.let {
            val inputStream = resolver.openInputStream(sourceUri)               //reads from source uri
                inputStream?.use { inputStream.copyTo(outputStream) }       // uri copy to outputStream
            }

            // BITMAP INPUT
            sourceBitmap?.let {
                sourceBitmap.compress( Bitmap.CompressFormat.JPEG, 100, outputStream)       // bitmap write to outputScream
            }
        }
        contentValues.clear()
        contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(destinationUri, contentValues, null, null)
    }
    Toast.makeText(context, "Photo Downloaded", Toast.LENGTH_SHORT).show()
}