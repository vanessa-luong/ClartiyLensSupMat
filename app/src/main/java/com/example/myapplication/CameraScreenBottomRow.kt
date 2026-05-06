package com.example.myapplication

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrowseGallery
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import java.io.File


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreenBottomRow(cameraContext: Context, cameraController: LifecycleCameraController, navController: NavController, clickedPhotoViewModel: ClickedPhotoViewModel = viewModel()) {
    val photoListViewModel = viewModel<PhotoListViewModel>()
    val photos by photoListViewModel.photos.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isSheetOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround
    ) {

        // OPEN RECENT GALLERY
        IconButton(
            onClick = { isSheetOpen = true }
        ) {
            Icon(
                imageVector = Icons.Default.BrowseGallery,
                contentDescription = "Recent Gallery",
                tint = MaterialTheme.colorScheme.onSurface     // dark mode compatibility
            )
        }

        // CAPTURE PHOTO
        IconButton(
            onClick = {
                isLoading.value = true
                takePhoto(cameraController, cameraContext, navController, onPhotoTaken = photoListViewModel::onTakePhoto)
            },
            modifier = Modifier.size(96.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Camera,
                contentDescription = "Take photo",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(48.dp)
            )
        }

        // GALLERY UPLOAD
        val singlePhotoPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia(),
            onResult = { uri ->
                uri?.let {
                    navController.navigate("filter/${Uri.encode(it.toString())}")
                }
            }
        )
        IconButton(
            onClick = {
                singlePhotoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
        ) {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = "Upload Image",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }

    // RECENT GALLERY
    if (isSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { isSheetOpen = false },
            sheetState = sheetState,
            modifier = Modifier
        ) {
            if (photos.isEmpty()) {
                Box(
                    modifier = Modifier.padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No photos taken.")
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(16.dp),
                    modifier = Modifier
                        .fillMaxHeight(0.6f)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    items(photos.reversed()) { photo ->
                        val context = LocalContext.current
                        val originalRotation = context.contentResolver.openInputStream(photo.uri)?.use {
                            getRotationDegrees(it) } ?: 0
                        val resultMat = rotateMat(bitmapToMat(photo.bitmap), originalRotation)
                        Image(
                            bitmap = matToBitmap(resultMat).asImageBitmap(),
                            contentDescription = "Captured photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(2.dp))
                                .clickable(onClick = {
                                    clickedPhotoViewModel.uri = photo.uri
                                    isSheetOpen = false })
                        )
                    }
                }
            }
        }
    }
}

private fun takePhoto(controller: LifecycleCameraController, context: Context, navController: NavController, onPhotoTaken: (Bitmap, Uri) -> Unit) {
    val photoFile = File(
        context.cacheDir,
        "photo_${System.currentTimeMillis()}.jpg"
    )
    controller.takePicture(
        ImageCapture.OutputFileOptions.Builder(photoFile).build(),
        ContextCompat.getMainExecutor(context),
        object: ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                val uri = Uri.fromFile(photoFile)
                val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)

                onPhotoTaken(bitmap, uri)
                navController.navigate(
                    "filter/${Uri.encode(uri.toString())}"
                )
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("Camera", "Photo capture failed", exception)
            }
        }
    )
}